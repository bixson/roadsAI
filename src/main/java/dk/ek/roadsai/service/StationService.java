package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;

public class StationService {
    private final StationProvider provider;
    private final WebClient http;

    public StationService(StationProvider provider) {
        this.provider = provider;
        this.http = WebClient.builder().build();
    }

    public List<Station> corridorStations(List<List<Double>> routeLonLat, double bufferMeters, String kind) {
        var cf = new CorridorFilter();
        return cf.filterByBuffer(provider.listStations(), routeLonLat, bufferMeters);
    }

    public List<StationObservation> fetchObsForStations(List<Station> stations, Instant from, Instant to) {
        return stations.stream()
                .flatMap(s -> provider.fetchObservations(s.id(), from, to).stream())
                .toList();
    }
}
