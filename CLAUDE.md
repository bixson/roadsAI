# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**roadsAI** is an AI-powered Iceland road safety advisory system that analyzes real-time weather conditions along the Reykjavík ↔ Ísafjörður route and generates AI-driven driving recommendations using OpenAI's GPT-4o-mini model. The application integrates multiple meteorological data sources and provides hazard-aware route guidance.

**Technology Stack:**
- Backend: Java 21, Spring Boot 3.5.7, Spring WebFlux
- Build: Maven
- Frontend: Vanilla JavaScript, HTML5, CSS3
- AI: OpenAI Chat API (GPT-4o-mini)
- External Data: Vegagerðin (Road Authority), vedur.is (Icelandic Meteorological Office)

## Build and Run

```bash
# Build and install dependencies
./mvnw clean install

# Run application (development)
./mvnw spring-boot:run

# Run specific test class
./mvnw test -Dtest=VedurAwsDtoTest

# Run all tests
./mvnw test

# Package production JAR
./mvnw package
java -jar target/roadsAI-0.0.1-SNAPSHOT.jar
```

**Required Setup:**
```bash
export OPENAI_API_KEY="sk-..."  # Set your OpenAI API key
```

**Server:** Runs on `http://localhost:8080` with embedded Tomcat

## Project Structure

```
src/main/java/dk/ek/roadsai/
├── RoadsAiApplication.java              (Spring Boot entry point)
├── controller/
│   ├── AdviceController.java            (Main POST /api/advice endpoint)
│   └── RouteController.java             (GET /api/route/rvk-isf endpoint)
├── service/
│   ├── RouteService.java                (Hardcoded RVK↔IFJ route coordinates)
│   ├── TimeWindowService.java           (Departure/arrival time window calculation)
│   ├── StationService.java              (Station aggregation & coordination)
│   ├── CorridorFilter.java              (Route-based station filtering)
│   ├── DataReducer.java                 (Weather observation aggregation)
│   ├── HazardDetector.java              (Wind/visibility/freezing hazard detection)
│   ├── PromptBuilder.java               (OpenAI prompt construction with requirements)
│   ├── OpenAiService.java               (OpenAI Chat API integration)
│   ├── VegagerdinProvider.java          (Road authority station data - 5 stations)
│   ├── VedurAwsProvider.java            (IMO AWS weather station data - 3 stations)
│   └── VedurCapProvider.java            (Common Alerting Protocol weather alerts)
├── model/
│   ├── Station.java                     (Station record with coords & metadata)
│   └── StationObservation.java          (Weather observation record)
├── dto/
│   ├── AdviceRequest.java               (User input: from, to, mode, timeIso)
│   ├── AdviceResponse.java              (API response: advice, hazards, map)
│   ├── CapAlert.java                    (Weather alert record)
│   ├── OpenAiRequest/Response.java      (OpenAI API DTOs)
│   ├── vedur/is/VedurAwsDto.java        (IMO weather data DTO)
│   └── vegagerdin/*Dto.java             (Vegagerðin station DTOs)
└── util/
    └── GeoDistance.java                 (Haversine & polyline calculations)

src/main/resources/
├── application.properties               (OpenAI config: key, model, timeout)
└── static/
    ├── index.html                       (Main UI with form & results)
    ├── app.js                           (Client-side logic & API calls)
    ├── css/
    │   ├── style.css                    (Main stylesheet with imports)
    │   └── components/                  (Component-based CSS)
    │       ├── reset.css, utility.css, container.css
    │       ├── header.css, form.css, loading.css
    │       ├── error.css, results.css
    └── images/png/                      (Departure, arrival icons)
```

## Architecture and Data Flow

### High-Level Flow

```
User Request (RVK→IFJ, departure, time)
    ↓
[AdviceController.advice()]
    ├─→ [RouteService] → Get route coordinates
    ├─→ [TimeWindowService] → Calculate observation window
    ├─→ [StationService] → Get 8 stations (5 road + 3 weather)
    │   └─→ [CorridorFilter] → Filter to 5km buffer
    ├─→ [StationService.fetchObs()] → Fetch weather observations
    │   ├─→ [VegagerdinProvider] → 5 road authority stations
    │   └─→ [VedurAwsProvider] → 3 IMO weather stations
    ├─→ [VedurCapProvider] → Fetch CAP weather alerts
    ├─→ [DataReducer] → Aggregate worst-case metrics per station
    ├─→ [HazardDetector] → Classify wind/visibility/freezing hazards
    ├─→ [PromptBuilder] → Build system + user prompts with requirements
    ├─→ [OpenAiService.ask()] → Call OpenAI Chat API
    └─→ Return AdviceResponse (9 advice points + hazards + map)
```

### Key Components

