# JavaScript Frontend Reference Guide
## Complete Function & Flow Documentation for RoadsAI Project
---

## 📋 TABLE OF ALL FUNCTIONS, VARIABLES & MEANINGS

---

### **app.js** (Entry Point)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `initialize()` | Function | Main entry point | Called when DOM loads, initializes form handling |
| `document.readyState` | Property | DOM readiness check | Checks if DOM is already loaded or still loading |
| `DOMContentLoaded` | Event | DOM ready event | Fires when HTML is fully parsed |

---

### **api-client.js** (Backend Communication)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `fetchObservations(request)` | Async Function | API call to backend | Sends POST request to `/api/observations` endpoint with route data |
| `request` | Parameter | Request payload | Object containing `from`, `to`, `forecastTime` |
| `response.ok` | Property | HTTP status check | Checks if response status is 200-299 (success) |
| `response.json()` | Method | Parse JSON response | Converts response body to JavaScript object |

---

### **form-handler.js** (Form Management & Validation)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `currentForecastTime` | Global Variable | Stores selected forecast time | Used to pass forecast time to display functions |
| `initializeForm()` | Function | Sets up entire form | Initializes date/time pickers, validation, submission handler |
| `initializeDateTimePickers()` | Function | Calendar & time setup | Creates calendar popup and time dropdown |
| `getTodayUTC()` | Function | Get today's date in UTC | Returns Date object set to midnight UTC |
| `buildCalendar()` | Function | Builds calendar HTML | Creates calendar grid with selectable dates (10-day forecast limit) |
| `FORECAST_DAYS` | Constant | Forecast limit | Maximum 10 days (Yr.no API limitation) |
| `MAX_TIME_MS` | Constant | Max time in milliseconds | Calculated from FORECAST_DAYS (10 days × 24 hours × 60 min × 60 sec × 1000 ms) |
| `selectedDateStr` | Variable | Currently selected date | Stores ISO date string (YYYY-MM-DD format) |
| `updateTimeOptions()` | Function | Populates time dropdown | Generates 15-minute interval time options based on selected date |
| `validateForm()` | Function | Form validation logic | Checks if form is valid: from/to different, date/time both or neither |
| `submitBtn.disabled` | Property | Submit button state | Enabled/disabled based on validation |
| `form.addEventListener('submit')` | Event Handler | Form submission | Handles form submit, prevents default, calls API, displays results |
| `showError(message)` | Function | Display error messages | Shows error in error div, removes hidden class |

---

### **results-display.js** (Results Orchestrator)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `displayResults(data, forecastTime)` | Function | Main results coordinator | Orchestrates all display functions, processes API response |
| `data` | Parameter | API response data | Contains `alerts`, `stations`, `route`, `observations`, `advice` |
| `alertsArray` | Variable | Processed alerts | Converts alerts Map to array format for display |
| `summaryStats` | Object | Summary statistics | Contains `stationsUsed` count |
| `mapData` | Object | Map visualization data | Contains `route.coordinates` and `stations` array |
| `observationsByStation` | Object | Grouped observations | Key-value object: `{stationId: [observations]}` |
| `window.LeafletMap` | Global Object | Map module | Contains map functions (loadLeaflet, initializeAdviceMap, clearMap) |
| `setTimeout()` | Function | Delayed map init | Performance optimization - loads map after 100ms delay |

---

### **advice-parser.js** (Text Parsing)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `parseAdviceText(adviceText)` | Function | Parses AI advice text | Extracts official alerts and road conditions from AI response |
| `adviceText` | Parameter | Raw AI advice string | Full text from AI service |
| `colonIndex` | Variable | Colon position | Finds first colon to separate header from content |
| `contentText` | Variable | Main content | Text after first colon (or full text if no colon) |
| `officialAlert` | Variable | Extracted alert | Text matching "OFFICIAL ALERT: <text>" pattern |
| `roadConditions` | Variable | Road conditions text | Content with official alert removed |
| `alertMatch` | Variable | Regex match result | Matches "OFFICIAL ALERT:" pattern in text |
| Return Object | Object | Parsed result | `{officialAlert: string|null, roadConditions: string}` |

