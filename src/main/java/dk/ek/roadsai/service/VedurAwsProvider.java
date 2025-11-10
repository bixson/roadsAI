package dk.ek.roadsai.service;

import dk.ek.roadsai.dto.vedur.is.VedurAwsDto;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Vedur.is (IMO) AWS station provider
 * 90 sek caching to reduce load on API
 */
@Service
public class VedurAwsProvider implements StationProvider {
    private final WebClient http = WebClient.builder()
            .baseUrl("https://api.vedur.is")
            .defaultHeader("User-Agent", "roadsai/1.0")
            .build();

    //caching
    private static final Duration TTL = Duration.ofSeconds(90);
    private Instant lastFetchAt = Instant.EPOCH;
    private List<VedurAwsDto.Aws10minBasic> lastResponse = null;
    //fixed for RVK↔IFJ
    private final List<Station> registry = List.of(
            new Station("imo:1475", "vedur.is Reykjavík, Faxaflói", 64.1275, -21.902, "IMO"),
            new Station("imo:2481", "vedur.is Hólmavík", 65.6873, -21.6813, "IMO"),
            new Station("imo:2642", "vedur.is Ísafjörður", 66.0596, -23.1699, "IMO")
    );

    @Override
    public List<Station> listStations() {
        return registry;
    }

    @Override
    public List<StationObservation> fetchObservations(String stationId, Instant from, Instant to) {
        // Strip "imo:" prefix if present for API compatibility
        String id = stationId.startsWith("imo:")
                ? stationId.substring("imo:".length())
                : stationId;

        // Fetch all stations in bulk (with caching) to reduce API calls
        List<VedurAwsDto.Aws10minBasic> allStations = getAllStations();
        if (allStations == null || allStations.isEmpty()) {
            return List.of();
        }

        // Filter bulk response for requested station and time window
        return allStations.stream()
                .filter(s -> s.stationId != null && s.stationId.equals(id)) // Match station ID
                .flatMap(s -> VedurAwsDto.map(stationId, List.of(s)).stream()) // Convert DTO to model
                .filter(o -> !o.timestamp().isBefore(from) && !o.timestamp().isAfter(to)) // Apply time filter
                .toList();
    }

    // get all cached stations, refresh if cache expired
    private List<VedurAwsDto.Aws10minBasic> getAllStations() {
        // Check cache validity: return cached data if less than 90 seconds old
        if (lastResponse != null && Duration.between(lastFetchAt, Instant.now()).compareTo(TTL) < 0) {
            return lastResponse;
        }
        // else fetch fresh data + update cache
        try {
            List<VedurAwsDto.Aws10minBasic> response = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather/observations/aws/10min/latest") // Latest 10-minute observations
                            .queryParam("station_id", "1475,2481,2642") // Bulk request for all route stations
                            .build())
                    .retrieve()
                    // Convert HTTP errors (4xx/5xx) to exceptions with response body for debugging
                    .onStatus(status -> status.value() >= 400,
                            resp -> resp.bodyToMono(String.class).map(body ->
                                    new RuntimeException("IMO latest failed " + resp.statusCode() + " body=" + body)))
                    .bodyToMono(new ParameterizedTypeReference<List<VedurAwsDto.Aws10minBasic>>() {})
                    .block();
            // Update cache with fresh data
            lastResponse = response != null ? response : List.of();
            lastFetchAt = Instant.now();
            return lastResponse;
        } catch (Exception e) {
            return lastResponse != null ? lastResponse : List.of(); // return empty list on error
        }
    }
}
