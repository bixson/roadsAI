package dk.ek.roadsai.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * promptBuilder for LLM
 * formats weather data into structured text for AI analysis
 */
@Service
public class PromptBuilder {


    // System prompt: Role, tone, and style guidelines (static)
    // Sets the AI's personality and communication style
    public String buildSystemPrompt() {
        return """
                You are a professional Iceland road safety expert providing detailed driving forecasts.
                Your role is to analyze weather data and provide comprehensive, actionable advice about what drivers will encounter.
                
                Communication style:
                - Professional expert tone - concise, factual, actionable
                - Be direct and factual - avoid redundant phrases unless absolutely necessary
                - Focus on specific conditions and concrete advice - skip generic warnings
                - Avoid phrases like "exercise caution", "stay alert", "drive carefully", "be cautious" - instead state the specific risk and action needed
                - Use actual data values from observations - be precise
                - No bullets, numbering, or introduction in your response
                """;
    }


    // User prompt: Critical requirements, data-specific rules, and actual weather data (dynamic)
    // Contains the "hard rules" that must be followed + actual data
    public String buildUserPrompt(String routeId, String mode, String isoTime, Map<String, DataReducer.SegmentFacts> segs, String from, String to) {
        String routeDesc = ("IFJ".equals(from) && "RVK".equals(to))
                ? "from Ísafjörður to Reykjavík"
                : "from Reykjavík to Ísafjörður";

        // Build weather data section
        String weatherData = buildWeatherDataSection(segs);

        return """
                CRITICAL REQUIREMENTS:
                - YOU MUST RETURN EXACTLY 9 ADVICE POINTS. DO NOT STOP EARLY. COMPLETE ALL 9 POINTS.
                - Follow the station order provided below (stations are ordered from start to end of route %s).
                - Cover each station in order - do not skip any.
                - REMINDER: You must complete all 9 points. Do not stop after point 5 or 6. Continue through all 9 stations.
                
                REQUIREMENTS FOR EACH STATION:
                - OFFICIAL ALERTS (marked with "OFFICIAL ALERT:") - These are official warnings from Icelandic Meteorological Office. You MUST mention these prominently and prioritize them over regular weather data. If an official alert exists, start your advice for that station with the alert information.
                - Temperature (°C) - ALWAYS include if available.
                - Wind speed - If wind is <0.5 m/s, say "calm" instead of the value. Otherwise include actual value in m/s.
                - Gusts - If "Gusts: X.X m/s" appears in the station data, you MUST include gusts in your response. Format: "gusts X.X m/s". If gusts are not in the data or 0.0 m/s, omit gusts entirely.
                - Road conditions - ALWAYS include inferred road conditions based on weather data. This is MANDATORY for every station:
                  * ≤0°C + precipitation = ice risk, slippery surfaces
                  * Wind ≥15 m/s = reduced vehicle stability, drifting snow risk
                  * Gusts ≥20 m/s = sudden wind bursts, control challenges
                  * Visibility <1000m = reduced reaction time, fog/low visibility
                  * Snow = wet/snow-covered surfaces, reduced traction
                  * Rain = wet surfaces, longer stopping distances
                  * Normal conditions = clear roads, good traction (if none of above apply)
                - Precipitation type (snow, rain, CAVOK) - always include if available.
                - Visibility (meters) - include if available.
                - Brief factual driving advice (1-2 sentences) specific to conditions - be concise.
                
                OUTPUT FORMAT:
                - Exactly 9 lines, one station per line.
                - Start immediately with station name: "[Station]: [OFFICIAL ALERT if present] Temperature X°C, wind [calm or Y m/s], [gusts Z m/s if present in data], [road conditions]..."
                - Example: "Reykjavík: Temperature 4.1°C, calm. Normal conditions - clear roads, good traction."
                - Example: "Hafnarfjall: Temperature 5.0°C, wind 1.7 m/s, gusts 4.1 m/s. Stable conditions - clear roads."
                - Example: "Brattabrekka: OFFICIAL ALERT: Severe wind warning. Temperature 0.4°C, wind 10.0 m/s, gusts 15.7 m/s. Near freezing - ice risk on shaded sections, reduced stability from wind."
                - IMPORTANT: Every response MUST include road conditions for that station - this is mandatory.
                - Each point: 20-25 words normally, but if OFFICIAL ALERT exists for that station, you may use up to 35-40 words to fully explain the alert.
                - Total under 250 words normally, but if OFFICIAL ALERTS are present, you may use up to 350 words total.
                - Only use the extra word count when OFFICIAL ALERTS are present. Without alerts, stick to 20-25 words per point and 250 words total.
                - If no data: "No recent data from [station] - exercise caution and check local conditions".
                
                Route: %s | Mode: %s | Time: %s
                
                %s
                """.formatted(routeDesc, routeId, mode, isoTime, weatherData);
    }


    //Builds the weather data section dynamically
    private String buildWeatherDataSection(Map<String, DataReducer.SegmentFacts> segs) {
        if (segs.isEmpty()) {
            return "No weather observations available for this route at the specified time.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Weather observations at ALL stations along the route (ordered from start to end - mention each station in your response):\n\n");

        // Format data per station
        for (var e : segs.entrySet()) {
            var s = e.getValue();
            sb.append(s.stationName).append(":\n");

            List<String> dataParts = new ArrayList<>();

            // Add CAP alerts FIRST (official warnings take priority)
            if (s.alerts != null && !s.alerts.isEmpty()) {
                for (var alert : s.alerts) {
                    String alertText = null;
                    if (alert.headline != null && !alert.headline.isBlank()) {
                        alertText = "OFFICIAL ALERT: " + alert.headline;
                    } else if (alert.description != null && !alert.description.isBlank()) {
                        alertText = "OFFICIAL ALERT: " + alert.description;
                    } else {
                        StringBuilder fallback = new StringBuilder();
                        if (alert.eventType != null && !alert.eventType.isBlank()) {
                            fallback.append(alert.eventType);
                        }
                        if (alert.severity != null && !alert.severity.isBlank()) {
                            if (fallback.length() > 0) {
                                fallback.append(" (").append(alert.severity).append(")");
                            } else {
                                fallback.append(alert.severity);
                            }
                        }
                        if (fallback.length() > 0) {
                            alertText = "OFFICIAL ALERT: " + fallback.toString();
                        }
                        // If no useful information, skip this alert entirely
                    }
                    if (alertText != null) {
                        dataParts.add(alertText);
                    }
                }
            }

            if (s.windMs != null) {
                dataParts.add("Wind: " + String.format("%.1f", s.windMs) + " m/s");
            }
            if (s.maxGustMs != null) {
                dataParts.add("Gusts: " + String.format("%.1f", s.maxGustMs) + " m/s");
            }
            if (s.minTempC != null) {
                dataParts.add("Temperature: " + String.format("%.1f", s.minTempC) + "°C");
            }
            if (s.minVisM != null) {
                dataParts.add("Visibility: " + String.format("%.0f", s.minVisM) + " m");
            }
            if (s.precipType != null && !s.precipType.isBlank()) {
                dataParts.add("Precipitation: " + s.precipType);
            }

            if (dataParts.isEmpty()) {
                sb.append("  No data available");
            } else {
                sb.append("  ").append(String.join(", ", dataParts));
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }
}