**AdviceController** (`POST /api/advice`)
- Main orchestrator: coordinates all services
- Input: `AdviceRequest` (from, to, mode, timeIso)
- Output: `AdviceResponse` (advice[], hazards[], mapData{})
- Validates: Reverses route if IFJ→RVK, calculates time window

**RouteService**
- Hardcoded 12-waypoint route: Reykjavík [−21.8046, 64.1238] ↔ Ísafjörður [−23.1239, 66.0746]
- Total distance: ~450km
- Supports bidirectional lookup (auto-reversal for return trips)
- Returns GeoJSON Feature for map rendering

**TimeWindowService**
- **Departure Mode:** 4-hour forward window from departure time
- **Arrival Mode:** Calculates backward assuming 90 km/h average speed, adds ±2h buffer
- Formula: `depart = arrival - (routeKm / 90) * 3600`

**StationService** (Aggregator)
- Combines 5 Vegagerðin stations + 3 IMO/vedur.is stations
- Routes fetch requests to correct provider by station type
- Filters stations to 5km buffer around route (via `CorridorFilter`)

**Data Providers**
- **VegagerdinProvider:** Wind speed, gusts, temperature (5 road stations, 15min cache)
- **VedurAwsProvider:** Comprehensive data including visibility, precipitation (3 weather stations, 15min cache)
- **VedurCapProvider:** Official weather alerts within 30km radius (30min cache)

**DataReducer**
- Aggregates observations per station to worst-case metrics
- Output: `Map<stationId, SegmentFacts>` with maxGust, windMs, minTemp, minVis, alerts
- Used by HazardDetector and PromptBuilder

**HazardDetector**
- Wind thresholds (Icelandic Road Safety guidelines):
  - Level 1: wind ≥20.0 m/s or gusts ≥26.0 m/s
  - Level 2: wind ≥24.0 m/s or gusts ≥30.0 m/s
  - Level 3: wind ≥28.0 m/s or gusts ≥35.0 m/s
- Other hazards: visibility <1000m, freezing+precipitation (ice risk)

**PromptBuilder**
- Constructs system prompt: professional tone, avoids generic phrases ("be careful"), data-driven focus
- Constructs user prompt with **CRITICAL requirement:** exactly 9 advice points (one per station segment)
- Marks official alerts with prefix: "**OFFICIAL ALERTS**"
- Specifies output format: station name + metrics + 20–25 words

**OpenAiService**
- Calls OpenAI Chat Completions API with system + user prompts
- Model: `gpt-4o-mini` (configured in `application.properties`)
- Timeout: 30 seconds
- Parses response: splits into 9 points, strips numbering/bullets, filters generic phrases
- Fallback: returns default advice on error/timeout

**CorridorFilter**
- Filters stations to those within 5km perpendicular distance of route
- Sorts by progress along route (distance from start)
- Uses equirectangular projection for segment distances, Haversine for final distance

**GeoDistance** (Utility)
- `haversineM()`: Great-circle distance (meters)
- `polylineLengthM()`: Total route length
- `pointToPolylineM()`: Shortest distance from point to polyline
- `progressAlongPolylineM()`: Position along route (meters from start)

## Configuration

**File:** `src/main/resources/application.properties`

```properties
spring.application.name=roadsAI
openai.api.key=${OPENAI_API_KEY:}    # Override with environment variable
openai.api.model=gpt-4o-mini          # GPT model selection
openai.api.timeout=30000              # Timeout in milliseconds
```

**Environment Variables:**
- `OPENAI_API_KEY` (required): Your OpenAI API key (format: `sk-...`)

## Frontend Architecture

**index.html**
- Form with dropdowns: From (RVK/IFJ), To (RVK/IFJ), Mode (Departure/Arrival with icons), Time (ISO datetime)
- Results area with three sections: Hazards, Advice (grouped by segments), Summary stats
- Prevents From=To by auto-swapping
- Form validation: submit disabled until all fields populated

**app.js** (Client-side logic)
- Event listeners: form submission, input validation, time default
- API call: `POST /api/advice` with `AdviceRequest` JSON
- Response parsing: extracts advice[], hazards[], mapData{}
- Rendering: populates hazards section, groups 9 advice points into 3 segments (3 per segment)
- Error handling: displays error messages, hides loading spinner

**CSS Structure** (Component-based)
- `style.css`: Main entry point with imports
- `components/reset.css`: Browser defaults
- `components/container.css`: Layout (flexbox, grid)
- `components/form.css`: Form styling (tiles, radio buttons, input validation)
- `components/results.css`: Results display (collapsible sections, chat-bubble advice)
- `components/loading.css`: Spinner animation
- `components/error.css`: Error message styling
- Color scheme: professional blues/grays with accent colors

## API Reference

