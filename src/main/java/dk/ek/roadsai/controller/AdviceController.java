package dk.ek.roadsai.controller;

import dk.ek.roadsai.dto.AdviceRequest;
import dk.ek.roadsai.dto.AdviceResponse;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import dk.ek.roadsai.service.*;
import dk.ek.roadsai.util.GeoDistance;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdviceController {

    private final RouteService routeService = new RouteService();
    private final TimeWindowService timeWindowService = new TimeWindowService();

    private final StationProvider provider = new VegagerdinProvider();
    private final StationService stationService = new StationService();
    private final DataReducer dataReducer = new DataReducer();
    private final PromptBuilder prompt = new PromptBuilder();
    private final OpenAiService openai = new OpenAiService();

    @PostMapping(value = "/advice", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AdviceResponse advice(@RequestBody AdviceRequest request) {
        var routeGeo = routeService.getCoordinates();
        var routeKm = estimateRouteKm(routeGeo);

        var t = Instant.parse(request.timeIso());
        var timeWindow = timeWindowService.window(request.mode(), t, routeKm);

        // 1) choose stations near route
        List<Station> corridor = stationService.corridorStations(routeGeo, 5000.0);
        // 2) fetch observations per-station
        List<StationObservation> obs = stationService.fetchObsForStations(corridor, timeWindow.get("from"), timeWindow.get("to"));
        // log: mode, time, how many corridor stations + observations were fetched
        System.out.println("[Advice] mode=" + request.mode() +
                " t=" + request.timeIso() +
                " corridorStations=" + corridor.size() +
                " obs=" + obs.size());
        // 3) reduce → segment fact
        var segments = dataReducer.reduceToSegments(obs);
        // 4) build prompt (ask LLM)
        var user = prompt.buildUserPrompt("rvk-isf", request.mode(), request.timeIso(), segments);
        var aiResponse = openai.ask(prompt.systemPrompt(), user);
        // 5) build response
        Map<String, Object> summary = Map.of(
                "stationsUsed", corridor.size(),
                "window", Map.of("from", timeWindow.get("from").toString(), "to", timeWindow.get("to").toString()),
                "hazards", List.of()
        );
        Map<String, Object> mapData = Map.of(
                "route", Map.of("type", "LineString", "coordinates", routeGeo),
                "stations", corridor.stream().map(s -> Map.of(
                        "id", s.id(),
                        "name", s.name(),
                        "lon", s.longitude(),
                        "lat", s.latitude()
                )).toList(),
                "cameras", List.of()
        );
        return new AdviceResponse(aiResponse, summary, mapData);
    }

    private double estimateRouteKm(List<List<Double>> line) {
        double km = 0;
        for (int i = 0; i < line.size() -1; i++) {
            var a = line.get(i);
            var b = line.get(i+1);
            km += GeoDistance.haversineM(a.get(1), a.get(0), b.get(1), b.get(0)) / 1000.0;
        }
        return km;
    }
}
