package dk.ek.roadsai.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * departure/arrival time window calculator
 * fixed for Reykjavík ↔ Ísafjörður
 */
@Service
public class TimeWindowService {
    private static final double DEFAULT_SPEED_KMH = 90.0; // Þjóðvegur = 90 km/h


    public Map<String, Instant> window(String mode, Instant t, double routeKm) {
        if ("arrival".equalsIgnoreCase(mode)) {
            // Calculate estimated travel time at 'speed limit'
            long ETAseconds = Math.round((routeKm / DEFAULT_SPEED_KMH) * 3600);
            // Work backward to find est departure time
            Instant depart = t.minusSeconds(ETAseconds);
            // Create window covering entire travel period: 2h before departure to 1h after arrival (ensures complete journey coverage)
            return Map.of("from", depart.minusSeconds(2 * 3600), "to", t.plusSeconds(1 * 3600));
        } else { // departure
            // Create 4-hour forward window from departure time
            return Map.of("from", t, "to", t.plus(4, ChronoUnit.HOURS));
        }
    }
}
