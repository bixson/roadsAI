package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.util.GeoDistance;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Filters and orders stations along route polyline (corridor).
 * Calculates distance and position relationships between stations and route geometry.
 * *** tager alle stationer på ruten og liner dem op i korrekt rækkefølge fra start til slut.
 */
@Service
public class CorridorFilter {

    // Filters stations within buffer distance (meters) from route polyline
    // Orders results by progress along route, then by distance from route
    public List<Station> filterByBuffer(List<Station> stations,
                                        List<List<Double>> routeLonLat,
                                        double bufferMeters) {
        Objects.requireNonNull(stations, "stations");
        Objects.requireNonNull(routeLonLat, "routeLonLat");
        // min 2 points to form a route (A-B, osv...)
        if (routeLonLat.size() < 2 || stations.isEmpty()) {
            return List.of();
        }

        return stations.stream()
                // Calculate distance and progress metrics for each station
                .map(s -> new StationWithMetrics(
                        s,
                        GeoDistance.pointToPolylineM(s.latitude(), s.longitude(), routeLonLat),
                        GeoDistance.progressAlongPolylineM(s.latitude(), s.longitude(), routeLonLat)
                ))
                .filter(m -> m.distanceM <= bufferMeters) // only stations within buffer
                .sorted(Comparator.comparingDouble((StationWithMetrics m) -> m.progressM) // 1) sort by progress along route
                        .thenComparingDouble(m -> m.distanceM)) // 2) then by distance to route
                .map(m -> m.station) // extract station objects
                .collect(Collectors.toList());
    }

    // Helper class holding station with calculated distance and position metrics
    private static final class StationWithMetrics {
        final Station station;
        final double distanceM; // Shortest distance from station to route
        final double progressM; // position along route to nearest point

        StationWithMetrics(Station station, double distanceM, double progressM) {
            this.station = station;
            this.distanceM = distanceM;
            this.progressM = progressM;
        }
    }
}