---

### **advice-display.js** (Advice Table Display)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `cleanStationName(name)` | Function | Cleans station names | Removes prefixes (vedur.is, veg:, imo:) and extracts name from parentheses |
| `displayAdvice(adviceArray, stations, observationsByStation, forecastTime, alerts)` | Function | Creates advice table | Builds HTML table showing station data, weather, and AI advice |
| `adviceArray` | Parameter | AI advice per station | Array of advice strings, one per station (by index) |
| `stations` | Parameter | Station data | Array of station objects with id, name, latitude, longitude |
| `observationsByStation` | Parameter | Grouped observations | Object mapping stationId to observation arrays |
| `forecastTime` | Parameter | Selected forecast time | ISO string or null (null = current observations) |
| `alerts` | Parameter | Official alerts | Object mapping stationId to alert arrays |
| `isForecast` | Variable | Forecast vs current | Boolean: true if forecastTime is in future |
| `stationData` | Array | Combined station data | Array of objects with station info, weather, advice |
| `latestObs` | Variable | Latest observation | Most recent observation for a station (by timestamp) |
| `parsed` | Variable | Parsed advice | Result from parseAdviceText() |
| `tableContainer` | Element | Table wrapper | Main container div for advice table |
| `tableHeader` | Element | Table header row | Header with column labels and icons |
| `tableRow` | Element | Table data row | Row for each station |
| `dataBadge` | Element | Badge element | Shows "Current Observations" or "Future Forecast" |
| `warningBadge` | Element | Warning indicator | Shows "WARNING" (official alert) or "CAUTION" (threshold exceeded) |
| `isCautious` | Variable | Caution check | True if wind > 15 m/s, gusts > 20 m/s, visibility < 1000m, or temp < -10°C |

---

### **hazards-display.js** (Weather Warnings Display)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `displayHazards(hazards)` | Function | Shows weather warnings | Creates warning message box with alerts |
| `hazards` | Parameter | Hazards array | First element is heading, rest are alert messages |
| `hazardsContent` | Element | Container div | Element with id "hazardsContent" |
| `has-warnings` | CSS Class | Warning state | Added when warnings exist |
| `message` | Element | Message box | Main container for warning display |
| `createPrimaryLine(text)` | Function | Creates heading line | Builds heading with warning icons (⚠️) on both sides |
| `heading` | Element | Heading section | Contains primary and secondary heading lines |
| `primaryText` | Variable | Main heading text | Text before parentheses |
| `secondaryText` | Variable | Subtitle text | Text inside parentheses |
| `contentWrapper` | Element | Alert content | Contains actual alert messages |

---

### **summary-display.js** (Route Summary Display)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `displaySummary(summaryStats)` | Function | Shows route summary | Creates summary box with stats and external links |
| `summaryStats` | Parameter | Summary data | Object with `stationsUsed` count |
| `summaryContent` | Element | Container div | Element with id "summaryContent" |
| `message` | Element | Message box | Main container for summary |
| `heading` | Element | Heading section | Contains title and subtitle |
| `title` | Element | Main title | "Route Summary" text |
| `subtitle` | Element | Subtitle | "Weather Data Overview" text |
| `contentWrapper` | Element | Stats content | Shows station count |
| `linksSection` | Element | Links container | Section with external weather links |
| `externalLinks` | Array | Link data | Array of objects with `url` and `display` properties |

---

### **leaflet-map.js** (Map Visualization)

