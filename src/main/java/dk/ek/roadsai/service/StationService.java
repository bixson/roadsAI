package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class StationService {
    private final StationProvider vegagerdin = new VegagerdinProvider();
    private final StationProvider vedur = new vedurAwsProvider();

    public List<Station> corridorStations(List<List<Double>> routeLonLat, double bufferM) {
        // merge both registries, then corridor-filter
        var all = Stream.concat(vegagerdin.listStations().stream(), vedur.listStations().stream()).toList();
        return new CorridorFilter().filterByBuffer(all, routeLonLat, bufferM);
    }

    public List<StationObservation> fetchObsForStations(List<Station> stations, Instant from, Instant to) {
        List<StationObservation> out = new ArrayList<>();
        for (var st : stations) {
            if ("VEGAGERDIN".equals(st.kind())) {
                out.addAll(vegagerdin.fetchObservations(st.id(), from, to));
            } else if ("IMO".equals(st.kind())) {
                out.addAll(vedur.fetchObservations(st.id(), from, to));
            }
        }
        return out;
    }
}
