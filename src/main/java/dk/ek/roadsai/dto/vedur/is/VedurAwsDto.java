package dk.ek.roadsai.dto.vedur.is;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.ek.roadsai.model.StationObservation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

///  Veðurstofa Íslands returns JSON in API
@JsonIgnoreProperties(ignoreUnknown = true)
public class VedurAwsDto {

    @JsonProperty("results")
    public List<Aws10minBasic> results;

    // 10min basic observation structure from Veðurstofa Íslands(IMO)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aws10minBasic {
        @JsonProperty("station_id")
        public String stationId;
        @JsonProperty("time")
        public String time;   // local ISO without zone (ex: 2025-11-05T21:40:00)
        @JsonProperty("t")
        public Double t;      // temp °C
        @JsonProperty("f")
        public Double f;      // wind m/s (mean)
        @JsonProperty("fg")
        public Double fg;     // gust m/s
        @JsonProperty("vis")
        public Double vis;    // visibility m (nullable)
        @JsonProperty("precip")
        public String precip; // precipitation code/text (nullable)
    }

//    // mapper for wrapped shape
//    public List<StationObservation> toStationObs(String stationId) {
//        if (results == null || results.isEmpty()) return List.of();
//        return map(stationId, results);
//    }

    // Converts bare array shape from IMO API to StationObservation objects (parses local timestamps to UTC)
    public static List<StationObservation> map(String stationId, List<Aws10minBasic> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        final ZoneId Z_REYK = ZoneId.of("Atlantic/Reykjavik");
        final DateTimeFormatter FMT_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        return list.stream()
                .map(it -> {
                    try {
                        LocalDateTime ldt = LocalDateTime.parse(it.time, FMT_LOCAL);
                        Instant ts = ldt.atZone(Z_REYK).toInstant();
                        return new StationObservation(
                                stationId,
                                ts,
                                it.t,     // temp °C
                                it.f,     // wind m/s
                                it.fg,    // gust m/s
                                it.vis,   // visibility m (nullable)
                                it.precip // precip (nullable)
                        );
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
