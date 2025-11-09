package dk.ek.roadsai.controller;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import dk.ek.roadsai.service.RouteService;
import dk.ek.roadsai.service.StationService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final RouteService route = new RouteService();
    private final StationService stations = new StationService();

    @GetMapping("/corridor")
    public Map<String, Object> corridor(@RequestParam(defaultValue = "8000") double bufferM) {
        var coords = route.getCoordinates(); // RVK↔ÍSAF
        List<Station> corridor = stations.corridorStations(coords, bufferM);
        return Map.of("bufferM", bufferM, "count", corridor.size(), "stations", corridor);
    }

    @GetMapping("/obs")
    public Map<String, Object> obs(@RequestParam(defaultValue = "2") int hours) {
        var coords = route.getCoordinates();
        var corridor = stations.corridorStations(coords, 8000);
        var to = Instant.now();
        var from = to.minusSeconds(hours * 3600L);
        List<StationObservation> data = stations.fetchObsForStations(corridor, from, to);
        return Map.of("from", from, "to", to, "stations", corridor.size(), "obs", data);
    }

    @GetMapping("/imo")
    public Map<String, Object> imo(@RequestParam String stationId) {
        var svc = new StationService(); // or inject
        var from = Instant.now().minusSeconds(3600);
        var to = Instant.now();

        var st = new Station("imo:" + stationId, "IMO " + stationId, 0, 0, "IMO");
        var obs = svc.fetchObsForStations(List.of(st), from, to);
        return Map.of("stationId", stationId, "count", obs.size(), "obs", obs);
    }
}
