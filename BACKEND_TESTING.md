# Backend Testing Guide - Full Verification

## Prerequisites

1. **Set OpenAI API Key**:
```bash
export OPENAI_API_KEY=sk-your-key-here
```

2. **Start Spring Boot Application**:
```bash
./mvnw spring-boot:run
```

Wait for: `Started RoadsAiApplication` message

---

## Test 1: Route Endpoint (GET)

**Test**: Verify route GeoJSON is returned correctly

```bash
curl http://localhost:8080/api/route/rvk-isf | jq
```

**Expected**:
- ✅ Returns GeoJSON Feature
- ✅ Has 9 coordinate points
- ✅ Length ~266-450km
- ✅ Properties include id, name, length_m

**Verify**: Route coordinates match your final polyline

---

## Test 2: Debug Endpoints

### 2a. Corridor Filtering

```bash
curl "http://localhost:8080/api/debug/corridor?bufferM=5000" | jq
```

**Expected**:
- ✅ Returns ~8 stations (5 Vegagerðin + 3 IMO)
- ✅ All stations have correct lat/lon
- ✅ Stations are ordered along route
- ✅ Station IDs have `veg:` or `imo:` prefixes

### 2b. Observations Fetching

```bash
curl "http://localhost:8080/api/debug/obs?hours=2" | jq '.obs | length'
```

**Expected**:
- ✅ Returns observations array
- ✅ Each observation has: stationId, timestamp, tempC, windMs, gustMs
- ✅ Some observations may have visibilityM and precipType (from IMO)
- ✅ Check console for any errors

### 2c. IMO Station Direct Test

```bash
curl "http://localhost:8080/api/debug/imo?stationId=1475" | jq
```

**Expected**:
- ✅ Returns observations for Reykjavík station
- ✅ Has temperature, wind, gust data
- ✅ May have visibility and precipitation

---

## Test 3: Main Advice Endpoint - Full Flow

### 3a. Test with Current Time (Departure Mode)

```bash
curl -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d "{
    \"from\": \"RVK\",
    \"to\": \"IFJ\",
    \"mode\": \"departure\",
    \"timeIso\": \"$(date -u +'%Y-%m-%dT%H:%M:%SZ')\"
  }" | jq
```

**Check Response Structure**:
- ✅ `advice`: Array of exactly 4 strings (AI-generated)
- ✅ `summaryStats.stationsUsed`: Number (should be ~8)
- ✅ `summaryStats.window`: Object with `from` and `to` timestamps
- ✅ `summaryStats.hazards`: Array (should include header + detected hazards)
- ✅ `mapData.route`: GeoJSON LineString with coordinates
- ✅ `mapData.stations`: Array of station objects with id, name, lat, lon

**Check Console Output**:
- ✅ `[Advice] mode=departure t=... corridorStations=8 obs=...`
- ✅ No errors or stack traces

**Verify Hazards**:
- ✅ First item: "Official Weather Warnings (Icelandic Road Safety Office):"
- ✅ If wind ≥ 20 m/s: Should see Warning Level 1/2/3
- ✅ If gusts ≥ 26 m/s: Should see gust warnings
- ✅ If visibility < 1000m: Should see low visibility warning
- ✅ If temp ≤ 0°C with precip: Should see ice risk warning

### 3b. Test Arrival Mode

```bash
curl -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d '{
    "from": "RVK",
    "to": "IFJ",
    "mode": "arrival",
    "timeIso": "2025-11-10T18:00:00Z"
  }' | jq '.summaryStats.window'
```

**Expected**:
- ✅ Different time window (backward calculation)
- ✅ Still returns 4 advice points
- ✅ Hazards still detected

### 3c. Test Error Handling

```bash
curl -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d '{
    "from": "RVK",
    "to": "IFJ",
    "mode": "departure",
    "timeIso": "invalid-time-format"
  }' | jq
```

**Expected**:
- ✅ Returns error response (4 error messages)
- ✅ Doesn't crash
- ✅ Console shows: `Error: ...`

---

## Test 4: Verify Caching Works

### 4a. Test Vegagerðin Caching

```bash
# First call - should fetch from API
time curl "http://localhost:8080/api/debug/obs?hours=1" > /dev/null

# Second call within 90 seconds - should use cache (faster)
time curl "http://localhost:8080/api/debug/obs?hours=1" > /dev/null
```

**Expected**:
- ✅ Second call is faster (uses cached data)
- ✅ No API errors

### 4b. Test IMO Caching

```bash
# First call
time curl -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d "{
    \"from\": \"RVK\",
    \"to\": \"IFJ\",
    \"mode\": \"departure\",
    \"timeIso\": \"$(date -u +'%Y-%m-%dT%H:%M:%SZ')\"
  }" > /dev/null

# Second call (should use cache)
time curl -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d "{
    \"from\": \"RVK\",
    \"to\": \"IFJ\",
    \"mode\": \"departure\",
    \"timeIso\": \"$(date -u +'%Y-%m-%dT%H:%M:%SZ')\"
  }" > /dev/null
```

