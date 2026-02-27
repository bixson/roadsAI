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

    /** Ensures the bulk JSON cache is fresh, fetching from the API if stale. */
    private void ensureFresh() {
        if (lastJson == null || Duration.between(lastFetchAt, Instant.now()).compareTo(TTL) >= 0) {
            try {
                lastJson = http.get().uri("/api/vedur2014_1")
                        .retrieve().bodyToMono(String.class).block();
                lastFetchAt = Instant.now();
            } catch (Exception e) { /* keep stale on error */ }
        }
    }

    /** Returns all Iceland road weather stations discovered dynamically from the bulk API. */
    @Override
    public List<Station> listStations() {
        ensureFresh();
        if (lastJson == null || lastJson.isBlank()) return List.of();
        try {
            List<VegagerdinItemDto> all = json.readValue(lastJson, new TypeReference<List<VegagerdinItemDto>>() {});
            Map<Integer, Station> seen = new LinkedHashMap<>();
            for (VegagerdinItemDto v : all) {
                if (v == null || v.nrVedurstofa == null || v.breidd == null || v.lengd == null) continue;
                seen.computeIfAbsent(v.nrVedurstofa, nr ->
                        new Station("veg:" + nr, v.nafn != null ? v.nafn : "veg:" + nr,
                                v.breidd, v.lengd, "VEGAGERDIN"));
            }
            return List.copyOf(seen.values());
        } catch (Exception e) {
            return List.of();
        }
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

        // 1) Fetch JSON array (bulk) using shared cache
        ensureFresh();
        if (lastJson == null || lastJson.isBlank()) {
            return List.of();
        }
        String jsonStr = lastJson;

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
                .toList();
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