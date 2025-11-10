package dk.ek.roadsai.model;

import java.time.Instant;

/**
 * Station weather observation
 */
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
