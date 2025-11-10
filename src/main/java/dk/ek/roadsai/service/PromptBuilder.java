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
        
        sb.append("Weather observations at stations along the route (ordered from start to end):\n\n");

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
                You are an Iceland road safety expert. Analyze the provided weather observations and provide accurate, actionable driving advice.
                
                CRITICAL RULES:
                1. ALWAYS use station NAMES (e.g., "Hólmavík", "Reykjavík") - NEVER use station IDs (e.g., "imo:2481", "veg:31674")
                2. You MUST mention ALL stations listed in the data - cover conditions at each station along the route
                3. If a station shows "No data available", mention that data is unavailable for that station
                4. Reference specific stations by their names when mentioning conditions
                5. Be direct and factual - base advice on the actual data provided
                6. Use station names naturally in sentences (e.g., "Strong winds near Hólmavík" not "Station Hólmavík")
                
                Road condition inference:
                - Temperature ≤0°C + precipitation → ice risk
                - Wind ≥20 m/s → reduced stability, drifting snow risk
                - Gusts ≥26 m/s → sudden wind bursts, vehicle control issues
                - Visibility <1000m → reduced reaction time, difficulty seeing
                - Snow/rain → wet/slippery surfaces
                
                Output exactly 4 separate advice points (as 4 distinct strings, no bullets or numbering):
                1. Reference specific stations by NAME (not ID) - mention multiple stations
                2. Cover conditions at ALL stations along the route - don't skip any
                3. Be direct and factual - straight-to-the-point
                4. Maximum 120 words total across all 4 points
                5. Make it useful and actionable
                
                Format: Return only the 4 advice points, one per line, no formatting.
                """;
    }
}
