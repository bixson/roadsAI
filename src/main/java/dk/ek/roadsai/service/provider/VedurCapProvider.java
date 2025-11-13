package dk.ek.roadsai.service.provider;

import dk.ek.roadsai.dto.vedur.is.CapAlert;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Veður.is CAP (Common Alerting Protocol) alerts provider
// 30 min caching to reduce load on API
@Service
public class VedurCapProvider {
    private final WebClient http = WebClient.builder()
            .baseUrl("https://api.vedur.is")
            .defaultHeader("User-Agent", "roadsai/1.0")
            .build();

    //caching
    private static final Duration TTL = Duration.ofMinutes(30);
    private final Map<String, List<CapAlert>> cache = new HashMap<>();
    private final Map<String, Instant> cacheTime = new HashMap<>();

    /**
     * Fetch CAP alerts for a station location
     *
     * @param latitude  Station latitude
     * @param longitude Station longitude
     * @return List of active CAP alerts
     */
    public List<CapAlert> fetchAlerts(double latitude, double longitude) {
        String cacheKey = latitude + "," + longitude;

        // Check cache
        if (cache.containsKey(cacheKey) &&
                cacheTime.containsKey(cacheKey) &&
                Duration.between(cacheTime.get(cacheKey), Instant.now()).compareTo(TTL) < 0) {
            return cache.get(cacheKey);
        }

        try {
            // Fetch CAP alerts (30km radius)
            List<CapAlert> alerts = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cap/v1/lat/{lat}/long/{lon}/srid/4326/distance/30/")
                            .build(latitude, longitude))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CapAlert>>() {
                    })
                    .block();

            if (alerts == null) {
                alerts = List.of();
            }

            // Update cache
            cache.put(cacheKey, alerts);
            cacheTime.put(cacheKey, Instant.now());

            return alerts;
        } catch (Exception e) {
            return List.of();
        }
    }
}

