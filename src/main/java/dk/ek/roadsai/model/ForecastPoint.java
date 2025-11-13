package dk.ek.roadsai.model;

import java.time.Instant;

/// Forecast point for route waypoint
public record ForecastPoint(
        Instant time,
        double latitude,
        double longitude,
        Double tempC,
        Double windMs,
        Double precipMm
) {
}

