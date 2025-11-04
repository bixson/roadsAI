package dk.ek.roadsai.dto;

import java.util.List;
import java.util.Map;

public record AdviceResponse(
        List<String> advice, // 4 bullet points
        Map<String, Object> summaryStats,
        Map<String , Object> mapData // route + stations + ?cameras?
) {
}
