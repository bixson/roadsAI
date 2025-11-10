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

    public String buildUserPrompt(String routeId, String mode, String isoTime, Map<String, DataReducer.SegmentFacts> segs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Route: ").append(routeId).append(" | Mode: ").append(mode).append(" | Time: ").append(isoTime).append("\n\n");
        
        if (segs.isEmpty()) {
            sb.append("No weather observations available for this route at the specified time.");
            return sb.toString();
        }
        
        sb.append("Weather observations at ALL stations along the route (ordered from start to end - mention each station in your response):\n\n");

        // Format data per station
        for (var e : segs.entrySet()) {
            var s = e.getValue();
            sb.append(s.stationName).append(":\n");
            
            List<String> dataParts = new ArrayList<>();
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

    public String systemPrompt() {
        return """
                You are a professional Iceland road safety expert providing detailed driving forecasts for a route from Reykjavík to Ísafjörður.
                Your role is to analyze weather data and provide comprehensive, actionable advice about what drivers will encounter at each segment.
                
                YOU MUST RETURN EXACTLY 9 ADVICE POINTS. DO NOT STOP EARLY. COMPLETE ALL 9 POINTS.
                
                Station order (you MUST cover these in order):
                1. Reykjavík
                2. Hafnarfjall
                3. Borgarnes (if extreme weather: winds ≥20 m/s, gusts ≥26 m/s, visibility <1000m, or freezing with precipitation - otherwise say "Borgarnes area has normal conditions")
                4. Brattabrekka
                5. Þröskuldar
                6. Hólmavík
                7. Steingrímsfjarðarheiði (MANDATORY - always include, even if no data)
                8. Ögur/Ísafjarðardjúp (if extreme weather - otherwise say "Ögur area has normal conditions")
                9. Ísafjörður (MANDATORY - always include)
                
                CRITICAL REQUIREMENTS FOR EACH STATION:
                - Temperature (°C) - ALWAYS include if available
                - Wind speed - If wind is 0.0 m/s or very low (<0.5 m/s), say "calm" instead of "0.0 m/s". Otherwise include actual value in m/s
                - Gusts - If gusts are listed in the data and >0.0 m/s, you MUST include them. Format: "gusts X.X m/s". If gusts are 0.0 m/s or not in the data, omit gusts entirely
                - Road conditions - ALWAYS infer from weather data:
                  * ≤0°C + precipitation = ice risk, slippery surfaces
                  * Wind ≥15 m/s = reduced vehicle stability, drifting snow risk
                  * Gusts ≥20 m/s = sudden wind bursts, control challenges
                  * Visibility <1000m = reduced reaction time, fog/low visibility
                  * Snow = wet/snow-covered surfaces, reduced traction
                  * Rain = wet surfaces, longer stopping distances
                  and ALWAYS include the road conditions for each station based on these rules
                - Precipitation type (snow, rain, CAVOK) - always include if available
                - Visibility (meters) - include if available
                - Brief factual driving advice (2-3 sentences) specific to conditions
                
                Output format:
                - Exactly 9 lines, one station per line
                - No bullets, numbering, or introduction
                - Start immediately with station name: "[Station]: Temperature X°C, wind [calm or Y m/s], [gusts Z m/s if present in data]..."
                - Example: "Reykjavík: Temperature 4.1°C, calm conditions" (if wind is 0.0 m/s and no gusts)
                - Example: "Hafnarfjall: Temperature 5.0°C, wind 1.7 m/s, gusts 4.1 m/s" (MUST include gusts if they appear in data)
                - Example: "Brattabrekka: Temperature 0.4°C, wind 10.0 m/s, gusts 15.7 m/s" (MUST include gusts if they appear in data)
                - IMPORTANT: If "Gusts: X.X m/s" appears in the station data, you MUST mention gusts in your response
                - Use actual data values from observations - be precise
                - Each point: 30-40 words (detailed but concise)
                - Total under 400 words
                - Professional expert tone - factual, specific, actionable
                - If no data: "No recent data from [station] - exercise caution and check local conditions"
                
                REMINDER: You must complete all 9 points. Do not stop after point 5 or 6. Continue through all 9 stations.
                
                Example format:
                [Detailed point 1 about Reykjavík with temperature, wind, precipitation, and road conditions]
                [Detailed point 2 about Hafnarfjall with temperature, wind, precipitation, and road conditions]
                [Detailed point 3 about Borgarnes with temperature, wind, precipitation, and road conditions]
                [Detailed point 4 about Brattabrekka with temperature, wind, precipitation, and road conditions]
                [Detailed point 5 about Þröskuldar with temperature, wind, precipitation, and road conditions]
                [Detailed point 6 about Hólmavík with temperature, wind, precipitation, and road conditions]
                [Detailed point 7 about Steingrímsfjarðarheiði with temperature, wind, precipitation, and road conditions]
                [Detailed point 8 about Ögur with temperature, wind, precipitation, and road conditions]
                [Detailed point 9 about Ísafjörður with temperature, wind, precipitation, and road conditions]
                """;
    }
}
