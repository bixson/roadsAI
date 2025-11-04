package dk.ek.roadsai.dto.vedur.is;

import java.time.Instant;
import java.util.List;

public class VedurAwsDto {
    public List<Item> results;

    public static class Item {
        public String station;     // station id string
        public Instant time;       // ISO timestamp
        public Double t;           // temp °C
        public Double ff;          // wind m/s
        public Double fx;          // gust m/s
        public Double vis;         // visibility m
        public String precip;      // precip type
    }

    public List<dk.ek.roadsai.model.StationObservation> toStationObs(String stationId) {
        if (results == null) return List.of();
        return results.stream().map(it -> new dk.ek.roadsai.model.StationObservation(
                stationId,
                it.time,
                it.t,
                it.ff,
                it.fx,
                it.vis,
                it.precip,
                null
        )).toList();
    }
}
