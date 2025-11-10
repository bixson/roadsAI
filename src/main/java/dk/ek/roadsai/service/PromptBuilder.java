package dk.ek.roadsai.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PromptBuilder {

    public String buildUserPrompt(String routeId, String mode, String isoTime, Map<String, DataReducer.SegmentFacts> segs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Route: ").append(routeId).append(" | Mode: ").append(mode).append(" | Time: ").append(isoTime).append("\n\n");
        sb.append("Weather observations along route segments:\n");
        for (var e : segs.entrySet()) {
            var s = e.getValue();
            sb.append("- Station ").append(s.name).append(": ");
            boolean hasData = false;
            if (s.maxGustMs != null) {
                sb.append("gust=").append(String.format("%.1f", s.maxGustMs)).append("m/s");
                hasData = true;
            }
            if (s.windMs != null) {
                if (hasData) sb.append(", ");
                sb.append("wind=").append(String.format("%.1f", s.windMs)).append("m/s");
                hasData = true;
            }
            if (s.minTempC != null) {
                if (hasData) sb.append(", ");
                sb.append("temp=").append(String.format("%.1f", s.minTempC)).append("°C");
                hasData = true;
            }
            if (s.minVisM != null) {
                if (hasData) sb.append(", ");
                sb.append("visibility=").append(String.format("%.0f", s.minVisM)).append("m");
                hasData = true;
            }
            if (s.precipType != null) {
                if (hasData) sb.append(", ");
                sb.append("precipitation=").append(s.precipType);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String systemPrompt() {
        return """
        You are an Iceland road safety expert. Analyze the provided weather observations and infer realistic road conditions along the route. Use ONLY the data provided - do not make assumptions beyond what the weather data suggests.
        
        Based on the weather data, infer likely road conditions:
        - Temperature near/below freezing + precipitation → ice risk
        - High wind/gusts → reduced vehicle stability, drifting snow
        - Low visibility → reduced reaction time, difficulty seeing road markings
        - Snow/rain → wet/slippery surfaces
        
        Output exactly 4 separate advice points (as 4 distinct strings, no bullet formatting). Each point should be:
        1. Specific to the conditions observed
        2. Actionable driving advice
        3. Focus on hazards and safety measures
        4. Maximum 90 words total across all 4 points
        
        Format: Return only the 4 advice points, one per line, no numbering or bullets.
        """;
    }
}
