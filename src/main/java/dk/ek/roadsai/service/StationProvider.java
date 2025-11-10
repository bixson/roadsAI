package dk.ek.roadsai.service;

import dk.ek.roadsai.model.Station;
import dk.ek.roadsai.model.StationObservation;

import java.time.Instant;
import java.util.List;

/**
 * interface for station data providers
 */
public interface StationProvider {
    List<Station> listStations();

    List<StationObservation> fetchObservations(String stationId, Instant from, Instant to);
}
