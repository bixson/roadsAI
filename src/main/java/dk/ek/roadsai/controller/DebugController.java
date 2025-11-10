package dk.ek.roadsai.controller;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import dk.ek.roadsai.service.RouteService;
import dk.ek.roadsai.service.StationService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * nice debug endpoints for testing and troubleshooting
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final RouteService routeService;
    private final StationService stationService;

    public DebugController(RouteService routeService, StationService stationService) {
        this.routeService = routeService;
        this.stationService = stationService;
    }

    /// GET /api/debug/corridor?bufferM=8000(8km)
    /// returns stations within bufferM meters of route
    @GetMapping("/corridor")
    public Map<String, Object> corridor(@RequestParam(defaultValue = "8000") double bufferM) {
        var coords = routeService.getCoordinates(); // RVK↔ÍSAF json route
        List<Station> corridor = stationService.corridorStations(coords, bufferM);
        return Map.of("bufferM", bufferM, "count", corridor.size(), "stations", corridor);
    }

    /// GET /api/debug/obs?hours=2
    /// returns time observations from stations within 8km corridor for past (2) hours
    @GetMapping("/obs")
    public Map<String, Object> obs(@RequestParam(defaultValue = "2") int hours) {
        var coords = routeService.getCoordinates();
        var corridor = stationService.corridorStations(coords, 8000);
        var to = Instant.now();
        var from = to.minusSeconds(hours * 3600L);
        List<StationObservation> data = stationService.fetchObsForStations(corridor, from, to);
        return Map.of("from", from, "to", to, "stations", corridor.size(), "obs", data);
    }

    @GetMapping("/imo")
    public Map<String, Object> imo(@RequestParam String stationId) {
        var from = Instant.now().minusSeconds(3600);
        var to = Instant.now();

        var st = new Station("imo:" + stationId, "IMO " + stationId, 0, 0, "IMO");
        var obs = stationService.fetchObsForStations(List.of(st), from, to);
        return Map.of("stationId", stationId, "count", obs.size(), "obs", obs);
    }
}