| Name | Type | Purpose | Details |
|------|------|---------|---------|
| `loadLeaflet()` | Function | Dynamically loads Leaflet | Loads Leaflet library from CDN if not already loaded |
| `L` | Global Object | Leaflet library | Leaflet.js library object (undefined until loaded) |
| `adviceMap` | Variable | Map instance | Leaflet map object for advice section |
| `adviceRouteLayer` | Variable | Route polyline | Leaflet polyline layer showing route |
| `adviceMapMarkers` | Array | Station markers | Array of Leaflet marker objects |
| `getRoadRoute(waypoints)` | Async Function | Gets route from OSRM | Calls OSRM API to get road-following route |
| `waypoints` | Parameter | Route coordinates | Array of [lon, lat] coordinate pairs |
| `OSRM` | External Service | Routing service | Open Source Routing Machine (free demo server) |
| `getRoadRouteWithRetry(waypoints, maxRetries)` | Async Function | Route with retry logic | Attempts OSRM call up to 3 times with exponential backoff |
| `maxRetries` | Parameter | Retry limit | Default 3 attempts |
| `clearMap()` | Function | Removes map | Cleans up map instance and layers |
| `initializeAdviceMap(mapData)` | Async Function | Sets up map | Creates Leaflet map, adds route, adds station markers |
| `mapData` | Parameter | Map data object | Contains `route.coordinates` and `stations` array |
| `routeCoords` | Variable | Route waypoints | Coordinates from backend [lon, lat] format |
| `leafletRouteCoords` | Variable | Converted coords | Coordinates converted to [lat, lon] for Leaflet |
| `L.map()` | Leaflet Method | Creates map | Initializes Leaflet map instance |
| `L.tileLayer()` | Leaflet Method | Adds map tiles | Adds satellite imagery (Esri World Imagery) |
| `L.polyline()` | Leaflet Method | Draws route line | Creates polyline layer for route visualization |
| `L.marker()` | Leaflet Method | Creates marker | Creates marker for each weather station |
| `marker.bindPopup()` | Leaflet Method | Adds popup | Adds clickable popup with station info |
| `window.LeafletMap` | Global Object | Public API | Exports map functions: loadLeaflet, initializeAdviceMap, clearMap |

---

## 🔄 COMPLETE JAVASCRIPT FLOW EXPLANATION

---

### **PHASE 1: INITIALIZATION (Page Load)**

1. **HTML loads** → All script tags load in order (defer attribute ensures they load after HTML parsing)

2. **app.js executes**:
   - Checks if DOM is ready (`document.readyState`)
   - If loading: waits for `DOMContentLoaded` event
   - If ready: calls `initialize()` immediately
   - `initialize()` calls `initializeForm()` from form-handler.js

3. **form-handler.js: initializeForm()**:
   - Gets form elements (form, date input, time select, submit button, from/to selects)
   - Calls `initializeDateTimePickers()` to set up calendar
   - Sets up validation listeners on all form fields
   - Attaches submit event handler to form
   - Calls `validateForm()` initially to set button state

4. **DateTime Picker Setup**:
   - `getTodayUTC()` calculates today's date in UTC
   - `buildCalendar()` creates calendar HTML with:
     - 42 days (6 weeks) in grid
     - Only dates within 10-day forecast window are selectable
     - Today is highlighted
     - Selected date is marked
   - `updateTimeOptions()` generates time dropdown:
     - If today: starts from current time (rounded to 15-min intervals)
     - If future date: starts from 00:00
     - Generates options every 15 minutes up to 10 days ahead
   - Calendar popup shows/hides on date input click
   - Time dropdown updates when date changes

5. **Form Validation Setup**:
   - `validateForm()` checks:
     - From and To must be selected and different
     - If date is selected, time must also be selected (or both empty)
   - Submit button disabled until form is valid
   - Auto-swaps from/to if user selects same value

---

### **PHASE 2: USER INTERACTION (Form Filling)**

1. **User selects From/To locations**:
   - Change event fires → `validateForm()` runs
   - If same location selected, other field auto-swaps
   - Submit button enabled/disabled accordingly

2. **User clicks date input**:
   - Calendar popup appears (`hidden` class removed)
   - `buildCalendar()` creates fresh calendar grid
   - User clicks date → date selected, calendar closes
   - `updateTimeOptions()` populates time dropdown
   - First available time auto-selected
   - `validateForm()` runs

