package dk.ek.roadsai.dto;

import java.util.List;
import java.util.Map;

/**
 * frontend user response
 */
public record AdviceResponse(
        List<String> advice, // txt advice from AI
        Map<String, Object> summaryStats, // distance, duration
        Map<String, Object> mapData // route + stations
) {
}
