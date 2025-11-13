package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/// combined station service for Vegagerðin and vedur.is(IMO)
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

    // return all stations within bufferM meters of route defined by routeLonLat (ordered)
    public List<Station> corridorStations(List<List<Double>> routeLonLat, double bufferM) {
        var all = Stream.concat(vegagerdin.listStations().stream(), vedur.listStations().stream()).toList(); // merge veg: + imo: stations
        return corridorFilter.filterByBuffer(all, routeLonLat, bufferM); //return correct ordered station-list
    }

    // fetch obs from providers, return combined obs-list
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
