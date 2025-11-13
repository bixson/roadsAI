package dk.ek.roadsai.service;

import dk.ek.roadsai.model.ForecastPoint;
import dk.ek.roadsai.model.Station;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;


/// Builds prompts for OpenAI to generate driving advice based on current observations + forecasts.
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

    public String buildUserPrompt(
            String routeName,
            String from,
            String to,
            Map<String, ObservationReducer.StationFacts> stations,
            List<Station> stationList,
            List<ForecastPoint> forecasts,
            Instant forecastTime) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze weather conditions for route: ").append(routeName)
                .append(" (").append(from).append(" → ").append(to).append(")\n");
        
        if (forecastTime != null) {
            prompt.append("Requested forecast time: ").append(forecastTime.toString()).append("\n");
        } else {
            prompt.append("Current conditions only (no forecast requested)\n");
        }
        prompt.append("\n");
        prompt.append("CRITICAL REQUIREMENT: Provide exactly ").append(stations.size())
                .append(" advice points, one per station segment.\n\n");

        // Add official alerts section - PRIORITIZED FIRST
        boolean hasAlerts = stations.values().stream()
                .anyMatch(f -> f.alerts != null && !f.alerts.isEmpty());

        if (hasAlerts) {
            prompt.append("**⚠️ CRITICAL: OFFICIAL CAP ALERTS - PRIORITIZE THESE IN YOUR ADVICE ⚠️**\n");
            prompt.append("These are official warnings from Veðurstofa Íslands and must be prominently featured.\n\n");
            for (var entry : stations.entrySet()) {
                var facts = entry.getValue();
                if (facts.alerts != null && !facts.alerts.isEmpty()) {
                    prompt.append("- ").append(facts.stationName).append(": ");
                    for (var alert : facts.alerts) {
                        if (alert.headline != null) {
                            prompt.append(alert.headline);
                        }
                        if (alert.severity != null) {
                            prompt.append(" [").append(alert.severity).append("]");
                        }
                        if (alert.eventType != null) {
                            prompt.append(" (").append(alert.eventType).append(")");
                        }
                    }
                    prompt.append("\n");
                }
            }
            prompt.append("\n");
        }

        // Add station observations
        prompt.append("Station Observations (worst-case metrics):\n");
        for (var entry : stations.entrySet()) {
            var facts = entry.getValue();
            prompt.append("- ").append(facts.stationName).append(": ");

            boolean hasData = false;
            if (facts.windMs != null) {
                prompt.append("Wind ").append(String.format("%.1f", facts.windMs)).append(" m/s");
                hasData = true;
            }
            if (facts.maxGustMs != null) {
                if (hasData) {
                    prompt.append(", ");
                }
                prompt.append("Gusts ").append(String.format("%.1f", facts.maxGustMs)).append(" m/s");
                hasData = true;
            }
            if (facts.minTempC != null) {
                if (hasData) {
                    prompt.append(", ");
                }
                prompt.append("Temp ").append(String.format("%.1f", facts.minTempC)).append("°C");
                hasData = true;
            }
            if (facts.minVisM != null) {
                if (hasData) {
                    prompt.append(", ");
                }
                prompt.append("Visibility ").append(String.format("%.0f", facts.minVisM)).append("m");
                hasData = true;
            }
            if (facts.precipType != null && !facts.precipType.isBlank()) {
                if (hasData) {
                    prompt.append(", ");
                }
                prompt.append("Precip: ").append(facts.precipType);
                hasData = true;
            }
            if (!hasData) {
                prompt.append("No recent observations");
            }
            prompt.append("\n");
        }

        // Add forecast data if available
        if (forecastTime != null && forecasts != null && !forecasts.isEmpty() && stationList != null) {
            prompt.append("\n**FORECAST DATA** (up to requested time)\n");
            
            // Match forecasts to stations by coordinates
            for (Station station : stationList) {
                List<ForecastPoint> stationForecasts = forecasts.stream()
                        .filter(f -> Math.abs(f.latitude() - station.latitude()) < 0.001 &&
                                   Math.abs(f.longitude() - station.longitude()) < 0.001 &&
                                   (f.time().isBefore(forecastTime) || f.time().equals(forecastTime)))
                        .toList();
                
                if (!stationForecasts.isEmpty()) {
                    // Find worst-case forecast values
                    Double maxWind = stationForecasts.stream()
                            .map(ForecastPoint::windMs)
                            .filter(java.util.Objects::nonNull)
                            .max(Double::compare)
                            .orElse(null);
                    Double minTemp = stationForecasts.stream()
                            .map(ForecastPoint::tempC)
                            .filter(java.util.Objects::nonNull)
                            .min(Double::compare)
                            .orElse(null);
                    Double maxPrecip = stationForecasts.stream()
                            .map(ForecastPoint::precipMm)
                            .filter(java.util.Objects::nonNull)
                            .max(Double::compare)
                            .orElse(null);
                    
                    prompt.append("- ").append(station.name()).append(" forecast: ");
                    boolean hasFc = false;
                    if (maxWind != null) {
                        prompt.append("Wind up to ").append(String.format("%.1f", maxWind)).append(" m/s");
                        hasFc = true;
                    }
                    if (minTemp != null) {
                        if (hasFc) prompt.append(", ");
                        prompt.append("Temp ").append(String.format("%.1f", minTemp)).append("°C");
                        hasFc = true;
                    }
                    if (maxPrecip != null && maxPrecip > 0) {
                        if (hasFc) prompt.append(", ");
                        prompt.append("Precip ").append(String.format("%.1f", maxPrecip)).append("mm");
                    }
                    prompt.append("\n");
                }
            }
            prompt.append("\n");
        }

        prompt.append("\n");
        if (hasAlerts) {
            prompt.append("IMPORTANT: If CAP alerts are present above, they MUST be prominently mentioned in your advice for affected stations.\n");
        }
        if (forecastTime != null) {
            prompt.append("Provide ").append(stations.size())
                    .append(" concise advice points (20-25 words each), one per station, combining current conditions with forecast trends up to the requested time.");
        } else {
            prompt.append("Provide ").append(stations.size())
                    .append(" concise advice points (20-25 words each), one per station, focusing on current conditions and specific driving guidance.");
        }

        return prompt.toString();
    }
}

