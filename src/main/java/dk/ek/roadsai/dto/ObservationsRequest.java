package dk.ek.roadsai.dto;

public record ObservationsRequest(
        String from, // "RVK" | "IFJ"
        String to    // "IFJ" | "RVK"
) {
}

