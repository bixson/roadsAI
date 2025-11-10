#!/bin/bash
# thanks to chatgpt for creating this wonderful script for simple backend testing
echo "=== Testing Route Endpoint ==="
curl -s http://localhost:8080/api/route/rvk-isf | jq -r '.properties.name'

echo -e "\n=== Testing Advice Endpoint ==="
RESPONSE=$(curl -s -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d "{
    \"from\": \"RVK\",
    \"to\": \"IFJ\",
    \"mode\": \"departure\",
    \"timeIso\": \"$(date -u +'%Y-%m-%dT%H:%M:%SZ')\"
  }")

echo "Advice points:"
echo "$RESPONSE" | jq -r '.advice[]'

echo -e "\nHazards detected:"
echo "$RESPONSE" | jq -r '.summaryStats.hazards[]'

echo -e "\nStations used: $(echo "$RESPONSE" | jq -r '.summaryStats.stationsUsed')"
echo "Stations in map: $(echo "$RESPONSE" | jq -r '.mapData.stations | length')"

echo -e "\n✅ Backend test complete!"
