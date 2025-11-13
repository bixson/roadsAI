package dk.ek.roadsai.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ek.roadsai.dto.yr.YrNoForecastDto;
import dk.ek.roadsai.model.ForecastPoint;
import dk.ek.roadsai.model.Station;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// YR.no weather forecast provider with 1 hour caching
@Service
public class YrNoProvider {
    private final WebClient http = WebClient.builder()
            .baseUrl("https://api.met.no")
            .defaultHeader("User-Agent", "roadsai/1.0")
            .build();

    private static final Duration TTL = Duration.ofHours(1);
    private final Map<String, YrNoForecastDto> cache = new HashMap<>();
    private final Map<String, Instant> cacheTime = new HashMap<>();
    private final ObjectMapper json = new ObjectMapper();

    public List<ForecastPoint> fetchForecastForStations(List<Station> stations) {
        List<ForecastPoint> forecasts = new ArrayList<>();
        Instant now = Instant.now();

        for (Station station : stations) {
            double lat = station.latitude();
            double lon = station.longitude();
            String cacheKey = lat + "," + lon;

            YrNoForecastDto dto = fetchForecast(lat, lon, cacheKey);
            if (dto != null && dto.properties != null && dto.properties.timeseries != null) {
                for (YrNoForecastDto.TimeStep step : dto.properties.timeseries) {
                    if (step.data != null && step.data.instant != null && step.data.instant.details != null) {
                        Instant time = ZonedDateTime.parse(step.time).toInstant();
                        // Only include future forecasts (yr.no API provides ~10 days)
                        if (time.isBefore(now)) continue;
                        
                        var details = step.data.instant.details;
                        Double precip = step.data.next1Hours != null && step.data.next1Hours.details != null 
                            ? step.data.next1Hours.details.precipitationAmount : null;
                        forecasts.add(new ForecastPoint(
                                time,
                                lat,
                                lon,
                                details.airTemperature,
                                details.windSpeed,
                                precip
                        ));
                    }
                }
            }
        }
        return forecasts;
    }

    private YrNoForecastDto fetchForecast(double lat, double lon, String cacheKey) {
        if (cache.containsKey(cacheKey) && cacheTime.containsKey(cacheKey) &&
                Duration.between(cacheTime.get(cacheKey), Instant.now()).compareTo(TTL) < 0) {
            return cache.get(cacheKey);
        }

        try {
            String response = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weatherapi/locationforecast/2.0/compact")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                return null;
            }

            YrNoForecastDto dto = json.readValue(response, YrNoForecastDto.class);
            cache.put(cacheKey, dto);
            cacheTime.put(cacheKey, Instant.now());
            return dto;
        } catch (Exception e) {
            return null;
        }
    }
}

