#!/bin/bash

echo "=== Test 1: Current observations only (no forecast) ==="
curl -s -X POST http://localhost:8080/api/observations \
  -H "Content-Type: application/json" \
  -d '{"from":"RVK","to":"IFJ"}' \
  | jq '{
    observations: (.observations | length),
    stations: (.stations | length),
    forecasts: (.forecasts | length),
    advice_count: (.advice | length)
  }'

echo -e "\n=== Test 2: Current + Forecast (10 hours from now) ==="
FORECAST_TIME=$(date -u -v+10H +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d "+10 hours" +%Y-%m-%dT%H:%M:%SZ)
curl -s -X POST http://localhost:8080/api/observations \
  -H "Content-Type: application/json" \
  -d "{\"from\":\"RVK\",\"to\":\"IFJ\",\"forecastTime\":\"$FORECAST_TIME\"}" \
  | jq '{
    observations: (.observations | length),
    stations: (.stations | length),
    forecasts: (.forecasts | length),
    forecast_time_range: {
      first: .forecasts[0].time,
      last: .forecasts[-1].time
    },
    advice_count: (.advice | length)
  }'

echo -e "\n=== Test 3: Current + Forecast (48 hours from now) ==="
FORECAST_TIME=$(date -u -v+48H +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d "+48 hours" +%Y-%m-%dT%H:%M:%SZ)
curl -s -X POST http://localhost:8080/api/observations \
  -H "Content-Type: application/json" \
  -d "{\"from\":\"RVK\",\"to\":\"IFJ\",\"forecastTime\":\"$FORECAST_TIME\"}" \
  | jq '{
    observations: (.observations | length),
    forecasts: (.forecasts | length),
    sample_forecast: .forecasts[0:3] | map({time, tempC, windMs, precipMm})
  }'

