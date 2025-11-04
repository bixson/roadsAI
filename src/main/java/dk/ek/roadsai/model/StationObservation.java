package dk.ek.roadsai.model;

import java.time.Instant;

public record StationObservation(
        String stationId,
        Instant timestamp,
        Double tempC,
        Double windMs,
        Double gustMs,
        Double visibilityM,
        String precipType, // "rain", "snow", "sleet"
        String roadCondition // "clear", "wet", "snowy", "icy" (when available)
) {
}
