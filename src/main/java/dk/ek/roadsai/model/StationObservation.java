package dk.ek.roadsai.model;

import java.time.Instant;

/// Station weather observation (for Vegagerdin & Vedur.is)
public record StationObservation(
        String stationId,
        Instant timestamp,
        Double tempC,
        Double windMs,
        Double gustMs,
        Double visibilityM,
        String precipType // "rain", "snow", "sleet"
) {
}
