package dk.ek.roadsai.model;

/// Station metadata (for Vegagerdin & Vedur.is)
public record Station(
        String id,
        String name,
        double latitude,
        double longitude,
        String kind // "road", "weather", etc...
) {
}
