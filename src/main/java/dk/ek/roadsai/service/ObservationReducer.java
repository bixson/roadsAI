package dk.ek.roadsai.service;

import dk.ek.roadsai.dto.vedur.is.CapAlert;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/// Reduces raw station observations for AI advice generation.
// Aggregates worst-case metrics per station segment.
@Service
public class ObservationReducer {

    // Container for station-level weather summary statistics
    public static class StationFacts {
        public String stationName;
        public String stationId;
        public Double maxGustMs;
        public Double windMs;
        public Double minTempC;
        public Double minVisM;
        public String precipType;
        public List<CapAlert> alerts;
    }

    // Calculates worst-case metrics per station from observations
    public Map<String, StationFacts> reduceToStations(
            List<StationObservation> obs,
            List<Station> stations,
            Map<String, List<CapAlert>> stationAlerts) {

        // Group observations by station ID
        Map<String, List<StationObservation>> byStation = obs.stream()
                .collect(Collectors.groupingBy(StationObservation::stationId));

        Map<String, StationFacts> out = new LinkedHashMap<>();

        // Process ALL stations (in order), (not just those with observations)
        for (Station station : stations) {
            var facts = new StationFacts();
            facts.stationId = station.id();
            facts.stationName = station.name();

            List<StationObservation> stationObs = byStation.getOrDefault(station.id(), List.of());

            if (!stationObs.isEmpty()) {
                // Calculate worst conditions from observations
                facts.maxGustMs = stationObs.stream()
                        .map(StationObservation::gustMs)
                        .filter(Objects::nonNull)
                        .max(Double::compare)
                        .orElse(null);
                facts.windMs = stationObs.stream()
                        .map(StationObservation::windMs)
                        .filter(Objects::nonNull)
                        .max(Double::compare)
                        .orElse(null);
                facts.minTempC = stationObs.stream()
                        .map(StationObservation::tempC)
                        .filter(Objects::nonNull)
                        .min(Double::compare)
                        .orElse(null);
                facts.minVisM = stationObs.stream()
                        .map(StationObservation::visibilityM)
                        .filter(Objects::nonNull)
                        .min(Double::compare)
                        .orElse(null);
                facts.precipType = stationObs.stream()
                        .map(StationObservation::precipType)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }

            // Add CAP alerts for this station
            facts.alerts = stationAlerts.getOrDefault(station.id(), List.of());

            out.put(station.id(), facts);
        }
        return out;
    }
}

