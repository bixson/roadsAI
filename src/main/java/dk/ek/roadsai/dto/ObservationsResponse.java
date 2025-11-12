package dk.ek.roadsai.dto;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;

import java.util.List;
import java.util.Map;

public record ObservationsResponse(
        List<StationObservation> observations,
        Map<String, List<CapAlert>> alerts,
        List<Station> stations,
        List<List<Double>> route
) {
}

