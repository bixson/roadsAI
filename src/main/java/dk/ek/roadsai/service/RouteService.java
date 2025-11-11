package dk.ek.roadsai.service;

import dk.ek.roadsai.util.GeoDistance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * hardcoded route Reykjavík ↔ Ísafjörður
 */

@Service
public class RouteService {

    private static final List<List<Double>> RVK_ISF = List.of(
            List.of(-21.8046, 64.1238), // Reykjavík, Orkan Vesturlandsvegi
            List.of(-21.9603, 64.4755), // Hafnarfjall, nálægt vindskilti
            List.of(-21.9101, 64.5439), // Borgarnes, N1 bensínstöð
            List.of(-21.5154, 64.8716), // Brattabrekka, Dalabyggð
            List.of(-21.7632, 65.1082), // Búðardalur (gamla Kaupfélagið)
            List.of(-21.8330, 65.5524), // Þröskuldar
            List.of(-21.6951, 65.7015), // Hólmavík, við afleggjara/flugvöll
            List.of(-22.1291, 65.7503), // Steingrímsfjarðarheiði
            List.of(-22.7303, 66.0403), // Ögur
            List.of(-22.9888, 66.0279), // Súðavík
            List.of(-23.0465, 66.0977),  // Arnarfjörður, Ísafjörður
            List.of(-23.1239, 66.0746)   // Ísafjörður, N1 bensínstöð
    );

    // returns raw coordinate list
    public List<List<Double>> getCoordinates() {
        return RVK_ISF;
    }

    // returns coordinate list, reversed for route switch (ifj->rvk)
    public List<List<Double>> getCoordinates(String from, String to) {
        if ("IFJ".equals(from) && "RVK".equals(to)) {
            // Reverse route for IFJ → RVK
            List<List<Double>> reversed = new ArrayList<>(RVK_ISF);
            Collections.reverse(reversed);
            return reversed;
        }
        // Default: RVK → IFJ
        return RVK_ISF;
    }

    // Generates GeoJSON Feature for Leaflet map visualization (frontend)
    public Map<String, Object> getGeoJson() {
        return Map.of(
                "type", "Feature",
                "properties", Map.of(
                        "id", "rvk-isf",
                        "name", "Reykjavík ↔ Ísafjörður",
                        "length_m", getLengthMeters() // Total route distance in meters
                ),
                "geometry", Map.of(
                        "type", "LineString",
                        "coordinates", RVK_ISF // Ordered waypoint coordinates
                )
        );
    }

    // Polyline length in meters
    public double getLengthMeters() {
        return GeoDistance.polylineLengthM(RVK_ISF);
    }
}
