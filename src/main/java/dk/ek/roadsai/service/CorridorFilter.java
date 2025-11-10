package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.util.GeoDistance;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


// Filters and orders stations in correct sequence with regard to route polyline(hardcoded JSON), else we would have clusterfuck in the stations-order.
// - Distance check uses GeoDistance.pointToPolylineM(...)
// - Ordering uses GeoDistance.progressAlongPolylineM(...)
@Service
public class CorridorFilter {

    /** Returns stations within a buffer (meters) of the route, ordered from start→end of the route.
     * @param stations     input station list
     * @param routeLonLat  route polyline as List of [lon,lat]
     * @param bufferMeters buffer width in meters (e.g., 8000 for fjord corridors)
     */
    public List<Station> filterByBuffer(List<Station> stations,
                                        List<List<Double>> routeLonLat,
                                        double bufferMeters) {
        Objects.requireNonNull(stations, "stations");
        Objects.requireNonNull(routeLonLat, "routeLonLat");
        if (routeLonLat.size() < 2 || stations.isEmpty()) {
            return List.of();
        }

        return stations.stream()
                .map(s -> new StationWithMetrics(
                        s,
                        GeoDistance.pointToPolylineM(s.latitude(), s.longitude(), routeLonLat),
                        GeoDistance.progressAlongPolylineM(s.latitude(), s.longitude(), routeLonLat)
                ))
                .filter(m -> m.distanceM <= bufferMeters)
                .sorted(Comparator.comparingDouble((StationWithMetrics m) -> m.progressM)
                        .thenComparingDouble(m -> m.distanceM))
                .map(m -> m.station)
                .collect(Collectors.toList());
    }

    private static final class StationWithMetrics {
        final Station station;
        final double distanceM; // shortest distance to route
        final double progressM; // position along route

        StationWithMetrics(Station station, double distanceM, double progressM) {
            this.station = station;
            this.distanceM = distanceM;
            this.progressM = progressM;
        }
    }
}
