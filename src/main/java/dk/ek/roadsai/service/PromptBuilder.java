package dk.ek.roadsai.service;

import java.util.Map;

public class PromptBuilder {

    public String buildUserPrompt(String routeId, String mode, String isoTime, Map<String, DataReducer.SegmentFacts> segs) {
        StringBuilder sb = new StringBuilder();
        sb.append("route=").append(routeId).append("; mode=").append(mode).append("; time=").append(isoTime).append(";\n");
        for (var e : segs.entrySet()) {
            var s = e.getValue();
            sb.append("segment[").append(s.name).append("]: ");
            if (s.maxGustMs != null) {
                sb.append("gust=").append(s.maxGustMs).append("m/s; ");
            }
            if (s.windMs != null) {
                sb.append("wind=").append(s.windMs).append("m/s; ");
            }
            if (s.minTempC != null) {
                sb.append("temp=").append(s.minTempC).append("C; ");
            }
            if (s.minVisM != null) {
                sb.append("vis=").append(s.minVisM).append("m; ");
            }
            if (s.precipType != null) {
                sb.append("precip=").append(s.precipType).append("; ");
            }
            sb.append("\n");
        }
        sb.append("driver notes: none");
        return sb.toString();
    }

    public String systemPrompt() {
        return """
        You are a concise Iceland road-safety assistant. Using the provided segment facts, output exactly 4 bullet points, max 90 words total. Prioritize hazards (wind/gusts, ice, snow, visibility) and realistic advice (speed, spacing, caution on passes). No disclaimers, no emojis.
        """;
    }
}
