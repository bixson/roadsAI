package dk.ek.roadsai.model;

import java.time.Instant;

/// Forecast point for route waypoint (for yr.no data)
public record ForecastPoint(
        Instant time,
        double latitude,
        double longitude,
        Double tempC,
        Double windMs,
        Double precipMm
) {
}

