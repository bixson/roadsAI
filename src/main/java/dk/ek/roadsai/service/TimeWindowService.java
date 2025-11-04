package dk.ek.roadsai.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

// departure/arruval time windows
public class TimeWindowService {
    private static final double DEFAULT_SPEED_KMH = 90.0; // Þjóðvegur = 90 km/h

    public Map<String, Instant> window(String mode, Instant t, double routeKm) {
        if ("arrival".equalsIgnoreCase(mode)) {
            long ETAseconds = Math.round((routeKm / DEFAULT_SPEED_KMH) * 3600);
            Instant ETA = t;
            Instant depart = t.minusSeconds(ETAseconds);
            return Map.of("from", depart.minusSeconds(2 * 3600), "to", depart.plusSeconds(2 * 3600));
        } else { // departure
            return Map.of("from", t, "to", t.plus(4, ChronoUnit.HOURS));
        }
    }
}
