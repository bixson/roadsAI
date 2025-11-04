package dk.ek.roadsai.model;

public record Station(
        String id,
        String name,
        double latitude,
        double longitude,
        String kind // "road", "weather", etc...
) {
}
