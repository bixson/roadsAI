package dk.ek.roadsai.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects road hazards based on wind warning guidelines
 * Uses thresholds from official road safety guidelines (SGS)
 */
@Service
public class HazardDetector {

    // wind warning thresholds (m/s)
    private static final double WARNING_1_WIND = 20.0;
    private static final double WARNING_2_WIND = 24.0;
    private static final double WARNING_3_WIND = 28.0;
    private static final double WARNING_1_GUST = 26.0;
    private static final double WARNING_2_GUST = 30.0;
    private static final double WARNING_3_GUST = 35.0;

    // Visibility threshold (meters) - low visibility hazard
    private static final double LOW_VISIBILITY = 1000.0;

    // Temperature threshold (°C) - freezing conditions
    private static final double FREEZING_TEMP = 0.0;

    /**
     * Detects hazards from segment facts (weather data per station)
     * Returns list of hazard descriptions based on SGS guidelines
     */
    public List<String> detectHazards(Map<String, DataReducer.SegmentFacts> segments) {
        List<String> hazards = new ArrayList<>();
        
        // Add header to hazards list so it appears in API response
        hazards.add("Official Weather Warnings (Icelandic Road Safety Office):");

        for (var entry : segments.entrySet()) {
            var facts = entry.getValue();
            String stationName = facts.name;

            // Check wind speed hazards
            if (facts.windMs != null) {
                if (facts.windMs >= WARNING_3_WIND) {
                    hazards.add("Warning Level 3: Wind " + String.format("%.1f", facts.windMs) + " m/s at " + stationName + " - Unconditional stop recommended");
                } else if (facts.windMs >= WARNING_2_WIND) {
                    hazards.add("Warning Level 2: Wind " + String.format("%.1f", facts.windMs) + " m/s at " + stationName + " - Reduce speed significantly");
                } else if (facts.windMs >= WARNING_1_WIND) {
                    hazards.add("Warning Level 1: Wind " + String.format("%.1f", facts.windMs) + " m/s at " + stationName + " - Drive carefully");
                }
            }

            // Check gust hazards
            if (facts.maxGustMs != null) {
                if (facts.maxGustMs >= WARNING_3_GUST) {
                    hazards.add("Severe gusts " + String.format("%.1f", facts.maxGustMs) + " m/s at " + stationName + " - Extreme caution");
                } else if (facts.maxGustMs >= WARNING_2_GUST) {
                    hazards.add("Strong gusts " + String.format("%.1f", facts.maxGustMs) + " m/s at " + stationName);
                } else if (facts.maxGustMs >= WARNING_1_GUST) {
                    hazards.add("Gusts " + String.format("%.1f", facts.maxGustMs) + " m/s at " + stationName + " - Reduced stability");
                }
            }

            // Check visibility hazards
            if (facts.minVisM != null && facts.minVisM < LOW_VISIBILITY) {
                hazards.add("Low visibility " + String.format("%.0f", facts.minVisM) + "m at " + stationName + " - Reduced reaction time");
            }

            // Check freezing conditions
            if (facts.minTempC != null && facts.minTempC <= FREEZING_TEMP) {
                if (facts.precipType != null && (facts.precipType.contains("snow") || facts.precipType.contains("rain"))) {
                    hazards.add("Freezing conditions " + String.format("%.1f", facts.minTempC) + "°C with " + facts.precipType + " at " + stationName + " - Ice risk");
                }
            }
        }

        return hazards;
    }
}

