package dk.ek.roadsai.controller;

import dk.ek.roadsai.dto.vedur.is.CapAlert;
import dk.ek.roadsai.model.RoadClosure;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import dk.ek.roadsai.service.RoadClosureService;
import dk.ek.roadsai.service.ai.ObservationAiService;
import dk.ek.roadsai.service.provider.VedurAwsProvider;
import dk.ek.roadsai.service.provider.VedurCapProvider;
import dk.ek.roadsai.service.provider.VegagerdinProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * V2 API Controller - Google Maps integration for interactive map experience.
 * Endpoints:
 * - GET /api/v2/maps-config - Get Google Maps configuration
 * - POST /api/v2/route - Calculate route with weather data
 */
@RestController
@RequestMapping("/api/v2")
public class V2RouteController {

    private final VedurAwsProvider vedurAwsProvider;
    private final VedurCapProvider vedurCapProvider;
    private final VegagerdinProvider vegagerdinProvider;
    private final ObservationAiService aiService;
    private final RoadClosureService roadClosureService;
    private final WebClient googleMapsClient;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    // Hardcoded route: Reykjavík ↔ Ísafjörður (via Hólmavík)
    private static final List<List<Double>> RVK_IFJ_ROUTE = List.of(
            List.of(-21.8046, 64.1238), // Reykjavík
            List.of(-21.9603, 64.4755), // Hafnarfjall
            List.of(-21.9101, 64.5439), // Borgarnes
            List.of(-21.5154, 64.8716), // Brattabrekka
            List.of(-21.7632, 65.1082), // Búðardalur
            List.of(-21.8330, 65.5524), // Þröskuldar
            List.of(-21.6951, 65.7015), // Hólmavík
            List.of(-22.1291, 65.7503), // Steingrímsfjarðarheiði
            List.of(-22.7303, 66.0403), // Ögur
            List.of(-22.9888, 66.0279), // Súðavík
            List.of(-23.0465, 66.0977), // Arnarfjörður
            List.of(-23.1239, 66.0746) // Ísafjörður
    );

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @Value("${google.maps.map.id:}")
    private String googleMapsMapId;

    public V2RouteController(
            VedurAwsProvider vedurAwsProvider,
            VedurCapProvider vedurCapProvider,
            VegagerdinProvider vegagerdinProvider,
            ObservationAiService aiService,
            RoadClosureService roadClosureService) {
        this.vedurAwsProvider = vedurAwsProvider;
        this.vedurCapProvider = vedurCapProvider;
        this.vegagerdinProvider = vegagerdinProvider;
        this.aiService = aiService;
        this.roadClosureService = roadClosureService;
        // Increase buffer size to handle large route responses (16MB)
        this.googleMapsClient = WebClient.builder()
                .baseUrl("https://routes.googleapis.com")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /**
     * Get Google Maps configuration for frontend.
     */
    @GetMapping("/maps-config")
    public ResponseEntity<Map<String, Object>> getMapsConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("configured", !googleMapsApiKey.isBlank());

        if (!googleMapsApiKey.isBlank()) {
            config.put("apiKey", googleMapsApiKey);
            if (!googleMapsMapId.isBlank()) {
                config.put("mapId", googleMapsMapId);
            }
        }

        return ResponseEntity.ok(config);
    }

