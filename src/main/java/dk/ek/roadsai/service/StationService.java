package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import dk.ek.roadsai.service.provider.VegagerdinProvider;
import dk.ek.roadsai.service.provider.VedurAwsProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/// combined station service for Vegagerðin and vedur.is(IMO)
/// Simplified: Stations are hardcoded for RVK↔IFJ route, so no buffer filtering needed
@Service
public class StationService {
    private final VegagerdinProvider vegagerdin;
    private final VedurAwsProvider vedur;

    public StationService(VegagerdinProvider vegagerdin, VedurAwsProvider vedur) {
        this.vegagerdin = vegagerdin;
        this.vedur = vedur;
    }


    // Stations from both providers merged and sorted by latitude (reversed if route is IFJ → RVK)
    public List<Station> corridorStations(List<List<Double>> routeLonLat) {
        // Merge stations from both providers
        var allStations = Stream.concat(vegagerdin.listStations().stream(), vedur.listStations().stream())
                .toList();
        
        // Sort by latitude (south to north) - base order: RVK → IFJ
        List<Station> sorted = allStations.stream()
                .sorted((s1, s2) -> Double.compare(s1.latitude(), s2.latitude()))
                .toList();
        
        // Check if route is reversed (IFJ → RVK) by comparing first and last route coordinates
        // If route starts further north than it ends, it's reversed
        if (routeLonLat.size() >= 2) {
            double firstLat = routeLonLat.get(0).get(1);  // First waypoint latitude
            double lastLat = routeLonLat.get(routeLonLat.size() - 1).get(1);  // Last waypoint latitude
            
            // If first waypoint is further north than last, route is reversed (IFJ → RVK)
            if (firstLat > lastLat) {
                return sorted.reversed();
            }
        }
        
        return sorted;  // RVK → IFJ
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
