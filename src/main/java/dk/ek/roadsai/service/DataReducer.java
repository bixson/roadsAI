package dk.ek.roadsai.service;

import dk.ek.roadsai.model.StationObservation;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

///  reduces raw data from stations (API) for advice generation
/// removes unnecessary data + computes summary stats per segment
/// - we only send worst/best data to advice engine
@Service
public class DataReducer {

    public static class SegmentFacts {
        public String name;
        public Double maxGustMs;
        public Double windMs;
        public Double minTempC;
        public Double minVisM;
        public String precipType; // dominant precip type
    }

    // calculates max gust, max wind, min temp, min vis, dominant precip type per segment
    public Map<String, SegmentFacts> reduceToSegments(List<StationObservation> obs) {
        Map<String, List<StationObservation>> byStation = obs.stream()
                .collect(Collectors.groupingBy(StationObservation::stationId));

        Map<String, SegmentFacts> out = new LinkedHashMap<>();
        for (var entry : byStation.entrySet()) {
            var facts = new SegmentFacts();
            facts.name = entry.getKey();
            facts.maxGustMs = entry.getValue().stream().map(StationObservation::gustMs).filter(Objects::nonNull).max(Double::compare).orElse(null);
            facts.windMs = entry.getValue().stream().map(StationObservation::windMs).filter(Objects::nonNull).max(Double::compare).orElse(null);
            facts.minTempC = entry.getValue().stream().map(StationObservation::tempC).filter(Objects::nonNull).min(Double::compare).orElse(null);
            facts.minVisM = entry.getValue().stream().map(StationObservation::visibilityM).filter(Objects::nonNull).min(Double::compare).orElse(null);
            facts.precipType = dominant(entry.getValue().stream().map(StationObservation::precipType).toList());
            out.put(entry.getKey(), facts);
        }
        return out;
    }

    private String dominant(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
