package dk.ek.roadsai.service;

import dk.ek.roadsai.dto.vedur.is.VedurAwsDto;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vedur.is (IMO) AWS station provider
 * 15 min caching to reduce load on API
 */
@Service
public class VedurAwsProvider implements StationProvider {
    private final WebClient http = WebClient.builder()
            .baseUrl("https://api.vedur.is")
            .defaultHeader("User-Agent", "roadsai/1.0")
            .build();

    //caching
    private static final Duration TTL = Duration.ofMinutes(15); // 15 min
    private Map<String, List<VedurAwsDto.Aws10minBasic>> cacheData = new HashMap<>();
    private Map<String, Instant> cacheTime = new HashMap<>();

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

        // return cached data if less than 15 minutes old
        List<VedurAwsDto.Aws10minBasic> cached = cacheData.get(id);
        Instant cachedTime = cacheTime.get(id);
        if (cached != null && cachedTime != null && Duration.between(cachedTime, Instant.now()).compareTo(TTL) < 0) {
            // Accept observations from last 2 hours (winter weather reporting can be delayed)
            Instant twoHoursAgo = Instant.now().minusSeconds(7200);
            return VedurAwsDto.map(stationId, cached).stream()
                    .filter(o -> o.timestamp().isAfter(twoHoursAgo))
                    .toList();
        }

        // Fetch fresh observations for requested station
        try {
            List<VedurAwsDto.Aws10minBasic> response = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather/observations/aws/10min/latest")
                            .queryParam("station_id", id)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.value() >= 400,
                            resp -> resp.bodyToMono(String.class).map(body ->
                                    new RuntimeException("IMO latest failed " + resp.statusCode() + " body=" + body)))
                    .bodyToMono(new ParameterizedTypeReference<List<VedurAwsDto.Aws10minBasic>>() {})
                    .block();

            if (response == null || response.isEmpty()) {
                return List.of();
            }

            // Update cache
            cacheData.put(id, response);
            cacheTime.put(id, Instant.now());

            // Accept observations from last 2 hours (winter weather reporting can be delayed)
            Instant twoHoursAgo = Instant.now().minusSeconds(7200);
            return VedurAwsDto.map(stationId, response).stream()
                    .filter(o -> o.timestamp().isAfter(twoHoursAgo))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
