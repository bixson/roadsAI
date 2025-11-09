package dk.ek.roadsai.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.ek.roadsai.dto.vegagerdin.VegagerdinArrayDto;
import dk.ek.roadsai.dto.vegagerdin.VegagerdinItemDto;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class VegagerdinProvider implements StationProvider {

    private static final String BASE = "https://gagnaveita.vegagerdin.is";
    private final WebClient http = WebClient.builder().baseUrl(BASE).build();
    private final XmlMapper xml = new XmlMapper();
    //caching
    private static final Duration TTL = Duration.ofSeconds(90);
    private Instant lastFetchAt = Instant.EPOCH;
    private String lastXml = null;

    private String getXml() {
        // simple TTL-based cache: if lastXml is present and not older than TTL, return it
        if (lastXml != null && Duration.between(lastFetchAt, Instant.now()).compareTo(TTL) < 0) {
            return lastXml;
        }
        // else fetch fresh XML + update cache
        String xmlStr = http.get().uri("/api/vedur2014_1")
                .retrieve().bodyToMono(String.class).block();
        lastXml = xmlStr;
        lastFetchAt = Instant.now();
        return xmlStr;
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm:ss");

    private final List<Station> registry = List.of(

            // TODO: CHECK IF 'veg:' SHOULD BE PART OF id OR JUST number?
            new Station("veg:31674", "HFNFJ (Hafnarfjall)", 64.4755, -21.9603, "VEGAGERDIN"),
            new Station("veg:31985", "BRATT (Brattabrekka)", 64.8716, -21.5155, "VEGAGERDIN"),
            new Station("veg:32377", "THROS (Þröskuldar)", 65.5524, -21.833, "VEGAGERDIN"),
            new Station("veg:32474", "STEHE (Steingrímsfjarðarheiði)", -65.7503, 22.1291, "VEGAGERDIN"),
            new Station("veg:32654", "OGURI (Ögur)", 66.0449, -22.6817, "VEGAGERDIN")
    );

    @Override
    public List<Station> listStations() {
        return registry;
    }

    @Override
    public List<StationObservation> fetchObservations(String stationId, Instant from, Instant to) {
        // Parse station number
        final String nrStr = stationId.startsWith("veg:") ? stationId.substring(4) : stationId;
        final int nrWanted;
        try {
            nrWanted = Integer.parseInt(nrStr);
        } catch (Exception e) {
            return List.of(); // empty
        }

        // 1) Fetch XML (bulk)
        final String xmlStr = getXml();
        if (xmlStr == null || xmlStr.isBlank()) {
            return List.of();
        }

        // 2) Parse XML
        final VegagerdinArrayDto root;
        try {
            root = xml.readValue(xmlStr, VegagerdinArrayDto.class);
        } catch (Exception e) {
            return List.of();
        }
        if (root == null || root.vedur == null || root.vedur.isEmpty()) {
            return List.of();
        }

        // 3) Filter for this station + time window
        final ZoneId zone = ZoneId.of("Atlantic/Reykjavik");
        return root.vedur.stream()
                .filter(v -> v != null && v.nr != null && v.nr.equals(nrWanted))
                .map(v -> toObs(stationId, v, zone))
                .filter(Objects::nonNull)
                .filter(o -> !o.timestamp().isBefore(from) && !o.timestamp().isAfter(to))
                .collect(Collectors.toList());
    }

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