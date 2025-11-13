package dk.ek.roadsai.controller;

import dk.ek.roadsai.dto.vedur.is.CapAlert;
import dk.ek.roadsai.dto.ObservationsRequest;
import dk.ek.roadsai.dto.ObservationsResponse;
import dk.ek.roadsai.model.ForecastPoint;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import dk.ek.roadsai.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/// Fetches observations and CAP alerts, generates AI advice based on current conditions
@RestController
@RequestMapping("/api")
public class ObservationsController {
    private final RouteService routeService;
    private final StationService stationService;
    private final VedurCapProvider vedurCapProvider;
    private final ObservationReducer observationReducer;
    private final ObservationPromptBuilder promptBuilder;
    private final ObservationAiService aiService;
    private final YrNoProvider yrNoProvider;

    public ObservationsController(
            RouteService routeService,
            StationService stationService,
            VedurCapProvider vedurCapProvider,
            ObservationReducer observationReducer,
            ObservationPromptBuilder promptBuilder,
            ObservationAiService aiService,
            YrNoProvider yrNoProvider) {
        this.routeService = routeService;
        this.stationService = stationService;
        this.vedurCapProvider = vedurCapProvider;
        this.observationReducer = observationReducer;
        this.promptBuilder = promptBuilder;
        this.aiService = aiService;
        this.yrNoProvider = yrNoProvider;
    }

    @PostMapping(value = "/observations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObservationsResponse> getObservations(@RequestBody ObservationsRequest request) {
        // Validation
        if (request == null || request.from() == null || request.from().isBlank() ||
            request.to() == null || request.to().isBlank() ||
            request.from().equals(request.to())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        try {
            // Get route coordinates
            var routeGeo = routeService.getCoordinates(request.from(), request.to());
            
            // Get stations along route (5km buffer)
            List<Station> corridor = stationService.corridorStations(routeGeo, 5000.0);
            
            // Fetch latest observations (last 15 min, cache capped for precision)
            Instant fifteenMinutesAgo = Instant.now().minusSeconds(900);
            List<StationObservation> obs = stationService.fetchObsForStations(
                corridor, fifteenMinutesAgo, Instant.now());
            
            // Fetch CAP alerts for each station (30min cache in VedurCapProvider)
            Map<String, List<CapAlert>> stationAlerts = new HashMap<>();
            for (Station station : corridor) {
                List<CapAlert> alerts = vedurCapProvider.fetchAlerts(
                    station.latitude(), station.longitude());
                stationAlerts.put(station.id(), alerts);
            }
            
            // Reduce observations to station-level facts
            Map<String, ObservationReducer.StationFacts> stationFacts = 
                observationReducer.reduceToStations(obs, corridor, stationAlerts);
            
            // Parse forecast time if provided - for prompt filtering
            Instant forecastTime = null;
            List<ForecastPoint> forecasts = new ArrayList<>();
            if (request.forecastTime() != null && !request.forecastTime().isBlank()) {
                try {
                    Instant parsedTime = Instant.parse(request.forecastTime());
                    forecastTime = parsedTime;
                    // Fetch forecasts for station coordinates
                    List<ForecastPoint> allForecasts = yrNoProvider.fetchForecastForStations(corridor);
                    // Filter forecasts to requested time window
                    Instant now = Instant.now();
                    final Instant finalForecastTime = forecastTime;
                    forecasts = allForecasts.stream()
                            .filter(f -> !f.time().isBefore(now) && 
                                       (f.time().isBefore(finalForecastTime) || f.time().equals(finalForecastTime)))
                            .collect(Collectors.toList());
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid forecastTime format: " + request.forecastTime());
                    // Continue without forecasts
                }
            }
            
            // Generate AI advice based on observations (and forecasts, if requested)
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(
                "rvk-isf", request.from(), request.to(), stationFacts, corridor, forecasts, forecastTime);
            List<String> advice = aiService.ask(systemPrompt, userPrompt, corridor.size());
            
            return ResponseEntity.ok(new ObservationsResponse(
                obs,
                stationAlerts,
                corridor,
                routeGeo,
                advice,
                forecasts
            ));
        } catch (Exception e) {
            System.out.println("Error fetching observations: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