3. **User selects time** (optional):
   - Change event fires → `validateForm()` runs
   - Form ready for submission when valid

---

### **PHASE 3: FORM SUBMISSION**

1. **User clicks "Get Advice" button**:
   - Form submit event fires
   - `e.preventDefault()` stops default form submission

2. **Pre-submission setup**:
   - Submit button disabled (prevents double-submission)
   - Previous results hidden (`results.classList.add('hidden')`)
   - Previous errors hidden (`error.classList.add('hidden')`)
   - Loading spinner shown (`loading.classList.remove('hidden')`)
   - Map cleared if exists (`window.LeafletMap.clearMap()`)

3. **Request preparation**:
   - Gets form values: `from`, `to`, `date`, `time`
   - Converts date+time to ISO-8601 UTC string:
     - If both provided: `new Date('${date}T${time}:00Z').toISOString()`
     - If empty: `null` (means current observations only)
   - Stores in `currentForecastTime` global variable
   - Creates request object: `{from, to, forecastTime}`

4. **API call**:
   - Calls `fetchObservations(request)` from api-client.js
   - Sends POST request to `/api/observations`
   - Waits for JSON response
   - If error: calls `showError()` and stops
   - If success: calls `displayResults(data, currentForecastTime)`

5. **Cleanup**:
   - Loading spinner hidden
   - `validateForm()` runs again (re-enables button if form still valid)

---

### **PHASE 4: RESULTS DISPLAY**

1. **results-display.js: displayResults()** receives API response:
   - `data` contains: `alerts`, `stations`, `route`, `observations`, `advice`

2. **Process Alerts**:
   - Converts `data.alerts` Map to array format
   - First element: heading "Official Weather Warnings..."
   - Remaining elements: individual alert messages
   - Calls `displayHazards(alertsArray)`

3. **Process Summary**:
   - Creates `summaryStats` object with `stationsUsed` count
   - Calls `displaySummary(summaryStats)`

4. **Process Map Data**:
   - Creates `mapData` object:
     - `route.coordinates`: array of [lon, lat] pairs
     - `stations`: array with id, name, lat, lon
   - Delays map initialization by 100ms (performance optimization)
   - Checks if Leaflet loaded, loads if needed
   - Calls `window.LeafletMap.initializeAdviceMap(mapData)`

5. **Process Observations**:
   - Groups observations by `stationId`:
     - Creates `observationsByStation` object
     - Key: stationId, Value: array of observations
   - This allows quick lookup of observations per station

6. **Process Advice**:
   - Calls `displayAdvice()` with:
     - `data.advice`: array of AI advice strings (one per station)
     - `data.stations`: station metadata
     - `observationsByStation`: grouped observations
     - `currentForecastTime`: selected forecast time
     - `data.alerts`: official alerts

7. **Show Results**:
   - Removes `hidden` class from results section
   - All display functions have populated their sections

---

### **PHASE 5: HAZARDS DISPLAY**

1. **hazards-display.js: displayHazards()**:
   - Clears previous content (`innerHTML = ''`)
   - Removes `has-warnings` class initially

2. **Create Message Box**:
   - Creates `message` div with class `hazards-message`

3. **Create Heading**:
   - If hazards exist:
     - First element is heading text
     - Splits at "(" to separate primary/secondary text
     - Creates primary line with warning icons (⚠️) on sides
     - Adds secondary line if parentheses found
   - If no hazards:
     - Default heading: "Official Weather Warnings (ICELANDIC METEOROLOGICAL OFFICE)"
     - Shows "No hazards detected" message

4. **Add Content**:
   - If hazards exist (length > 1):
     - Adds `has-warnings` class (for styling)
     - Joins remaining hazard messages with `<br><br>`
   - If no hazards:
     - Shows "No hazards detected" message

5. **Append to DOM**:
   - Appends message box to `hazardsContent` div

---

### **PHASE 6: SUMMARY DISPLAY**

1. **summary-display.js: displaySummary()**:
   - Clears previous content
   - Creates message box structure

