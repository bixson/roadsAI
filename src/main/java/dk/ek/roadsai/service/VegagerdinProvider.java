package dk.ek.roadsai.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.ek.roadsai.dto.vegagerdin.VegagerdinArrayDto;
import dk.ek.roadsai.dto.vegagerdin.VegagerdinItemDto;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Vegagerðin road weather station data provider
 * 90 sek caching to reduce load on API
 */
@Service
public class VegagerdinProvider implements StationProvider {

    private static final String BASE = "https://gagnaveita.vegagerdin.is";
    private final WebClient http = WebClient.builder().baseUrl(BASE).build();
    private final XmlMapper xml = new XmlMapper();

    //caching
    private static final Duration TTL = Duration.ofSeconds(90);
    private Instant lastFetchAt = Instant.EPOCH; // Tracks when cache was last populated
    private String lastXml = null; // Cached bulk XML response

    // Fetches XML data with simple TTL-based caching
    private String getXml() {
        // if lastXml is present and not older than TTL, return it
        if (lastXml != null && Duration.between(lastFetchAt, Instant.now()).compareTo(TTL) < 0) {
            return lastXml;
        }
        // else fetch fresh XML + update cache
        String xmlStr = http.get().uri("/api/vedur2014_1")
                .retrieve().bodyToMono(String.class).block();
        // Update cache with fresh data
        lastXml = xmlStr;
        lastFetchAt = Instant.now();
        return xmlStr;
    }
    // format used in Vegagerdin XML timestamps
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm:ss");
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

        // 1) Fetch XML (bulk)
        final String xmlStr = getXml();
        if (xmlStr == null || xmlStr.isBlank()) {
            return List.of();
        }

        // 2) Parse XML into DTOs
        final VegagerdinArrayDto root;
        try {
            root = xml.readValue(xmlStr, VegagerdinArrayDto.class);
        } catch (Exception e) {
            return List.of(); // XML parsing failed
        }
        if (root == null || root.vedur == null || root.vedur.isEmpty()) {
            return List.of(); // No data in XML
        }

        // 3) Filter for requested station
        // Accept only observations from last 1 hour (winter weather changes quickly)
        final ZoneId zone = ZoneId.of("Atlantic/Reykjavik");
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        return root.vedur.stream()
                .filter(v -> v != null && v.nr != null && v.nr.equals(nrWanted)) // Match station number
                .map(v -> toObs(stationId, v, zone)) // Convert XML DTO to model
                .filter(Objects::nonNull)// Skip malformed observations
                .filter(o -> o.timestamp().isAfter(oneHourAgo)) // Accept only recent data (last 1 hour)
                .collect(Collectors.toList());
    }

    // Converts a VegagerdinItemDto to StationObservation
    private StationObservation toObs(String stationId, VegagerdinItemDto v, ZoneId zone) {
        try {
            var local = LocalDateTime.parse(v.dags, FMT);
            var ts = local.atZone(zone).toInstant();
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