package dk.ek.roadsai.service;

import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
public class RouteService {

    /// ⚠️ Replace these demo coords with your full RVK–ÍSAF polyline (WGS84 lon,lat).
    // Keep it lean for now; denser points = smoother line & better proximity math.
    private static final double[][] RVK_IFJ_LINE = new double[][]{ // nested for {lon, lat}
            {64,1289, 21,9082}, // Reykjavík, Faxaflói
            {64,4755, 21,9603}, // Hafnarfjall, Hvalfjörður
            {64.5609, 21.9105}, // Borgarnes
            {64,8716, 21,5154}, // Brattabrekka
            {65,5524, 21,833},  // Þröskuldar
            {65,6873, 21,6813}, //Hólmavík
            {65,7503, 22,1291}, // Steingrímsfjarðarheiði
            {66.0449, 22.6817}, // Ögur
            {66.0596, 23.1699}  // Arnarfjörður, Ísafjörður
    };

    public Map<String, Object> getRvkIsfGeoJson() {
        return Map.of(
                "type", "Feature",
                "properties", Map.of("id", "rvk-isf", "name", "Reykjavík ↔ Ísafjörður"),
                "geometry", Map.of(
                        "type", "LineString",
                        "coordinates", toListOfPairs(RVK_IFJ_LINE)
                )
        );
    }

    @SuppressWarnings("unchecked")
    public List<List<Double>> getRvkIsfCoordinates() {
        return (List<List<Double>>) getRvkIsfGeoJson().get("geometry") instanceof Map geom
                ? (List<List<Double>>) ((Map<?, ?>) geom).get("coordinates")
                : List.of();
    }

    private static List<List<Double>> toListOfPairs(double[][] coords) {
        return java.util.Arrays.stream(coords)
                .map(p -> List.of(p[0], p[1])) // [lon, lat]
                .toList();
    }
}
