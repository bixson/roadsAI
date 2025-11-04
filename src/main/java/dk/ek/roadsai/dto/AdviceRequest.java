package dk.ek.roadsai.dto;

public record AdviceRequest(
        String from, // "RVK" | "IFJ"
        String to, // "IFJ" | "RVK"
        String mode, // "departure" | "arrival"
        String timeIso // ISO-8601 UTC
) {}
