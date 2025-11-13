#!/bin/bash

echo "=== Test 1: Current observations + all forecasts (48h) ==="
curl -s -X POST http://localhost:8080/api/observations \
  -H "Content-Type: application/json" \
  -d '{"from":"RVK","to":"IFJ"}' \
  | jq '{
    observations_count: (.observations | length),
    stations_count: (.stations | length),
    forecasts_count: (.forecasts | length),
    route_points: (.route | length)
  }'

echo -e "\n=== Test 2: Current observations only ==="
curl -s -X POST http://localhost:8080/api/observations \
  -H "Content-Type: application/json" \
  -d '{"from":"RVK","to":"IFJ"}' \
  | jq '{
    observations: .observations | map({stationId, tempC, windMs, timestamp}),
    stations: .stations | map({id, name}),
    alerts: .alerts,
    advice_count: (.advice | length)
  }'

echo -e "\n=== Test 3: Forecasts for next 10 hours (first station) ==="
curl -s -X POST http://localhost:8080/api/observations \
  -H "Content-Type: application/json" \
  -d '{"from":"RVK","to":"IFJ"}' \
  | jq '
    . as $data |
    $data.stations[0] as $firstStation |
    (now | todateiso8601) as $now |
    ((now + 10*3600) | todateiso8601) as $future |
    {
      station: $firstStation,
      current_obs: [$data.observations[] | select(.stationId == $firstStation.id)],
      forecasts_10h: [$data.forecasts[] | 
        select(.latitude == $firstStation.latitude and 
               .longitude == $firstStation.longitude and 
               .time >= $now and 
               .time <= $future)] | 
        map({time, tempC, windMs, precipMm})
    }
  '

echo -e "\n=== Test 4: All forecasts grouped by station ==="
curl -s -X POST http://localhost:8080/api/observations \
  -H "Content-Type: application/json" \
  -d '{"from":"RVK","to":"IFJ"}' \
  | jq '.forecasts | group_by(.latitude + "," + .longitude) | 
    map({
      location: .[0] | {lat: .latitude, lon: .longitude},
      forecast_count: length,
      time_range: {from: (.[0].time), to: (.[-1].time)}
    })'

