package dk.ek.roadsai.dto;

/// Observations request DTO
 // from frontend to backend
public record ObservationsRequest(
        String from, // "RVK" | "IFJ"
        String to,   // "IFJ" | "RVK"
        String forecastTime // ISO-8601 UTC (optional, null = current time only)
) {
}

