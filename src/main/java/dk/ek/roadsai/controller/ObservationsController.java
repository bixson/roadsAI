package dk.ek.roadsai.controller;

import dk.ek.roadsai.dto.CapAlert;
import dk.ek.roadsai.dto.ObservationsRequest;
import dk.ek.roadsai.dto.ObservationsResponse;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import dk.ek.roadsai.service.RouteService;
import dk.ek.roadsai.service.StationService;
import dk.ek.roadsai.service.VedurCapProvider;
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

// fetched observations and CAP alerts
// returns raw observation data
@RestController
@RequestMapping("/api")
public class ObservationsController {
    private final RouteService routeService;
    private final StationService stationService;
    private final VedurCapProvider vedurCapProvider;

    public ObservationsController(
            RouteService routeService,
            StationService stationService,
            VedurCapProvider vedurCapProvider) {
        this.routeService = routeService;
        this.stationService = stationService;
        this.vedurCapProvider = vedurCapProvider;
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
            
            // Fetch latest observations (last 2 hours - APIs return recent data)
            // TODO: double check caching and minimize down to 15 min
            Instant twoHoursAgo = Instant.now().minusSeconds(7200);
            List<StationObservation> obs = stationService.fetchObsForStations(
                corridor, twoHoursAgo, Instant.now());
            
            // Fetch CAP alerts for each station
            Map<String, List<CapAlert>> stationAlerts = new HashMap<>();
            for (Station station : corridor) {
                List<CapAlert> alerts = vedurCapProvider.fetchAlerts(
                    station.latitude(), station.longitude());
                stationAlerts.put(station.id(), alerts);
            }
            
            return ResponseEntity.ok(new ObservationsResponse(
                obs,
                stationAlerts,
                corridor,
                routeGeo
            ));
        } catch (Exception e) {
            System.out.println("Error fetching observations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