2. **Create Heading**:
   - Title: "Route Summary"
   - Subtitle: "Weather Data Overview"

3. **Add Stats**:
   - Shows "Stations Used: X" from `summaryStats.stationsUsed`

4. **Add External Links**:
   - Creates links section with title
   - Adds 6 external weather links:
     - vedur.is, umferdin.is, yr.no, spakort.vedur.is, belgingur.is, windy.com
   - All links open in new tab (`target="_blank"`)

5. **Append to DOM**:
   - Appends message box to `summaryContent` div

---

### **PHASE 7: MAP INITIALIZATION**

1. **leaflet-map.js: initializeAdviceMap()**:
   - Checks if Leaflet library loaded (`typeof L !== 'undefined'`)
   - Gets map container element (`adviceMap` div)
   - Clears existing map if present

2. **Extract Route Data**:
   - Gets `routeCoords` from `mapData.route.coordinates` ([lon, lat] format)
   - Converts to Leaflet format: `leafletRouteCoords` ([lat, lon] format)

3. **Create Map Instance**:
   - `L.map('adviceMap')` creates map
   - Sets view to center of Iceland: `[64.8, -19.0]` zoom level 6
   - Adds satellite tile layer (Esri World Imagery)

4. **Wait for Map Ready**:
   - `adviceMap.whenReady()` callback fires when map loaded

5. **Get Road Route**:
   - Calls `getRoadRouteWithRetry(routeCoords)`:
     - Attempts up to 3 times
     - Calls OSRM API: `https://router.project-osrm.org/route/v1/driving/...`
     - Formats waypoints as `lon,lat;lon,lat;...`
     - Waits between retries (exponential backoff: 1s, 2s, 3s)
   - If successful: gets road-following route geometry
   - Converts coordinates from [lon, lat] to [lat, lon]

6. **Draw Route**:
   - If OSRM route exists:
     - Creates polyline with road-following route (blue line, weight 4)
   - If OSRM fails:
     - Falls back to direct polyline between waypoints

7. **Add Station Markers**:
   - Loops through `mapData.stations`
   - Creates marker at each station location: `L.marker([lat, lon])`
   - Binds popup with station name and ID
   - Stores markers in `adviceMapMarkers` array

