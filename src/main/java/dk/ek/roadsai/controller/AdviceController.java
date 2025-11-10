package dk.ek.roadsai.controller;

import dk.ek.roadsai.dto.AdviceRequest;
import dk.ek.roadsai.dto.AdviceResponse;
import dk.ek.roadsai.dto.CapAlert;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import dk.ek.roadsai.service.*;
import dk.ek.roadsai.util.GeoDistance;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * advice(): receives route/time, fetches weather station data, returns AI advice with map coordinates
 * estimateRouteKm(): Sums the haversine distances between each coordinate pair,on route, returns total length in km
 */
@RestController
@RequestMapping("/api")
public class AdviceController {

    private final RouteService routeService;
    private final TimeWindowService timeWindowService;
    private final StationService stationService;
    private final DataReducer dataReducer;
    private final PromptBuilder promptBuilder;
    private final OpenAiService openAiService;
    private final HazardDetector hazardDetector;
    private final VedurCapProvider vedurCapProvider;

    public AdviceController(RouteService routeService, TimeWindowService timeWindowService,
                            StationService stationService, DataReducer dataReducer,
                            PromptBuilder promptBuilder, OpenAiService openAiService,
                            HazardDetector hazardDetector, VedurCapProvider vedurCapProvider) {
        this.routeService = routeService;
        this.timeWindowService = timeWindowService;
        this.stationService = stationService;
        this.dataReducer = dataReducer;
        this.promptBuilder = promptBuilder;
        this.openAiService = openAiService;
        this.hazardDetector = hazardDetector;
        this.vedurCapProvider = vedurCapProvider;
    }

    @PostMapping(value = "/advice", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdviceResponse> advice(@RequestBody AdviceRequest request) {
        // request validation
        if (request == null || request.from() == null || request.from().isBlank() ||
            request.to() == null || request.to().isBlank() ||
            request.mode() == null || request.mode().isBlank() ||
            request.timeIso() == null || request.timeIso().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AdviceResponse(
                    List.of("Invalid request", "Missing required fields", "Check from, to, mode, and timeIso", "All fields required", "Please provide valid input", "Request format error", "Try again", "Invalid input", "Retry request"),
                    Map.of("stationsUsed", 0, "window", Map.of(), "hazards", List.of()),
                    Map.of("route", Map.of(), "stations", List.of())
            ));
        }
        
        try { // Fetch route, get weather data, generate AI advice, build response
            // Fetch route
            var routeGeo = routeService.getCoordinates();
            var routeKm = estimateRouteKm(routeGeo);

            var t = Instant.parse(request.timeIso());
            var timeWindow = timeWindowService.window(request.mode(), t, routeKm);

            List<Station> corridor = stationService.corridorStations(routeGeo, 5000.0); // choose stations near route

            List<StationObservation> obs = stationService.fetchObsForStations(corridor, timeWindow.get("from"), timeWindow.get("to")); // fetch observations per-station
            
            // Fetch CAP alerts for each station
            Map<String, List<CapAlert>> stationAlerts = new HashMap<>();
            for (Station station : corridor) {
                List<CapAlert> alerts = vedurCapProvider.fetchAlerts(station.latitude(), station.longitude());
                stationAlerts.put(station.id(), alerts);
            }
            
            // log: mode, time, (amount) corridor stations + observations
            System.out.println("[Advice] mode=" + request.mode() +
                    " t=" + request.timeIso() +
                    " corridorStations=" + corridor.size() +
                    " obs=" + obs.size());

            var segments = dataReducer.reduceToSegments(obs, corridor, stationAlerts); // reduce → segment fact with station names
            var hazards = hazardDetector.detectHazards(segments); // detect hazards from API data
            var user = promptBuilder.buildUserPrompt("rvk-isf", request.mode(), request.timeIso(), segments); // build prompt (ask LLM)
            var aiResponse = openAiService.ask(promptBuilder.systemPrompt(), user);
            // build response
            Map<String, Object> summary = Map.of(
                    "stationsUsed", corridor.size(),
                    "window", Map.of("from", timeWindow.get("from").toString(), "to", timeWindow.get("to").toString()), // time window used
                    "hazards", hazards // hazards detected from API data
            );
            Map<String, Object> mapData = Map.of(
                    "route", Map.of("type", "LineString", "coordinates", routeGeo), // route coordinates
                    "stations", corridor.stream().map(s -> Map.of( // stations on route
                            "id", s.id(),
                            "name", s.name(),
                            "lon", s.longitude(),
                            "lat", s.latitude()
                    )).toList()
            );
            return ResponseEntity.ok(new AdviceResponse(aiResponse, summary, mapData));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AdviceResponse(
                    List.of("Error occurred", "Invalid request", "Check time format", "Try again", "Service unavailable", "Please retry", "Connection issue", "API error", "Retry request"),
                    Map.of("stationsUsed", 0, "window", Map.of(), "hazards", List.of()),
                    Map.of("route", Map.of(), "stations", List.of())
            ));
        }
    }

    // Estimate route length in kilometers using haversine formula
    private double estimateRouteKm(List<List<Double>> line) { // list of a list of [lon, lat], yes.
        double km = 0;
        for (int i = 0; i < line.size() - 1; i++) {
            var a = line.get(i);
            var b = line.get(i + 1);
            km += GeoDistance.haversineM(a.get(1), a.get(0), b.get(1), b.get(0)) / 1000.0;
        }
        return km;
    }
}
