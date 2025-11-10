
package dk.ek.roadsai.util;

import java.util.List;
import java.util.Objects;

/**
 * Geodesic distance calculations for route analysis and station filtering
 * Uses Haversine formula for great-circle distances and equirectangular projection for segment operations.
 */
public final class GeoDistance {

    private GeoDistance() {
    }

    public static final double EARTH_RADIUS_M = 6_371_008.8; // earth radius in meters. '_' = thousands separator

    // precise circle distance between two points in meters using Haversine formula
    public static double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double φ1 = Math.toRadians(lat1); // φ == (phi) == latitude
        double φ2 = Math.toRadians(lat2);
        double dφ = Math.toRadians(lat2 - lat1);
        double dλ = Math.toRadians(lon2 - lon1); // λ == (lambda) == longitude

        // Haversine formula: handles antipodal points correctly
        double a = Math.sin(dφ / 2) * Math.sin(dφ / 2)
                + Math.cos(φ1) * Math.cos(φ2) * Math.sin(dλ / 2) * Math.sin(dλ / 2);
        double c = 2 * Math.asin(Math.min(1.0, Math.sqrt(a))); // min() guards against floating-point errors
        return EARTH_RADIUS_M * c;
    }

    // Total length (m) of a polyline (List<[lon,lat]>)
    // uses Haversine between each pair of points to sum up total length
    public static double polylineLengthM(List<List<Double>> lineLonLat) {
        if (lineLonLat == null || lineLonLat.size() < 2) {
            return 0.0;
        }
        double m = 0.0;
        // Sum great-circle distances between consecutive waypoints
        for (int i = 0; i < lineLonLat.size() - 1; i++) {
            var a = lineLonLat.get(i);
            var b = lineLonLat.get(i + 1);
            m += haversineM(a.get(1), a.get(0), b.get(1), b.get(0)); // [lon, lat] → lat, lon order
        }
        return m;
    }

    // Minimum distance from a point to a polyline (meters)
    public static double pointToPolylineM(double lat, double lon, List<List<Double>> lineLonLat) {
        if (lineLonLat == null || lineLonLat.size() < 2) {
            return Double.POSITIVE_INFINITY;  // Invalid polyline → exclude station
        }
        double min = Double.POSITIVE_INFINITY;
        // Check distance to each route segment and keep minimum
        for (int i = 0; i < lineLonLat.size() - 1; i++) {
            var a = lineLonLat.get(i);
            var b = lineLonLat.get(i + 1);
            min = Math.min(min, pointToSegmentM(lat, lon, a, b));
        }
        return min;
    }

    // helper to order stations along a route (from start→end in meters)
    public static double progressAlongPolylineM(double lat, double lon, List<List<Double>> lineLonLat) {
        if (lineLonLat == null || lineLonLat.size() < 2) {
            return 0.0;
        }

        double progressBefore = 0.0;
        double bestDistance = Double.POSITIVE_INFINITY;
        double bestProgressOnSegment = 0.0;

        // Find closest segment and calculate total progress from start
        for (int i = 0; i < lineLonLat.size() - 1; i++) {
            var a = lineLonLat.get(i);
            var b = lineLonLat.get(i + 1);

            // Project point onto current segment
            var proj = projectPointOnSegment(lat, lon, a, b);
            double dToSeg = proj.distanceM;
            double segLenM = haversineM(a.get(1), a.get(0), b.get(1), b.get(0));

            // Update if this segment is closer than previous best
            if (dToSeg < bestDistance) {
                bestDistance = dToSeg;
                bestProgressOnSegment = segLenM * proj.tClamped; // tClamped ∈ [0,1] → meters along segment
                progressBefore = lengthUntilIndexM(lineLonLat, i); // Length from start to segment start
            }
        }
        return progressBefore + bestProgressOnSegment;
    }

    // distance from point to segment [a,b] (meters). a and b == [lon,lat]
    // returns shortest distance from point to segment in m
    public static double pointToSegmentM(double lat, double lon, List<Double> a, List<Double> b) {
        return projectPointOnSegment(lat, lon, a, b).distanceM;
    }

    // internal helper class to hold projection results
    private static final class Projection {
        final double distanceM; // shortest distance from point to segment
        final double tClamped;  // Parametric position along segment: 0 = at point a, 1 = at point b

        Projection(double distanceM, double tClamped) {
            this.distanceM = distanceM;
            this.tClamped = tClamped;
        }
    }

    // returns distance (m) and tClamped (0..1 along segment) of projection
    private static Projection projectPointOnSegment(double lat, double lon, List<Double> a, List<Double> b) {
        Objects.requireNonNull(a, "segment point a");
        Objects.requireNonNull(b, "segment point b");
        double lat1 = a.get(1), lon1 = a.get(0);
        double lat2 = b.get(1), lon2 = b.get(0);

        // Use average latitude for x scaling (reduces distortion)
        double lat0 = (lat1 + lat2) / 2.0;
        double cosLat0 = Math.cos(Math.toRadians(lat0));

        // Equirectangular projection to a local plane
        // X coordinates scaled by cos(latitude) to maintain aspect ratio
        double x1 = Math.toRadians(lon1) * cosLat0;
        double y1 = Math.toRadians(lat1);
        double x2 = Math.toRadians(lon2) * cosLat0;
        double y2 = Math.toRadians(lat2);
        double xp = Math.toRadians(lon) * cosLat0;
        double yp = Math.toRadians(lat);

        // Calculate projection using vector dot product
        double dx = x2 - x1;
        double dy = y2 - y1;
        double segLen2 = dx * dx + dy * dy;

        // Parametric position t: project point onto infinite line through segment
        double t = segLen2 == 0 ? 0 : ((xp - x1) * dx + (yp - y1) * dy) / segLen2;
        double tClamped = Math.max(0.0, Math.min(1.0, t)); // Clamp to [0,1] to stay within segment

        // Calculate projected point coordinates
        double xProj = x1 + tClamped * dx;
        double yProj = y1 + tClamped * dy;

        // Convert projected point back to geographic coordinates
        double projLat = Math.toDegrees(yProj);
        double projLon = Math.toDegrees(xProj / cosLat0);

        // Use Haversine for final distance calculation (accurate great-circle distance)
        double distanceM = haversineM(lat, lon, projLat, projLon);
        return new Projection(distanceM, tClamped);
    }

    // length of polyline up to (but excluding) vertex index @endExclusive
    // Used by progressAlongPolylineM to determine distance from route start
    private static double lengthUntilIndexM(List<List<Double>> lineLonLat, int endExclusive) {
        double m = 0.0;
        // Sum segment lengths from start to endExclusive
        for (int i = 0; i < Math.min(endExclusive, Math.max(0, lineLonLat.size() - 1)); i++) {
            var a = lineLonLat.get(i);
            var b = lineLonLat.get(i + 1);
            m += haversineM(a.get(1), a.get(0), b.get(1), b.get(0));
        }
        return m;
    }
}
