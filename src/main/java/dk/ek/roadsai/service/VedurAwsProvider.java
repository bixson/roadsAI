package dk.ek.roadsai.service;

import dk.ek.roadsai.dto.vedur.is.VedurAwsDto;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;

import java.time.Instant;
import java.util.List;

public class VedurAwsProvider implements StationProvider{
    private final WebClient http = WebClient.builder()
            .baseUrl("https://api.vedur.is")
            .defaultHeader("User-Agent", "roadsai/1.0")
            .build();

    private final List<Station> registry = List.of(
            // TODO: CHECK IF 'ved:' SHOULD BE PART OF id OR JUST number?
            new Station("imo:1475", "vedur.is Reykjavík, Faxaflói", 64.1275, -21.902, "IMO"),
            new Station("imo:2481", "vedur.is Hólmavík", 65.6873, -21.6813, "IMO"),
            new Station("imo:2642", "vedur.is Ísafjörður", 66.0596, -23.1699, "IMO")
    );

    @Override public List<Station> listStations() { return registry; }

    @Override
    public List<StationObservation> fetchObservations(String stationId, Instant from, Instant to) {
        String id = stationId.startsWith("imo:")
                ? stationId.substring("imo:".length())
                : stationId;

        /*
         * Endpoint and parameters below are based on the Weather API OpenAPI (observations/aws/*).
         * - Path: /weather/observations/aws/10min/latest
         * - Query param: station_id (array, but here we pass a single value)
         * - Optional params for clarity: parameters=basic, count=1
         */
        List<VedurAwsDto.Aws10minBasic> list = http.get()
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
        if (list == null) list = java.util.List.of();
        return VedurAwsDto.map(stationId, list);
    }
}