| Endpoint | Method | Input | Output | Purpose |
|----------|--------|-------|--------|---------|
| `/api/advice` | POST | `AdviceRequest{from, to, mode, timeIso}` | `AdviceResponse{advice[], summaryStats, mapData}` | Main advice endpoint |
| `/api/route/rvk-isf` | GET | — | GeoJSON Feature | Route geometry for map |
| `/` | GET | — | HTML | Main application page |

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=VedurAwsDtoTest

# Test files
src/test/java/dk/ek/roadsai/
├── RoadsAiApplicationTests.java      (Spring Boot context test)
└── VedurAwsDtoTest.java              (DTO parsing validation)
```

## Design Decisions

1. **Hardcoded Route:** Route is fixed (RVK ↔ IFJ) for simplicity. Can be extracted to database/parameter for scalability.

2. **Dual Data Providers:** Vegagerðin (road authority) + IMO/vedur.is (meteorological) for redundancy and comprehensive coverage.

3. **AI Prompt Engineering:** System prompt defines behavior; user prompt contains hard requirements (exactly 9 points, format rules) to ensure consistent output.

4. **Haversine + Equirectangular Hybrid:** Accurate for long distances (Haversine) with good local approximation (equirectangular).

5. **Caching:** Vegagerðin/IMO data: 15min TTL; CAP alerts: 30min TTL. Balances freshness with API rate limits.

6. **Frontend-Agnostic Response:** JSON response includes all data; frontend independently formats/renders results.

## Common Development Tasks

### Adding a New Data Provider

1. Create class implementing `StationProvider` interface:
   ```java
   public interface StationProvider {
       List<Station> getStations();
       List<StationObservation> fetchObservations(List<Station> stations, Instant start, Instant end);
   }
   ```

2. Inject into `StationService` constructor:
   ```java
   private final List<StationProvider> providers;
   ```

3. Register in aggregation logic in `StationService.corridorStations()`.

### Modifying Route

1. Edit `RouteService.getRoute()` to return new coordinates array
2. Update route documentation in this CLAUDE.md
3. Re-run time window tests

### Adjusting Hazard Thresholds

Edit `HazardDetector.java` constants:
- `WIND_LEVEL_1_MS`, `WIND_LEVEL_2_MS`, `WIND_LEVEL_3_MS`
- `GUST_LEVEL_1_MS`, `GUST_LEVEL_2_MS`, `GUST_LEVEL_3_MS`
- `MIN_VISIBILITY_M`, `FREEZING_THRESHOLD_C`

### Changing AI Prompt Format

Edit `PromptBuilder.java`:
- `buildSystemPrompt()`: Change AI behavior/tone
- `buildUserPrompt()`: Change data format/requirements
- Update `OpenAiService.parseAdvicePoints()` if output format changes

## Troubleshooting

| Issue | Solution |
|-------|----------|
| 401 Unauthorized from OpenAI | Verify `OPENAI_API_KEY` env var is set and valid |
| API timeout (>30s) | Check network; increase `openai.api.timeout` in properties |
| Empty advice response | Check prompt requirements in `PromptBuilder`; verify OpenAI response parsing in `OpenAiService` |
| No stations found | Verify route coordinates in `RouteService`; check 5km buffer in `CorridorFilter` |
| Incorrect time window | Debug `TimeWindowService` calculation; check route length in `GeoDistance.polylineLengthM()` |

## Key Files for Understanding

1. **Entry Point:** `RoadsAiApplication.java` — Spring Boot startup
2. **Main Controller:** `AdviceController.java` — Service orchestration
3. **Core Services:** `RouteService.java`, `StationService.java`, `DataReducer.java`, `PromptBuilder.java`
4. **Data Providers:** `VegagerdinProvider.java`, `VedurAwsProvider.java`
5. **Frontend:** `index.html`, `app.js`, `css/style.css`

## Package Organization

- `controller`: HTTP endpoints and request/response handling
- `service`: Business logic (data aggregation, hazard detection, prompt building, API calls)
- `model`: Core domain records (Station, Observation)
- `dto`: Data Transfer Objects for external APIs
- `util`: Utility functions (geographic calculations)

## Reference Data

**Route:** Reykjavík [−21.8046, 64.1238] → Ísafjörður [−23.1239, 66.0746], ~450km

**Stations:**
- **Vegagerðin (Road Authority):** HFNFJ, BRATT, THROS, STEHE, OGURI
- **vedur.is (IMO AWS):** Reykjavík (Faxaflói), Hólmavík, Ísafjörður

**External APIs:**
- Vegagerðin: `https://gagnaveita.vegagerdin.is/api/vedur2014_1`
- vedur.is AWS: `https://api.vedur.is/weather/observations/aws/10min/latest`
- vedur.is CAP: `https://api.vedur.is/cap/v1/lat/{lat}/long/{lon}...`
- OpenAI: `https://api.openai.com/v1/chat/completions`
