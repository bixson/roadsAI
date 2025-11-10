package dk.ek.roadsai.service;

import dk.ek.roadsai.util.GeoDistance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RouteService {

    // TODO: Replace with final polyline when ready.
    private static final List<List<Double>> RVK_ISF = List.of(
        List.of(-21.9082, 64.1289), // Reykjavík, Faxaflói
        List.of(-21.9603, 64.4755), // Hafnarfjall (Vegagerðin)
        List.of(-21.9105, 64.5609), // Borgarnes (approx)
        List.of(-21.5154, 64.8716), // Brattabrekka (Vegagerðin)
        List.of(-21.8330, 65.5524), // Þröskuldar (Vegagerðin)
        List.of(-21.6813, 65.6873), // Hólmavík (approx)
        List.of(-22.1291, 65.7503), // Steingrímsfjarðarheiði (Vegagerðin)
        List.of(-22.6817, 66.0449), // Ögur (Vegagerðin)
        List.of(-23.1699, 66.0596)  // Arnarfjörður / Ísafjörður (approx)
    );

    public List<List<Double>> getCoordinates() {
        return RVK_ISF;
    }

    // GeoJSON Feature for Leaflet.
    public Map<String, Object> getGeoJson() {
        return Map.of(
            "type", "Feature",
            "properties", Map.of(
                "id", "rvk-isf",
                "name", "Reykjavík ↔ Ísafjörður",
                "length_m", getLengthMeters()
            ),
            "geometry", Map.of(
                "type", "LineString",
                "coordinates", RVK_ISF
            )
        );
    }

    // Polyline length in meters
    public double getLengthMeters() {
        return GeoDistance.polylineLengthM(RVK_ISF);
    }
}
