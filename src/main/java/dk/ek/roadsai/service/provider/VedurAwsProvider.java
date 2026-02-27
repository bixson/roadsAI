package dk.ek.roadsai.service.provider;

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


/// Vedur.is (IMO) AWS station provider
// 15 min caching to reduce load on API
@Service
public class VedurAwsProvider implements StationProvider {
    private final WebClient http = WebClient.builder()
            .baseUrl("https://api.vedur.is")
            .defaultHeader("User-Agent", "roadsai/1.0")
            .build();

    //caching
    private static final Duration TTL = Duration.ofMinutes(15); // 15 min
    private final Map<String, List<VedurAwsDto.Aws10minBasic>> cacheData = new HashMap<>();
    private final Map<String, Instant> cacheTime = new HashMap<>();

    // All-Iceland AWS station registry
    private final List<Station> registry = List.of(
            new Station("imo:1475", "Reykjavík, Faxaflói", 64.1275, -21.902, "IMO"),
            new Station("imo:1474", "Keflavík", 63.9765, -22.5899, "IMO"),
            new Station("imo:1477", "Akranes", 64.3169, -22.0792, "IMO"),
            new Station("imo:1497", "Selfoss", 63.9325, -20.9975, "IMO"),
            new Station("imo:1461", "Vík í Mýrdal", 63.4187, -19.0067, "IMO"),
            new Station("imo:1495", "Vestmannaeyjar", 63.4381, -20.2893, "IMO"),
            new Station("imo:2481", "Hólmavík", 65.6873, -21.6813, "IMO"),
            new Station("imo:2540", "Stykkishólmur", 65.0769, -22.7287, "IMO"),
            new Station("imo:2642", "Ísafjörður", 66.0596, -23.1699, "IMO"),
            new Station("imo:2643", "Akureyri", 65.6854, -18.0872, "IMO"),
            new Station("imo:2636", "Blönduós", 65.6584, -20.2881, "IMO"),
            new Station("imo:2501", "Sauðárkrókur", 65.7458, -19.6381, "IMO"),
            new Station("imo:2601", "Siglufjörður", 66.1523, -18.9098, "IMO"),
            new Station("imo:2620", "Hveravellir", 64.8706, -19.5527, "IMO"),
            new Station("imo:3133", "Egilsstaðir", 65.2667, -14.4036, "IMO"),
            new Station("imo:3166", "Neskaupstaður", 65.1437, -13.6847, "IMO"),
            new Station("imo:3009", "Höfn í Hornafirði", 64.2636, -15.2119, "IMO"),
            new Station("imo:3190", "Dalatangi", 65.2696, -13.5726, "IMO"),
            new Station("imo:1476", "Hellisheiði", 64.0238, -21.3877, "IMO"),
            new Station("imo:1492", "Landmannalaugar", 63.9833, -19.0667, "IMO"),
            new Station("imo:1488", "Kirkjubæjarklaustur", 63.7861, -18.0507, "IMO")
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
            return VedurAwsDto.map(stationId, cached).stream() // stream cached observations
                    .filter(o -> !o.timestamp().isBefore(from) && !o.timestamp().isAfter(to)) // filter by requested time window
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
                    .bodyToMono(new ParameterizedTypeReference<List<VedurAwsDto.Aws10minBasic>>() {
                    })
                    .block();

            if (response == null || response.isEmpty()) {
                return List.of();
            }

            // Update cache
            cacheData.put(id, response);
            cacheTime.put(id, Instant.now());

            return VedurAwsDto.map(stationId, response).stream() // stream fresh observations
                    .filter(o -> !o.timestamp().isBefore(from) && !o.timestamp().isAfter(to)) // filter by requested time window
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
