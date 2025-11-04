package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;


public class VegagerdinProvider implements StationProvider {
    private final WebClient http = WebClient.builder().build();

    @Override
    public List<Station> listStations() {
        // TODO: FIX ONLY STATIONS ALONG ROUTE
        return List.of(
                new Station("holt_n", "Holtavörðuheiði N", 65.1270, -21.3325, "road"),
                new Station("bratta",  "Brattabrekka",      64.7220, -21.7670, "road")
                // ... add the real ones you like (IDs must match the API you call)
        );
    }

    @Override
    public List<StationObservation> fetchObservations(String stationId, Instant from, Instant to) {
        // ⚠️ Replace with real API call for each stationId.
        // Example pattern (pseudo):
        // var url = "https://api.vegagerdin.is/stations/" + stationId + "?from=" + from + "&to=" + to;
        // var dto = http.get().uri(url).retrieve().bodyToMono(VGObsDTO.class).block();
        // return dto.toStationObs();
        return List.of(); // placeholder until you wire real calls
    }
}