8. **Map Stays Zoomed Out**:
   - Map shows all of Iceland (doesn't auto-fit to route)
   - Provides geographic context

---

### **PHASE 8: ADVICE DISPLAY**

1. **advice-display.js: displayAdvice()**:
   - Clears previous content
   - Determines if forecast or current: `isForecast = forecastTime !== null && new Date(forecastTime) > new Date()`

2. **Process Each Station**:
   - Loops through `stations` array
   - For each station:
     - Gets observations: `observationsByStation[station.id]`
     - Finds latest observation (by timestamp comparison)
     - Gets AI advice: `adviceArray[index]` (by array index)
     - Parses advice: `parseAdviceText(adviceText)` → extracts official alert and road conditions
     - Gets station alerts: `alerts[station.id]` or empty array
     - Creates `stationData` object with:
       - Station name (cleaned)
       - Temperature, wind, gusts (formatted with units)
       - Road conditions (from parsed advice)
       - Official alert (if present)
       - Latest observation
       - Station alerts

3. **Create Table Structure**:
   - Creates `tableContainer` div
   - Creates `tableHeader` with columns:
     - Station (with data badge: "Current Observations" or "Future Forecast")
     - Temperature (with icon)
     - Wind (with icon)
     - Gusts (with icon)
     - Road Conditions (with icon)

4. **Create Table Rows**:
   - For each station in `stationData`:
     - Creates `tableRow` div
     - Station cell:
       - Station name
       - Warning badge if:
         - Official alerts exist → "WARNING" badge
         - OR threshold exceeded → "CAUTION" badge (wind > 15, gusts > 20, visibility < 1000m, temp < -10°C)
       - Official alert badge if present (⚠️ + alert text)
     - Data cells: temperature, wind, gusts, road conditions

5. **Append to DOM**:
   - Appends table to `adviceContent` div

---

### **PHASE 9: ADVICE PARSING (During Display)**

1. **advice-parser.js: parseAdviceText()**:
   - Finds first colon index
   - Extracts content after colon (or full text if no colon)
   - Searches for "OFFICIAL ALERT: <text>" pattern using regex
   - If found:
     - Extracts alert text
     - Removes alert from road conditions text
   - Cleans up road conditions (removes leading/trailing punctuation)
   - Returns object: `{officialAlert: string|null, roadConditions: string}`

---

## 🔗 FUNCTION CALL CHAIN (Complete Flow)

```
Page Load
  ↓
app.js: initialize()
  ↓
form-handler.js: initializeForm()
  ├─→ initializeDateTimePickers()
  │   ├─→ getTodayUTC()
  │   ├─→ buildCalendar()
  │   └─→ updateTimeOptions()
  ├─→ validateForm()
  └─→ form.addEventListener('submit', ...)
      │
      User Submits Form
      ↓
      form-handler.js: submit handler
        ├─→ fetchObservations(request)
        │   └─→ api-client.js: fetchObservations()
        │       └─→ POST /api/observations
        │
        └─→ displayResults(data, forecastTime)
            └─→ results-display.js: displayResults()
                ├─→ displayHazards(alertsArray)
                │   └─→ hazards-display.js: displayHazards()
                │
                ├─→ displaySummary(summaryStats)
                │   └─→ summary-display.js: displaySummary()
                │
                ├─→ window.LeafletMap.initializeAdviceMap(mapData)
                │   └─→ leaflet-map.js: initializeAdviceMap()
                │       ├─→ loadLeaflet() [if needed]
                │       └─→ getRoadRouteWithRetry(waypoints)
                │           └─→ getRoadRoute(waypoints)
                │               └─→ OSRM API call
                │
                └─→ displayAdvice(adviceArray, stations, observationsByStation, forecastTime, alerts)
                    └─→ advice-display.js: displayAdvice()
                        ├─→ parseAdviceText(adviceText)
                        │   └─→ advice-parser.js: parseAdviceText()
                        └─→ cleanStationName(name)
```

---

## 🎯 KEY CONCEPTS TO REMEMBER FOR EXAM

1. **Modular Architecture**: Each JS file handles one concern (form, API, display, map, parsing)

2. **Global Variables**: 
   - `currentForecastTime` - stores selected forecast time
   - `window.LeafletMap` - public API for map functions

3. **Async Flow**: Form submission → API call → Results display (all async/await)

4. **Data Transformation**:
   - Backend sends [lon, lat] → Frontend converts to [lat, lon] for Leaflet
   - Alerts Map → Array for display
   - Observations array → Grouped by stationId object

5. **Performance Optimizations**:
   - Map loads only when needed (lazy loading)
   - Map initialization delayed by 100ms
   - Leaflet library loaded dynamically

6. **Error Handling**:
   - API errors caught and displayed
   - OSRM retry logic (3 attempts with backoff)
   - Fallback to direct polyline if OSRM fails

7. **Form Validation**:
   - Real-time validation on all inputs
   - Auto-swap locations if same selected
   - Submit button state reflects validity

8. **Date/Time Logic**:
   - 10-day forecast limit (Yr.no API constraint)
   - UTC timezone handling
   - 15-minute interval time options
   - Today's times start from current time

---

## 📝 EXAM TIPS

- **Flow**: Always start from user action → form submission → API → display
- **Data Flow**: Backend response → Processing → DOM manipulation
- **Key Functions**: Know what each display function does and when it's called
- **Map Flow**: OSRM route → Leaflet polyline → Station markers
- **Advice Flow**: AI text → Parse → Extract alerts → Display in table
- **State Management**: Global variables track forecast time, map instances
- **Error Handling**: Every async operation has try/catch or error handling

---

**Good luck with your exam! 🎓**

