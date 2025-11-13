package dk.ek.roadsai.dto;

import dk.ek.roadsai.dto.vedur.is.CapAlert;
import dk.ek.roadsai.model.ForecastPoint;
import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;

import java.util.List;
import java.util.Map;

/// Response DTO for observations API
 // From backend to frontend
public record ObservationsResponse(
        List<StationObservation> observations,
        Map<String, List<CapAlert>> alerts,
        List<Station> stations,
        List<List<Double>> route,
        List<String> advice,
        List<ForecastPoint> forecasts
) {
}

