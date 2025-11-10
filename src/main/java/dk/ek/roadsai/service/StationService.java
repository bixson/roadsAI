package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class StationService {
    private final StationProvider vegagerdin;
    private final StationProvider vedur;
    private final CorridorFilter corridorFilter;

    public StationService(VegagerdinProvider vegagerdin, VedurAwsProvider vedur, CorridorFilter corridorFilter) {
        this.vegagerdin = vegagerdin;
        this.vedur = vedur;
        this.corridorFilter = corridorFilter;
    }

    public List<Station> corridorStations(List<List<Double>> routeLonLat, double bufferM) {
        // merge both registries, then corridor-filter
        var all = Stream.concat(vegagerdin.listStations().stream(), vedur.listStations().stream()).toList();
        return corridorFilter.filterByBuffer(all, routeLonLat, bufferM);
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