    /**
     * Debug endpoint to check API configuration status.
     */
    @GetMapping("/config-status")
    public ResponseEntity<Map<String, Object>> getConfigStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("googleMapsConfigured", !googleMapsApiKey.isBlank());
        status.put("googleMapsMapIdConfigured", !googleMapsMapId.isBlank());
        status.put("openAiConfigured", aiService.isConfigured());
        return ResponseEntity.ok(status);
    }

    /**
     * Calculate route with weather data.
     */
    @PostMapping("/route")
    public ResponseEntity<?> calculateRoute(@RequestBody RouteRequest request) {
        try {
            // Validate request
            if (request.origin == null || request.origin.isBlank()) {
                return badRequest("Origin is required");
            }
            if (request.destination == null || request.destination.isBlank()) {
                return badRequest("Destination is required");
            }

            // Try hardcoded route first
            RouteResult routeResult = tryHardcodedRoute(request.origin, request.destination);

            // Fall back to Google Routes API
            if (routeResult == null) {
                routeResult = getGoogleRoute(request.origin, request.destination);
            }

            if (routeResult == null || routeResult.coordinates.isEmpty()) {
                return badRequest("Could not calculate route between these locations");
            }

            // Find weather stations along route
            double corridorWidth = request.corridorWidthKm != null ? request.corridorWidthKm : 15.0;
            List<Station> stations = findStationsAlongRoute(routeResult.coordinates, corridorWidth);

            // Fetch weather data + closures in parallel
            CompletableFuture<List<StationObservation>> observationsFuture =
                CompletableFuture.supplyAsync(() -> fetchObservations(stations, request.forecastTime), executorService);
            CompletableFuture<Map<String, List<CapAlert>>> alertsFuture =
                CompletableFuture.supplyAsync(() -> fetchAlerts(stations), executorService);
            CompletableFuture<List<RoadClosure>> closuresFuture =
                CompletableFuture.supplyAsync(() -> roadClosureService.fetchClosures(), executorService);

            // Wait for all to complete
            List<StationObservation> observations = observationsFuture.join();
            Map<String, List<CapAlert>> alerts = alertsFuture.join();
            List<RoadClosure> closures = closuresFuture.join();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("routeCoordinates", routeResult.coordinates);
            response.put("stations", stations);
            response.put("observations", observations);
            response.put("alerts", alerts);
            response.put("closures", closures);
            response.put("routeInfo", Map.of(
                    "distanceKm", routeResult.distanceKm,
                    "durationMinutes", routeResult.durationMinutes));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Generate AI advice for a route (called asynchronously by frontend).
     */
    @PostMapping("/advice")
    public ResponseEntity<?> getRouteAdvice(@RequestBody RouteRequest request) {
        try {
            // Re-calculate route context to find stations
            // (In a production app, we might pass station IDs, but re-calc is safe and
            // stateless)
            if (request.origin == null || request.destination == null) {
                return badRequest("Origin and Destination required");
            }

            RouteResult routeResult = tryHardcodedRoute(request.origin, request.destination);
            if (routeResult == null) {
                routeResult = getGoogleRoute(request.origin, request.destination);
            }

            if (routeResult == null || routeResult.coordinates.isEmpty()) {
                return badRequest("Could not calculate route");
            }

            double corridorWidth = request.corridorWidthKm != null ? request.corridorWidthKm : 15.0;
            List<Station> stations = findStationsAlongRoute(routeResult.coordinates, corridorWidth);
            List<StationObservation> observations = fetchObservations(stations, request.forecastTime);
            Map<String, List<CapAlert>> alerts = fetchAlerts(stations);

            // Generate advice
            List<String> advice = generateAdvice(stations, observations, alerts);
            List<RoadClosure> closures = roadClosureService.fetchClosures();

            return ResponseEntity.ok(Map.of("advice", advice, "closures", closures));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", true, "message", e.getMessage()));
        }
    }

    /**
     * Check for hardcoded routes and use waypoints with Google Routes API for
     * proper road polyline.
     */
    private RouteResult tryHardcodedRoute(String origin, String destination) {
        String from = normalizeLocation(origin);
        String to = normalizeLocation(destination);

        boolean isRvkIfj = (from.equals("RVK") || from.equals("REYKJAVIK")) &&
                (to.equals("IFJ") || to.equals("ISAFJORDUR"));
        boolean isIfjRvk = (from.equals("IFJ") || from.equals("ISAFJORDUR")) &&
                (to.equals("RVK") || to.equals("REYKJAVIK"));

        if (!isRvkIfj && !isIfjRvk) {
            return null; // No hardcoded route available
        }

        // Get proper road-following polyline using Google with our waypoints
        List<List<Double>> waypoints = isIfjRvk ? new ArrayList<>(RVK_IFJ_ROUTE.reversed())
                : new ArrayList<>(RVK_IFJ_ROUTE);

        return getGoogleRouteWithWaypoints(waypoints);
    }

    private String normalizeLocation(String location) {
        if (location == null)
            return "";
        String norm = location.toUpperCase()
                .replace("Í", "I")
                .replace("Ö", "O")
                .replace("Ð", "D")
                .replaceAll("[^A-Z]", "");
        return norm;
    }

    /**
     * Get route from Google Routes API using specific waypoints (for hardcoded
     * routes).
     */
    private RouteResult getGoogleRouteWithWaypoints(List<List<Double>> waypoints) {
        if (googleMapsApiKey.isBlank() || waypoints.size() < 2) {
            return null;
        }

        try {
            // First and last are origin/destination
            List<Double> originCoord = waypoints.get(0);
            List<Double> destCoord = waypoints.get(waypoints.size() - 1);

            // Build request body with waypoints
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("origin", Map.of(
                    "location", Map.of(
                            "latLng", Map.of("latitude", originCoord.get(1), "longitude", originCoord.get(0)))));
            requestBody.put("destination", Map.of(
                    "location", Map.of(
                            "latLng", Map.of("latitude", destCoord.get(1), "longitude", destCoord.get(0)))));

            // Add intermediate waypoints
            if (waypoints.size() > 2) {
                List<Map<String, Object>> intermediates = new ArrayList<>();
                for (int i = 1; i < waypoints.size() - 1; i++) {
                    List<Double> wp = waypoints.get(i);
                    intermediates.add(Map.of(
                            "location", Map.of(
                                    "latLng", Map.of("latitude", wp.get(1), "longitude", wp.get(0)))));
                }
                requestBody.put("intermediates", intermediates);
            }

            requestBody.put("travelMode", "DRIVE");
            requestBody.put("polylineEncoding", "GEO_JSON_LINESTRING");

            // Call Google Routes API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = googleMapsClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", googleMapsApiKey)
                    .header("X-Goog-FieldMask",
                            "routes.duration,routes.distanceMeters,routes.polyline.geoJsonLinestring")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseGoogleRouteResponse(response);

        } catch (Exception e) {
            // Fallback to straight lines if Google fails
            RouteResult result = new RouteResult();
            result.coordinates = waypoints;
            result.distanceKm = 460;
            result.durationMinutes = 330;
            return result;
        }
    }

    /**
     * Get route from Google Routes API by address.
     */
    private RouteResult getGoogleRoute(String origin, String destination) {
        if (googleMapsApiKey.isBlank()) {
            throw new IllegalStateException("Google Maps API key not configured");
        }

        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("origin", Map.of(
                    "address", origin + ", Iceland"));
            requestBody.put("destination", Map.of(
                    "address", destination + ", Iceland"));
            requestBody.put("travelMode", "DRIVE");
            requestBody.put("polylineEncoding", "GEO_JSON_LINESTRING");

            // Call Google Routes API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = googleMapsClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", googleMapsApiKey)
                    .header("X-Goog-FieldMask",
                            "routes.duration,routes.distanceMeters,routes.polyline.geoJsonLinestring")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseGoogleRouteResponse(response);

        } catch (Exception e) {
            throw new RuntimeException("Google Routes API error: " + e.getMessage(), e);
        }
    }

    /**
     * Parse Google Routes API response.
     */
    @SuppressWarnings("unchecked")
    private RouteResult parseGoogleRouteResponse(Map<String, Object> response) {
        if (response == null || !response.containsKey("routes")) {
            return null;
        }

        List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
        if (routes.isEmpty()) {
            return null;
        }

        Map<String, Object> route = routes.get(0);

        // Extract distance and duration
        int distanceMeters = (Integer) route.getOrDefault("distanceMeters", 0);
        String durationStr = (String) route.getOrDefault("duration", "0s");
        int durationSeconds = parseDuration(durationStr);

        // Extract coordinates from GeoJSON LineString
        Map<String, Object> polyline = (Map<String, Object>) route.get("polyline");
        Map<String, Object> geoJson = (Map<String, Object>) polyline.get("geoJsonLinestring");
        List<List<Double>> coordinates = (List<List<Double>>) geoJson.get("coordinates");

        RouteResult result = new RouteResult();
        result.coordinates = coordinates;
        result.distanceKm = distanceMeters / 1000.0;
        result.durationMinutes = durationSeconds / 60;

        return result;
    }

    private int parseDuration(String duration) {
        // Format: "12345s"
        if (duration == null || duration.isEmpty())
            return 0;
        try {
            return Integer.parseInt(duration.replace("s", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Find weather stations within corridor of route - OPTIMIZED.
     * Instead of checking every route coordinate against every station,
     * we sample route coordinates and use spatial indexing.
     */
    private List<Station> findStationsAlongRoute(List<List<Double>> routeCoords, double corridorWidthKm) {
        // Get all stations from both providers
        List<Station> allStations = Stream.concat(
                vedurAwsProvider.listStations().stream(),
                vegagerdinProvider.listStations().stream()).toList();

        if (allStations.isEmpty() || routeCoords.isEmpty()) {
            return List.of();
        }

        Set<String> addedIds = new HashSet<>();
        List<Station> result = new ArrayList<>();

        // OPTIMIZATION: Sample route coordinates instead of checking every single one
        // For long routes, checking every coordinate is wasteful
        int sampleStep = Math.max(1, routeCoords.size() / 50); // Sample up to 50 points
        
        for (int i = 0; i < routeCoords.size(); i += sampleStep) {
            List<Double> coord = routeCoords.get(i);
            double routeLon = coord.get(0);
            double routeLat = coord.get(1);

            // Check stations - break early if we've found enough
            for (Station station : allStations) {
                if (addedIds.contains(station.id()))
                    continue;

                double distance = haversineKm(routeLat, routeLon, station.latitude(), station.longitude());
                if (distance <= corridorWidthKm) {
                    result.add(station);
                    addedIds.add(station.id());
                    
                    // Early exit if we have enough stations
                    if (result.size() >= 15) {
                        break;
                    }
                }
            }
            
            // Early exit if we have enough stations
            if (result.size() >= 15) {
                break;
            }
        }

        // Sort by latitude (north to south for typical Iceland routes)
        result.sort((a, b) -> Double.compare(b.latitude(), a.latitude()));

        // Limit to max 15 stations for display
        if (result.size() > 15) {
            result = limitStations(result, 15);
        }

        return result;
    }

    private List<Station> limitStations(List<Station> stations, int max) {
        if (stations.size() <= max)
            return stations;

        List<Station> result = new ArrayList<>();
        double step = (double) stations.size() / max;

        for (int i = 0; i < max; i++) {
            int index = (int) (i * step);
            if (index < stations.size()) {
                result.add(stations.get(index));
            }
        }

        return result;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Fetch observations for stations in parallel.
     */
    private List<StationObservation> fetchObservations(List<Station> stations, String forecastTime) {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofMinutes(30));

        // Fetch observations in parallel
        List<CompletableFuture<StationObservation>> futures = stations.stream()
                .map(station -> CompletableFuture.supplyAsync(() -> {
                    try {
                        List<StationObservation> stationObs;
                        if ("IMO".equals(station.kind())) {
                            stationObs = vedurAwsProvider.fetchObservations(station.id(), from, now);
                        } else if ("VEGAGERDIN".equals(station.kind())) {
                            stationObs = vegagerdinProvider.fetchObservations(station.id(), from, now);
                        } else {
                            return null;
                        }

                        // Get latest observation
                        if (!stationObs.isEmpty()) {
                            return stationObs.stream()
                                    .max((a, b) -> a.timestamp().compareTo(b.timestamp()))
                                    .orElse(null);
                        }
                    } catch (Exception e) {
                        // Skip failed stations
                    }
                    return null;
                }, executorService))
                .collect(Collectors.toList());

        // Collect results
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Fetch CAP alerts for stations in parallel.
     */
    private Map<String, List<CapAlert>> fetchAlerts(List<Station> stations) {
        // Fetch alerts in parallel
        List<CompletableFuture<Map.Entry<String, List<CapAlert>>>> futures = stations.stream()
                .map(station -> CompletableFuture.supplyAsync(() -> {
                    try {
                        List<CapAlert> stationAlerts = vedurCapProvider.fetchAlerts(
                                station.latitude(), station.longitude());
                        if (stationAlerts != null && !stationAlerts.isEmpty()) {
                            return Map.entry(station.id(), stationAlerts);
                        }
                    } catch (Exception e) {
                        // Skip failed stations
                    }
                    return null;
                }, executorService))
                .collect(Collectors.toList());

        // Collect results into map
        Map<String, List<CapAlert>> alerts = new HashMap<>();
        futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .forEach(entry -> alerts.put(entry.getKey(), entry.getValue()));

        return alerts;
    }

    /**
     * Generate AI driving advice.
     */
    private List<String> generateAdvice(List<Station> stations,
            List<StationObservation> observations,
            Map<String, List<CapAlert>> alerts) {
        try {
            String systemPrompt = """
                    You are a professional Iceland road safety advisor analyzing real-time weather observations.
                    Provide concise, data-driven driving advice based on actual measured conditions.
                    Avoid generic phrases - focus on specific conditions and actionable guidance.
                    Use metric units (m/s for wind, °C for temperature).
                    """;

            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("Analyze weather conditions for this Iceland road route.\n\n");

            // Add alerts if any
            boolean hasAlerts = alerts.values().stream().anyMatch(list -> !list.isEmpty());
            if (hasAlerts) {
                userPrompt.append("⚠️ OFFICIAL WEATHER ALERTS:\n");
                alerts.forEach((stationId, stationAlerts) -> {
                    Station station = stations.stream()
                            .filter(s -> s.id().equals(stationId))
                            .findFirst().orElse(null);
                    String name = station != null ? station.name() : stationId;
                    for (CapAlert alert : stationAlerts) {
                        userPrompt.append("- ").append(name).append(": ");
                        if (alert.headline != null)
                            userPrompt.append(alert.headline);
                        if (alert.severity != null)
                            userPrompt.append(" [").append(alert.severity).append("]");
                        userPrompt.append("\n");
                    }
                });
                userPrompt.append("\n");
            }

            // Add observations
            userPrompt.append("Current Observations:\n");
            for (Station station : stations) {
                StationObservation obs = observations.stream()
                        .filter(o -> o.stationId().equals(station.id()))
                        .findFirst().orElse(null);

                userPrompt.append("- ").append(station.name()).append(": ");
                if (obs != null) {
                    List<String> parts = new ArrayList<>();
                    if (obs.tempC() != null)
                        parts.add("Temp " + String.format("%.1f°C", obs.tempC()));
                    if (obs.windMs() != null)
                        parts.add("Wind " + String.format("%.1f m/s", obs.windMs()));
                    if (obs.gustMs() != null)
                        parts.add("Gusts " + String.format("%.1f m/s", obs.gustMs()));
                    userPrompt.append(parts.isEmpty() ? "No data" : String.join(", ", parts));
                } else {
                    userPrompt.append("No recent data");
                }
                userPrompt.append("\n");
            }

            userPrompt.append("\nProvide ").append(stations.size())
                    .append(" concise advice points (one per station), 15-20 words each.");

            return aiService.ask(systemPrompt, userPrompt.toString(), stations.size());
        } catch (Exception e) {
            return List.of("AI advice unavailable - please review weather data manually");
        }
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", true, "message", message));
    }

    // Request/Response classes
    public static class RouteRequest {
        public String origin;
        public String destination;
        public Double corridorWidthKm;
        public String forecastTime;
    }

    private static class RouteResult {
        List<List<Double>> coordinates = new ArrayList<>();
        double distanceKm;
        int durationMinutes;
    }
}
