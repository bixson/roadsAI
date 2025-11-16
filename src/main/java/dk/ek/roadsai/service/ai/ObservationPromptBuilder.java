package dk.ek.roadsai.service.ai;

import dk.ek.roadsai.model.ForecastPoint;
import dk.ek.roadsai.model.Station;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/// Builds prompts for OpenAI to generate driving advice based on current observations + forecasts.
// takes ObservationReducer and writes into prompt - both system- + user-prompt -> sends to ObservationAiService
@Service
public class ObservationPromptBuilder {

    public String buildSystemPrompt() {
        return """
                You are a professional Iceland road safety advisor analyzing real-time weather observations and forecasts.
                CRITICAL: Official CAP (Common Alerting Protocol) alerts from Veðurstofa Íslands MUST be prioritized and prominently featured in your advice.
                These are official warnings and take precedence over observations and forecasts.
                Provide concise, data-driven driving advice based on actual measured conditions and predicted changes.
                Avoid generic phrases like "be careful" or "drive safely" - focus on specific conditions and actionable guidance.
                Use metric units (m/s for wind, °C for temperature, meters for visibility).
                """;
    }

    ///  1. Route info & forecast context
    ///  2. Official CAP alerts (highest priority)
    ///  3. Current station observations (vedur.is + vegagerdin.is)
    ///  4. Forecast data (yr.no API, if requested)
    ///  5. Final instructions for AI response format
    public String buildUserPrompt(
            String routeName,
            String from,
            String to,
            Map<String, ObservationReducer.StationFacts> stations,
            List<Station> stationList,
            List<ForecastPoint> forecasts,
            Instant forecastTime) {

        StringBuilder prompt = new StringBuilder();

        /// 1: Route Information & Forecast Context
        prompt.append("Analyze weather conditions for route: ").append(routeName)
                .append(" (").append(from).append(" → ").append(to).append(")\n");

        if (forecastTime != null) {
            long hoursAhead = java.time.Duration.between(Instant.now(), forecastTime).toHours();
            prompt.append("Requested forecast time: ").append(forecastTime.toString())
                    .append(" (").append(hoursAhead).append(" hours ahead)\n");

            if (hoursAhead > 2) {
                prompt.append("**IMPORTANT: Forecast requested for ").append(hoursAhead)
                        .append(" hours in the future. Current observations below are for context only. ")
                        .append("Focus primarily on forecast data for the requested time.**\n");
            }
        } else {
            prompt.append("Current conditions only (no forecast requested)\n");
        }
        prompt.append("\n");
        prompt.append("CRITICAL REQUIREMENT: Provide exactly ").append(stations.size())
                .append(" advice points, one per station segment.\n\n");

        /// 2: Official CAP Alerts (Highest Priority)
        boolean hasAlerts = stations.values().stream() // stream all station facts
                .anyMatch(f -> f.alerts != null && !f.alerts.isEmpty()); // check if any station has alerts

        //builds alert section, if present (handle multiple per-station with ',')
        if (hasAlerts) {
            prompt.append("**⚠️ CRITICAL: OFFICIAL CAP ALERTS - PRIORITIZE THESE IN YOUR ADVICE ⚠️**\n");
            prompt.append("These are official warnings from Veðurstofa Íslands and must be prominently featured.\n\n");
            for (var entry : stations.entrySet()) {
                var facts = entry.getValue();
                if (facts.alerts != null && !facts.alerts.isEmpty()) {
                    prompt.append("- ").append(facts.stationName).append(": ");
                    boolean firstAlert = true;
                    for (var alert : facts.alerts) {
                        if (!firstAlert) {
                            prompt.append("; ");
                        }
                        if (alert.headline != null) {
                            prompt.append(alert.headline);
                        }
                        if (alert.severity != null) {
                            prompt.append(" [").append(alert.severity).append("]");
                        }
                        if (alert.eventType != null) {
                            prompt.append(" (").append(alert.eventType).append(")");
                        }
                        firstAlert = false;
                    }
                    prompt.append("\n");
                }
            }
            prompt.append("\n");
        }

        /// 3: Current Station Observations
        // Shows measured weather data from vedur.is and vegagerdin.is (last 15 minutes)
        if (forecastTime != null) {
            long hoursAhead = java.time.Duration.between(Instant.now(), forecastTime).toHours();
            if (hoursAhead > 2) {
                prompt.append("Station Observations (current conditions, last 15 min - for context only):\n");
            } else {
                prompt.append("Station Observations (current conditions, last 15 min):\n");
            }
        } else {
            prompt.append("Station Observations (worst-case metrics, last 15 min):\n");
        }
        for (var entry : stations.entrySet()) {
            var facts = entry.getValue();
            prompt.append("- ").append(facts.stationName).append(": ");

            List<String> parts = new java.util.ArrayList<>();
            if (facts.windMs != null) {
                parts.add("Wind " + String.format("%.1f", facts.windMs) + " m/s");
            }
            if (facts.maxGustMs != null) {
                parts.add("Gusts " + String.format("%.1f", facts.maxGustMs) + " m/s");
            }
            if (facts.minTempC != null) {
                parts.add("Temp " + String.format("%.1f", facts.minTempC) + "°C");
            }
            if (facts.minVisM != null) {
                parts.add("Visibility " + String.format("%.0f", facts.minVisM) + "m");
            }
            if (facts.precipType != null && !facts.precipType.isBlank()) {
                parts.add("Precip: " + facts.precipType);
            }
            
            prompt.append(parts.isEmpty() ? "No recent observations" : String.join(", ", parts));
            prompt.append("\n");
        }

        /// 4: Forecast Data (if requested)
        // Shows predicted weather from yr.no API up to the requested time
        if (forecastTime != null && forecasts != null && !forecasts.isEmpty()) {
            prompt.append("\n**FORECAST DATA** (up to requested time)\n");

            // Match forecasts to stations by coordinates
            for (Station station : stationList) {
                List<ForecastPoint> stationForecasts = forecasts.stream() // stream all forecasts
                        .filter(f -> Math.abs(f.latitude() - station.latitude()) < 0.001 && // match by coordinates
                                Math.abs(f.longitude() - station.longitude()) < 0.001 &&
                                (f.time().isBefore(forecastTime) || f.time().equals(forecastTime))) // up to requested time
                        .toList();

                if (!stationForecasts.isEmpty()) {
                    // Find worst-case forecast values
                    Double maxWind = stationForecasts.stream().map(ForecastPoint::windMs).filter(Objects::nonNull).max(Double::compare).orElse(null); // worst-case wind
                    Double minTemp = stationForecasts.stream().map(ForecastPoint::tempC).filter(Objects::nonNull).min(Double::compare).orElse(null); // worst-case temp
                    Double maxPrecip = stationForecasts.stream().map(ForecastPoint::precipMm).filter(Objects::nonNull).max(Double::compare).orElse(null); // worst-case precip

                    // output summary for station
                    prompt.append("- ").append(station.name()).append(" forecast: ");
                    boolean hasForecast = false;
                    if (maxWind != null) {
                        prompt.append("Wind up to ").append(String.format("%.1f", maxWind)).append(" m/s");
                        hasForecast = true;
                    }
                    if (minTemp != null) {
                        if (hasForecast) {
                            prompt.append(", ");
                        }
                        prompt.append("Temp ").append(String.format("%.1f", minTemp)).append("°C");
                        hasForecast = true;
                    }
                    if (maxPrecip != null && maxPrecip > 0) {
                        if (hasForecast) {
                            prompt.append(", ");
                        }
                        prompt.append("Precip ").append(String.format("%.1f", maxPrecip)).append("mm");
                    }
                    prompt.append("\n");
                }
            }
            prompt.append("\n");
        }

        /// 5: Final Instructions
        // Tells AI how to format the response based on data type (current vs forecast)
        prompt.append("\n");
        if (hasAlerts) {
            prompt.append("IMPORTANT: If CAP alerts are present above, they MUST be prominently mentioned in your advice for affected stations.\n");
        }
        if (forecastTime != null) {
            long hoursAhead = java.time.Duration.between(Instant.now(), forecastTime).toHours();
            prompt.append("Provide ").append(stations.size())
                    .append(" concise advice points (20-25 words each), one per station, combining current conditions with forecast trends up to the requested time (")
                    .append(hoursAhead).append(" hours ahead).");
        } else {
            prompt.append("Provide ").append(stations.size())
                    .append(" concise advice points (20-25 words each), one per station, focusing on current conditions and specific driving guidance.");
        }

        return prompt.toString();
    }
}

