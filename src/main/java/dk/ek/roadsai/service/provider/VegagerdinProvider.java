package dk.ek.roadsai.service.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ek.roadsai.dto.vegagerdin.VegagerdinItemDto;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/// Vegagerðin road weather station data provider
// 15 min caching to reduce load on API
@Service
public class VegagerdinProvider implements StationProvider {

    private static final String BASE = "https://gagnaveita.vegagerdin.is";
    private final WebClient http = WebClient.builder().baseUrl(BASE).build();
    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    //caching (15 min TTL)
    private static final Duration TTL = Duration.ofMinutes(15);
    private Instant lastFetchAt = Instant.EPOCH;
    private String lastJson = null;

    // Vegagerdin JSON timestamps ("4.11.2025 21:50:00")
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm:ss");
    private static final ZoneId Z_REYK = ZoneId.of("Atlantic/Reykjavik");
    //fixed for RVK↔IFJ
    private final List<Station> registry = List.of(
            new Station("veg:31674", "HFNFJ (Hafnarfjall)", 64.4755, -21.9603, "VEGAGERDIN"),
            new Station("veg:31985", "BRATT (Brattabrekka)", 64.8716, -21.5155, "VEGAGERDIN"),
            new Station("veg:32377", "THROS (Þröskuldar)", 65.5524, -21.833, "VEGAGERDIN"),
            new Station("veg:32474", "STEHE (Steingrímsfjarðarheiði)", 65.7503, -22.1291, "VEGAGERDIN"),
            new Station("veg:32654", "OGURI (Ögur)", 66.0449, -22.6817, "VEGAGERDIN")
    );

    @Override
    public List<Station> listStations() {
        return registry;
    }

    @Override
    public List<StationObservation> fetchObservations(String stationId, Instant from, Instant to) {
        // Strip "veg:" prefix + parse numeric station ID
        final String nrStr = stationId.startsWith("veg:") ? stationId.substring(4) : stationId;
        final int nrWanted;
        try {
            nrWanted = Integer.parseInt(nrStr);
        } catch (Exception e) {
            return List.of(); // Invalid station ID format
        }

        // 1) Fetch JSON array (bulk) (cache-check)
        String jsonStr = null;
        if (lastJson != null && Duration.between(lastFetchAt, Instant.now()).compareTo(TTL) < 0) {
            jsonStr = lastJson;
        } else {
            // Fetch fresh JSON + update cache
            jsonStr = http.get().uri("/api/vedur2014_1")
                    .retrieve().bodyToMono(String.class).block();
            lastJson = jsonStr;
            lastFetchAt = Instant.now();
        }
        
        if (jsonStr == null || jsonStr.isBlank()) {
            return List.of();
        }

        // 2) Parse JSON array directly into DTOs
        List<VegagerdinItemDto> vedur;
        try {
            vedur = json.readValue(jsonStr, new TypeReference<List<VegagerdinItemDto>>() {});
        } catch (Exception e) {
            return List.of(); // JSON parsing failed
        }

        // 3) Filter for requested station and time window
        return vedur.stream() // stream all vegagerdin observations
                .filter(v -> v != null && v.nrVedurstofa != null && v.nrVedurstofa.equals(nrWanted)) // filter by station ID
                .map(v -> toObs(stationId, v, Z_REYK)) // convert DTO to model
                .filter(Objects::nonNull) // skip malformed observations
                .filter(o -> !o.timestamp().isBefore(from) && !o.timestamp().isAfter(to)) // filter by requested time window
                .collect(Collectors.toList());
    }

    // Converts a VegagerdinItemDto to StationObservation
    private StationObservation toObs(String stationId, VegagerdinItemDto v, ZoneId zone) {
        try {
            var local = LocalDateTime.parse(v.dags, FMT);
            var ts = local.atZone(zone).toInstant(); // Parse local Iceland time → convert to UTC Instant
            Double wind = v.vindhradi;
            Double gust = v.vindhvida;
            return new StationObservation(
                    stationId,
                    ts,
                    v.hiti,
                    wind,
                    gust,
                    null,  // visibility not provided in this feed
                    null   // precip not provided
            );
        } catch (Exception e) {
            return null;
        }
    }
}