**Expected**:
- ✅ Second call faster (cached IMO data)
- ✅ Same hazards detected (from cached data)

---

## Test 5: Verify Hazard Detection

### 5a. Test with High Wind Data

If you have test data with high winds, verify hazards are detected:

```bash
curl -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d '{
    "from": "RVK",
    "to": "IFJ",
    "mode": "departure",
    "timeIso": "2025-11-10T14:00:00Z"
  }' | jq '.summaryStats.hazards'
```

**Expected**:
- ✅ Array with header message
- ✅ If wind ≥ 20 m/s: Warning Level messages
- ✅ If gusts ≥ 26 m/s: Gust warnings
- ✅ If visibility < 1000m: Low visibility warnings
- ✅ If freezing + precip: Ice risk warnings

---

## Test 6: Response Format Verification

**Critical for Frontend Integration**:

```bash
curl -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d '{
    "from": "RVK",
    "to": "IFJ",
    "mode": "departure",
    "timeIso": "2025-11-10T14:00:00Z"
  }' | jq '{
    advice_count: (.advice | length),
    advice_is_array: (.advice | type == "array"),
    hazards_count: (.summaryStats.hazards | length),
    stations_count: (.mapData.stations | length),
    route_type: .mapData.route.type,
    route_coords_count: (.mapData.route.coordinates | length)
  }'
```

**Expected**:
- ✅ `advice_count`: 4
- ✅ `advice_is_array`: true
- ✅ `hazards_count`: ≥ 1 (at least header)
- ✅ `stations_count`: ~8
- ✅ `route_type`: "LineString"
- ✅ `route_coords_count`: 9 (or your final polyline count)

---

## Test 7: End-to-End Flow Verification

**Complete flow test**:

```bash
curl -X POST http://localhost:8080/api/advice \
  -H "Content-Type: application/json" \
  -d '{
    "from": "RVK",
    "to": "IFJ",
    "mode": "departure",
    "timeIso": "2025-11-10T14:00:00Z"
  }' | jq '{
    has_advice: (.advice != null),
    advice_points: (.advice | length),
    has_hazards: (.summaryStats.hazards != null),
    hazards_header: (.summaryStats.hazards[0]),
    stations_used: .summaryStats.stationsUsed,
    has_route: (.mapData.route != null),
    has_stations: (.mapData.stations != null),
    station_count: (.mapData.stations | length)
  }'
```

**Expected**: All fields should be `true` or have valid values

---

## Checklist Summary

### Core Functionality
- [ ] Route endpoint returns valid GeoJSON
- [ ] Debug endpoints work (corridor, obs, imo)
- [ ] Advice endpoint works with departure mode
- [ ] Advice endpoint works with arrival mode
- [ ] Error handling works (invalid time format)

### Data & Processing
- [ ] Stations are fetched correctly (~8 stations)
- [ ] Observations are fetched and processed
- [ ] Data reduction works (segments created)
- [ ] Hazards are detected from API data
- [ ] Hazards header appears in response

### Caching
- [ ] Vegagerðin caching works (90 second TTL)
- [ ] IMO caching works (90 second TTL)
- [ ] Second calls are faster (use cache)

### Response Format
- [ ] Advice array has exactly 4 items
- [ ] Hazards array includes header + detected hazards
- [ ] Map data includes route coordinates
- [ ] Map data includes stations with lat/lon
- [ ] Summary stats include stationsUsed and window

### Integration
- [ ] OpenAI integration works (or returns placeholder)
- [ ] All services work together
- [ ] No crashes or errors
- [ ] Console logs show proper flow

---

## Quick Test Script

Save as `test-backend.sh`:

```bash
#!/bin/bash

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
```

Make executable: `chmod +x test-backend.sh`
Run: `./test-backend.sh`

---

## Common Issues & Solutions

**No observations returned**:
- Normal if stations don't have recent data
- Try different time windows
- Check external APIs are accessible

**OpenAI errors**:
- Check API key: `echo $OPENAI_API_KEY`
- Check console for specific error
- Placeholder responses are fine for testing flow

**Hazards empty**:
- Normal if conditions are below thresholds
- Should still have header message
- Test with different time windows

**Port 8080 in use**:
```bash
lsof -ti:8080 | xargs kill -9
```

---

## Ready for Frontend?

✅ **Backend is ready if**:
- All endpoints return valid JSON
- Response structure matches expected format
- Hazards are detected correctly
- No crashes or errors
- Caching works

**Frontend will need**:
- `advice`: Array of 4 strings
- `summaryStats.hazards`: Array of hazard strings
- `mapData.route.coordinates`: Array of [lon, lat] pairs
- `mapData.stations`: Array of station objects

Everything should be ready! 🚀

