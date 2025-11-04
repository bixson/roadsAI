package dk.ek.roadsai.service;

import dk.ek.roadsai.dto.vedur.is.VedurAwsDto;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;

public class vedurAwsProvider implements StationProvider{
    private final WebClient http = WebClient.builder()
            .baseUrl("https://api.vedur.is")
            .defaultHeader("User-Agent", "roadsai/1.0")
            .build();

    private final List<Station> registry = List.of(
            // TODO: CHECK IF 'ved:' SHOULD BE PART OF id OR JUST number?
            new Station("vedur:1475", "vedur.is Reykjavík, Faxaflói", 64.1275, 21.902, "VEDUR.IS"),
            new Station("vedur:2481", "vedur.is Hólmavík", 65.6873, 21.6813, "VEDUR.IS"),
            new Station("vedur:2642", "vedur.is Ísafjörður", 66.0596, 23.1699, "VEDUR.IS")
    );

    @Override public List<Station> listStations() { return registry; }

    @Override
    public List<StationObservation> fetchObservations(String stationId, Instant from, Instant to) {
        String id = stationId.startsWith("imo:") ? stationId.substring(4) : stationId;

        // TODO: Adjust path/params to the real IMO endpoint
        VedurAwsDto dto = http.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/weather/observations/aws")
                        .queryParam("station", id)
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .build())
                .retrieve()
                .bodyToMono(VedurAwsDto.class)
                .block();

        return dto == null ? List.of() : dto.toStationObs(stationId);
    }
}
