/**
 * Roads Advisor V2 - Interactive Map Application
 * Full-page Google Maps with weather station overlays
 */

// =============================================================================
// State Management
// =============================================================================
const App = {
    map: null,
    markers: [],
    routeLine: null,
    infoWindows: [],
    // Store advice per station name
    stationAdvice: {},
    routeData: null,
    mapReady: false,
    // Custom hover boxes for stations
    hoverBoxes: new Map(), // Map of stationId -> hover box element
    pinnedStations: new Set() // Set of pinned station IDs
};

// =============================================================================
// Initialization
// =============================================================================
document.addEventListener('DOMContentLoaded', initialize);

async function initialize() {
    console.log('🛣️ Roads Advisor V2 initializing...');

    setupFormHandlers();
    setupPanelHandlers();
    await loadGoogleMaps();
}

// =============================================================================
// Google Maps Loading
// =============================================================================
async function loadGoogleMaps() {
    try {
        const response = await fetch('/api/v2/maps-config');
        const config = await response.json();

        if (!config.configured || !config.apiKey) {
            showError('Google Maps API not configured. Please set GOOGLE_MAPS_API_KEY.');
            return;
        }

        // Store map ID for AdvancedMarkerElement
        if (config.mapId) {
            window.GOOGLE_MAPS_MAP_ID = config.mapId;
        }

        // Load Google Maps script
        const script = document.createElement('script');
        script.src = `https://maps.googleapis.com/maps/api/js?key=${config.apiKey}&libraries=marker&callback=onGoogleMapsLoaded&loading=async`;
        script.async = true;
        script.defer = true;
        script.onerror = () => showError('Failed to load Google Maps');
        document.head.appendChild(script);

    } catch (error) {
        console.error('Maps config error:', error);
        showError('Failed to initialize maps. Check API configuration.');
    }
}

// Google Maps callback
window.onGoogleMapsLoaded = function () {
    console.log('Google Maps API loaded');
    initializeMap();
};

function initializeMap() {
    const container = document.getElementById('map');

    if (!container) {
        console.error('Map container not found!');
        return;
    }

    console.log('Map container size:', container.offsetWidth, 'x', container.offsetHeight);

    const options = {
        center: { lat: 64.9631, lng: -19.0208 }, // Iceland center
        zoom: 6,
        mapTypeId: 'roadmap',
        mapTypeControl: true,
        mapTypeControlOptions: {
            style: google.maps.MapTypeControlStyle.DROPDOWN_MENU,
            position: google.maps.ControlPosition.TOP_RIGHT,
            mapTypeIds: ['roadmap', 'satellite', 'hybrid', 'terrain']
        },
        zoomControl: true,
        zoomControlOptions: {
            position: google.maps.ControlPosition.RIGHT_CENTER
        },
        streetViewControl: false,
        fullscreenControl: true,
        fullscreenControlOptions: {
            position: google.maps.ControlPosition.RIGHT_TOP
        },
        gestureHandling: 'greedy',
        // Custom dark styling
        styles: [
            { elementType: 'geometry', stylers: [{ color: '#1d2c4d' }] },
            { elementType: 'labels.text.fill', stylers: [{ color: '#8ec3b9' }] },
            { elementType: 'labels.text.stroke', stylers: [{ color: '#1a3646' }] },
            { featureType: 'administrative.country', elementType: 'geometry.stroke', stylers: [{ color: '#4b6878' }] },
            { featureType: 'administrative.land_parcel', elementType: 'labels.text.fill', stylers: [{ color: '#64779e' }] },
            { featureType: 'administrative.province', elementType: 'geometry.stroke', stylers: [{ color: '#4b6878' }] },
            { featureType: 'landscape.man_made', elementType: 'geometry.stroke', stylers: [{ color: '#334e87' }] },
            { featureType: 'landscape.natural', elementType: 'geometry', stylers: [{ color: '#023e58' }] },
            { featureType: 'poi', elementType: 'geometry', stylers: [{ color: '#283d6a' }] },
            { featureType: 'poi', elementType: 'labels.text.fill', stylers: [{ color: '#6f9ba5' }] },
            { featureType: 'poi', elementType: 'labels.text.stroke', stylers: [{ color: '#1d2c4d' }] },
            { featureType: 'poi.park', elementType: 'geometry.fill', stylers: [{ color: '#023e58' }] },
            { featureType: 'poi.park', elementType: 'labels.text.fill', stylers: [{ color: '#3C7680' }] },
            { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#304a7d' }] },
            { featureType: 'road', elementType: 'labels.text.fill', stylers: [{ color: '#98a5be' }] },
            { featureType: 'road', elementType: 'labels.text.stroke', stylers: [{ color: '#1d2c4d' }] },
            { featureType: 'road.highway', elementType: 'geometry', stylers: [{ color: '#2c6675' }] },
            { featureType: 'road.highway', elementType: 'geometry.stroke', stylers: [{ color: '#255763' }] },
            { featureType: 'road.highway', elementType: 'labels.text.fill', stylers: [{ color: '#b0d5ce' }] },
            { featureType: 'road.highway', elementType: 'labels.text.stroke', stylers: [{ color: '#023e58' }] },
            { featureType: 'transit', elementType: 'labels.text.fill', stylers: [{ color: '#98a5be' }] },
            { featureType: 'transit', elementType: 'labels.text.stroke', stylers: [{ color: '#1d2c4d' }] },
            { featureType: 'transit.line', elementType: 'geometry.fill', stylers: [{ color: '#283d6a' }] },
            { featureType: 'transit.station', elementType: 'geometry', stylers: [{ color: '#3a4762' }] },
            { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#0e1626' }] },
            { featureType: 'water', elementType: 'labels.text.fill', stylers: [{ color: '#4e6d70' }] }
        ]
    };

    // Add Map ID if available (for AdvancedMarkerElement)
    if (window.GOOGLE_MAPS_MAP_ID) {
        options.mapId = window.GOOGLE_MAPS_MAP_ID;
        console.log('Using Map ID for AdvancedMarkerElement');
    }

    App.map = new google.maps.Map(container, options);
    App.mapReady = true;

    // Add listeners to reposition hover boxes when map moves/zooms
    App.map.addListener('zoom_changed', repositionAllHoverBoxes);
    App.map.addListener('bounds_changed', repositionAllHoverBoxes);
    App.map.addListener('center_changed', repositionAllHoverBoxes);

    // Enable form
    updateFormState();

    console.log('Map initialized');
}

// =============================================================================
// Form Handling
// =============================================================================
const LOCATIONS = [
    'Reykjavík', 'Akureyri', 'Ísafjörður', 'Egilsstaðir', 'Keflavík Airport',
    'Vík', 'Höfn', 'Selfoss', 'Borgarnes', 'Húsavík', 'Mývatn', 'Blönduós',
    'Sauðárkrókur', 'Siglufjörður', 'Stykkishólmur'
];

function setupFormHandlers() {
    const form = document.getElementById('routeForm');
    const origin = document.getElementById('origin');
    const destination = document.getElementById('destination');
    const originDropdown = document.getElementById('originDropdown');
    const destinationDropdown = document.getElementById('destinationDropdown');

    // Setup custom dropdowns
    setupCustomDropdown(origin, originDropdown);
    setupCustomDropdown(destination, destinationDropdown);

    // Validation on input
    [origin, destination].forEach(input => {
        input.addEventListener('input', () => {
            updateFormState();
            // Filter dropdown options as user types
            const dropdown = input === origin ? originDropdown : destinationDropdown;
            filterDropdown(input.value, dropdown);
        });
    });

    // Form submit
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await calculateRoute();
    });
}

function setupCustomDropdown(input, dropdown) {
    // Populate dropdown
    function populateDropdown(filter = '') {
        dropdown.innerHTML = '';
        const filtered = LOCATIONS.filter(loc =>
            loc.toLowerCase().includes(filter.toLowerCase())
        );

        filtered.forEach(location => {
            const item = document.createElement('div');
            item.className = 'dropdown-item';
            item.textContent = location;
            item.addEventListener('click', () => {
                input.value = location;
                input.closest('.custom-dropdown').classList.remove('active');
                updateFormState();
            });
            dropdown.appendChild(item);
        });
    }

    // Show dropdown on focus/click
    input.addEventListener('focus', () => {
        input.closest('.custom-dropdown').classList.add('active');
        populateDropdown(input.value);
    });

    input.addEventListener('click', () => {
        input.closest('.custom-dropdown').classList.add('active');
        populateDropdown(input.value);
    });

    // Hide dropdown when clicking outside
    document.addEventListener('click', (e) => {
        if (!input.closest('.custom-dropdown').contains(e.target)) {
            input.closest('.custom-dropdown').classList.remove('active');
        }
    });

    // Filter on input
    input.addEventListener('input', () => {
        populateDropdown(input.value);
    });
}

function filterDropdown(filter, dropdown) {
    const items = dropdown.querySelectorAll('.dropdown-item');
    items.forEach(item => {
        if (item.textContent.toLowerCase().includes(filter.toLowerCase())) {
            item.style.display = 'block';
        } else {
            item.style.display = 'none';
        }
    });
}

function updateFormState() {
    const origin = document.getElementById('origin').value.trim();
    const dest = document.getElementById('destination').value.trim();
    const btn = document.getElementById('submitBtn');

    const isValid = origin.length >= 2 && dest.length >= 2 &&
        origin.toLowerCase() !== dest.toLowerCase() &&
        App.mapReady;

    btn.disabled = !isValid;
}

// =============================================================================
// Route Calculation
// =============================================================================
async function calculateRoute() {
    const origin = document.getElementById('origin').value.trim();
    const destination = document.getElementById('destination').value.trim();
    const date = document.getElementById('date').value;
    const time = document.getElementById('time').value;

    let forecastTime = null;
    if (date && time) {
        forecastTime = new Date(`${date}T${time}:00`).toISOString();
    }

    showLoading('Calculating route...');

    try {
        const response = await fetch('/api/v2/route', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                origin,
                destination,
                corridorWidthKm: 15,
                forecastTime
            })
        });

        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.message || `Request failed: ${response.status}`);
        }

        const data = await response.json();
        console.log('Route data:', data);

        App.routeData = data;

        showLoading('Loading weather data...');
        displayRoute(data);
        displayWeatherPanel(data);
        displayRouteSummary(data);
        displayRouteSummary(data);
        // displayAdvice(data); // Removed sync call

        // Fetch advice in background
        loadAdviceAsync();

    } catch (error) {
        console.error('Route error:', error);
        showError(error.message);
    } finally {
        hideLoading();
    }
}

async function loadAdviceAsync() {
    const origin = document.getElementById('origin').value.trim();
    const destination = document.getElementById('destination').value.trim();
    const date = document.getElementById('date').value;
    const time = document.getElementById('time').value;

    let forecastTime = null;
    if (date && time) {
        forecastTime = new Date(`${date}T${time}:00`).toISOString();
    }

    // Show indicator
    const statusEl = document.getElementById('weatherStatus');
    let analyzingSpan = null;
    if (statusEl) {
        analyzingSpan = document.createElement('span');
        analyzingSpan.style.cssText = 'font-size:0.8em;opacity:0.8;margin-left:8px;';
        analyzingSpan.textContent = '✨ Analyzing...';
        statusEl.appendChild(analyzingSpan);
    }

    try {
        const response = await fetch('/api/v2/advice', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                origin,
                destination,
                corridorWidthKm: 15,
                forecastTime
            })
        });

        if (response.ok) {
            const adviceData = await response.json();
            console.log('Advice fetched:', adviceData);

            // Process advice
            displayAdvice(adviceData);

            // Refresh markers to show advice
            refreshMarkers();

            // Remove analyzing indicator
            if (analyzingSpan && analyzingSpan.parentNode) {
                analyzingSpan.remove();
            }

            // Refresh status display to ensure it's clean
            if (App.routeData) {
                displayRouteSummary(App.routeData);
            }
        } else {
            // Remove analyzing indicator on error too
            if (analyzingSpan && analyzingSpan.parentNode) {
                analyzingSpan.remove();
            }
        }
    } catch (e) {
        console.warn("Failed to load advice async", e);
        // Remove analyzing indicator on error
        if (analyzingSpan && analyzingSpan.parentNode) {
            analyzingSpan.remove();
        }
    }
}

// =============================================================================
// Map Display
// =============================================================================
function clearMarkers() {
    App.markers.forEach(m => m.setMap(null));
    App.markers = [];
    App.infoWindows.forEach(iw => iw.close());
    App.infoWindows = [];
    // Clear custom hover boxes
    App.hoverBoxes.forEach(box => box.remove());
    App.hoverBoxes.clear();
    App.pinnedStations.clear();
}

function clearMap() {
    if (App.routeLine) {
        App.routeLine.setMap(null);
        App.routeLine = null;
    }
    clearMarkers();
}

function refreshMarkers() {
    if (!App.routeData) return;
    clearMarkers();
    drawMarkers(App.routeData);
}

function displayRoute(data) {
    // Clear everything first
    if (App.routeLine) {
        App.routeLine.setMap(null);
        App.routeLine = null;
    }
    clearMarkers();

    const coords = data.routeCoordinates || [];
    if (coords.length === 0) return;

    // Convert coordinates
    const path = coords.map(c => {
        if (Array.isArray(c)) return { lat: c[1], lng: c[0] };
        return { lat: c.latitude || c.lat, lng: c.longitude || c.lon };
    });

    // Draw route with glow effect (background line)
    const routeGlow = new google.maps.Polyline({
        path,
        geodesic: true,
        strokeColor: '#00d4ff',
        strokeOpacity: 0.3,
        strokeWeight: 14
    });
    routeGlow.setMap(App.map);
    // Note: GLOW is treated as a marker for cleanup purposes in previous code? 
    // Wait, App.markers.push(routeGlow) was used. 
    // We should track glows separately or include them in 'markers' array but they are polylines.
    // Let's push to markers array for simple cleanup for now, or separate list.
    // simpler to push to markers for now as clearMarkers handles setMap(null).
    App.markers.push(routeGlow);

    // Main route line
    App.routeLine = new google.maps.Polyline({
        path,
        geodesic: true,
        strokeColor: '#00d4ff',
        strokeOpacity: 1,
        strokeWeight: 4,
        icons: [{
            icon: {
                path: 'M 0,-1 0,1',
                strokeOpacity: 1,
                strokeWeight: 2,
                scale: 3
            },
            offset: '0',
            repeat: '20px'
        }]
    });
    App.routeLine.setMap(App.map);

    // Fit bounds
    const bounds = new google.maps.LatLngBounds();
    path.forEach(p => bounds.extend(p));
    App.map.fitBounds(bounds, { top: 80, right: 380, bottom: 80, left: 350 });

    // Draw markers
    drawMarkers(data, path);
}

function drawMarkers(data, pathOverride) {
    // Need path for endpoints. If called from refreshMarkers, we need to reconstruct path or store it?
    // Start/End markers depend on path endpoints.
    // If refreshing, we can use App.routeData.routeCoordinates 

    let path = pathOverride;
    if (!path && data.routeCoordinates) {
        path = data.routeCoordinates.map(c => {
            if (Array.isArray(c)) return { lat: c[1], lng: c[0] };
            return { lat: c.latitude || c.lat, lng: c.longitude || c.lon };
        });
    }

    if (path && path.length > 0) {
        // Get observations for start/end points to show temperature
        const startObs = getObservationForLocation(path[0], data.stations || [], data.observations || []);
        const endObs = getObservationForLocation(path[path.length - 1], data.stations || [], data.observations || []);

        // Start marker (green) with temperature
        addEndpointMarker(path[0], 'Start', startObs, '#22c55e');

        // End marker (blue) with temperature
        addEndpointMarker(path[path.length - 1], 'Destination', endObs, '#5e9fff');
    }

    // Station markers with weather icons
    const stations = data.stations || [];
    const observations = data.observations || [];
    const alerts = data.alerts || {};

    stations.forEach(station => {
        const pos = { lat: station.latitude, lng: station.longitude };
        const obs = getLatestObservation(station.id, observations);
        const hasAlert = alerts[station.id]?.length > 0;
        addStationMarker(pos, station, obs, hasAlert);
    });
}

// Helper function to get observation for a location
function getObservationForLocation(location, stations, observations) {
    // Find nearest station to this location
    let nearestStation = null;
    let minDistance = Infinity;

    stations.forEach(station => {
        const distance = haversineKm(
            location.lat, location.lng,
            station.latitude, station.longitude
        );
        if (distance < minDistance && distance < 5) { // Within 5km
            minDistance = distance;
            nearestStation = station;
        }
    });

    if (nearestStation) {
        return getLatestObservation(nearestStation.id, observations);
    }
    return null;
}

function haversineKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

// Create endpoint markers (start/destination) - simple clear temperature display
function addEndpointMarker(position, title, observation, bgColor) {
    const useAdvanced = window.GOOGLE_MAPS_MAP_ID && google.maps.marker?.AdvancedMarkerElement;

    // Get temperature to display - just the number and C
    let tempText = '';
    if (observation && observation.tempC !== null) {
        tempText = `${observation.tempC.toFixed(0)}°C`;
    } else {
        return; // Don't show marker if no temp data
    }

    if (useAdvanced) {
        // Simple, clear temperature text - white and bigger, no color coding
        const markerEl = document.createElement('div');
        markerEl.className = 'custom-endpoint-marker';
        markerEl.innerHTML = `
            <div style="
                font-size: 20px;
                font-weight: 700;
                color: white;
                text-shadow: 2px 2px 6px rgba(0,0,0,0.9), -1px -1px 2px rgba(0,0,0,0.5);
                cursor: pointer;
                white-space: nowrap;
                pointer-events: auto;
            ">${tempText}</div>
        `;

        const marker = new google.maps.marker.AdvancedMarkerElement({
            position,
            map: App.map,
            title: `${title}: ${tempText}`,
            content: markerEl
        });
        App.markers.push(marker);
    } else {
        // Fallback for non-advanced markers
        const marker = new google.maps.Marker({
            position,
            map: App.map,
            title: `${title}: ${tempText}`,
            label: {
                text: tempText,
                color: 'white',
                fontSize: '20px',
                fontWeight: 'bold'
            },
            icon: {
                path: google.maps.SymbolPath.CIRCLE,
                scale: 0, // Hide default icon, use label only
                fillOpacity: 0,
                strokeOpacity: 0
            }
        });
        App.markers.push(marker);
    }
}

// Create weather station markers with weather icons
function addStationMarker(position, station, observation, hasAlert) {
    const useAdvanced = window.GOOGLE_MAPS_MAP_ID && google.maps.marker?.AdvancedMarkerElement;

    // Determine data to show - ALWAYS show wind speed
    let label = '--';
    let subLabel = 'm/s';
    let bgColor = 'rgba(94, 159, 255, 0.85)'; // Soothing blue default
    let borderColor = 'rgba(255,255,255,0.3)';

    // Logic for what to display on the mini-marker - always show wind speed
    if (observation) {
        const windSpeed = observation.windMs || 0;
        const gustSpeed = observation.gustMs || 0;
        const maxWind = Math.max(windSpeed, gustSpeed);

        // Always show wind speed
        label = `${maxWind.toFixed(0)}`;
        subLabel = 'm/s';

        // Progressive color gradient: more wind = stronger color (same blue/purple palette)
        // Base color: rgba(94, 159, 255, 0.85) - light blue
        // Max color: rgba(139, 92, 246, 0.95) - deep purple
        // Interpolate based on wind speed (0-30 m/s range)
        const windRatio = Math.min(maxWind / 30, 1); // Normalize to 0-1

        // Interpolate between blue and purple
        const r1 = 94, g1 = 159, b1 = 255; // Light blue
        const r2 = 139, g2 = 92, b2 = 246;  // Deep purple

        const r = Math.round(r1 + (r2 - r1) * windRatio);
        const g = Math.round(g1 + (g2 - g1) * windRatio);
        const b = Math.round(b1 + (b2 - b1) * windRatio);
        const opacity = 0.75 + (windRatio * 0.2); // 0.75 to 0.95

        bgColor = `rgba(${r}, ${g}, ${b}, ${opacity})`;
        borderColor = `rgba(255,255,255,${0.3 + windRatio * 0.2})`;
    } else {
        // No observation - use default blue
        label = '--';
        subLabel = 'm/s';
    }

    // Always show wind speed, even if there's an alert
    // Alert status is shown in the popup, not on the marker badge

    // Create custom hover box for this station
    const hoverBox = createStationHoverBox(station, observation, hasAlert);

    // Create custom marker element with fixed size
    const markerEl = document.createElement('div');
    markerEl.className = 'custom-station-marker';
    markerEl.style.pointerEvents = 'auto';
    markerEl.style.cursor = 'pointer';
    // Add unique identifier that will persist through cloning
    markerEl.dataset.stationId = station.id || station.name;
    markerEl.dataset.stationName = station.name;
    markerEl.innerHTML = `
        <div style="
            background: ${bgColor};
            backdrop-filter: blur(4px);
            border: 1px solid ${borderColor};
            border-radius: 6px;
            padding: 3px 6px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            width: 32px;
            height: 28px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.2);
            cursor: pointer;
            transition: transform 0.2s;
            box-sizing: border-box;
        ">
            <div style="font-size: 11px; font-weight: 700; color: white; line-height: 1;">${label}</div>
            ${subLabel ? `<div style="font-size: 7px; color: rgba(255,255,255,0.9); margin-top:0px; line-height: 1;">${subLabel}</div>` : ''}
        </div>
        <div style="
            width: 0; 
            height: 0; 
            border-left: 5px solid transparent;
            border-right: 5px solid transparent;
            border-top: 5px solid ${bgColor};
            margin: 0 auto;
        "></div>
    `;

    // Hover effect
    const inner = markerEl.firstElementChild;
    markerEl.addEventListener('mouseenter', () => inner.style.transform = 'scale(1.1)');
    markerEl.addEventListener('mouseleave', () => inner.style.transform = 'scale(1)');

    // Detect if device is likely touch-based (mobile/tablet)
    const isTouch = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);

    // Track hover and pin state
    let hoverTimeout = null;
    let showTimeout = null;
    let isHovering = false;
    let markerRef = null;
    const stationId = station.id || station.name;

    // Save metadata for repositioning
    hoverBox.stationData = station;
    hoverBox.isHoveringInternal = false;

    // Create handlers for custom hover box
    const showHoverBox = () => {
        console.log(`🔍 [DEBUG] showHoverBox triggered for station: ${station.name} (${stationId})`);
        if (!markerRef || !App.map) {
            console.warn('⚠️ [DEBUG] No markerRef or App.map', { markerRef, map: !!App.map });
            return;
        }

        isHovering = true;

        if (hoverTimeout) {
            clearTimeout(hoverTimeout);
            hoverTimeout = null;
        }

        if (showTimeout) clearTimeout(showTimeout);
        showTimeout = setTimeout(() => {
            if (!isHovering) {
                console.log(`ℹ️ [DEBUG] Hovering cancelled for ${stationId}`);
                return;
            }

            console.log(`✨ [DEBUG] Showing hover box for ${stationId}`);

            // Close other non-pinned hover boxes
            App.hoverBoxes.forEach((box, id) => {
                if (id !== stationId && !App.pinnedStations.has(id)) {
                    box.classList.remove('visible');
                    box.isHoveringInternal = false;
                }
            });

            // Position and show hover box
            positionHoverBox(hoverBox, markerRef);
            hoverBox.classList.add('visible');
            showTimeout = null;
        }, 100); // Faster response
    };

    const hideHoverBox = () => {
        isHovering = false;

        if (showTimeout) {
            clearTimeout(showTimeout);
            showTimeout = null;
        }

        if (hoverTimeout) {
            clearTimeout(hoverTimeout);
        }

        hoverTimeout = setTimeout(() => {
            // Only hide if not hovering over the marker AND not hovering over the box internal
            if (!isHovering && !hoverBox.isHoveringInternal && !App.pinnedStations.has(stationId)) {
                hoverBox.classList.remove('visible');
            }
            hoverTimeout = null;
        }, 400); // More generous hide timeout for easier transition
    };

    // Update hoverBox internal hover state
    hoverBox.addEventListener('mouseenter', () => {
        hoverBox.isHoveringInternal = true;
        if (hoverTimeout) clearTimeout(hoverTimeout);
    });

    hoverBox.addEventListener('mouseleave', () => {
        hoverBox.isHoveringInternal = false;
        hideHoverBox();
    });


    const togglePin = () => {
        console.log(`📌 [DEBUG] Toggling pin for ${stationId}`);
        if (App.pinnedStations.has(stationId)) {
            // Unpin
            App.pinnedStations.delete(stationId);
            hoverBox.classList.remove('pinned');
            // Don't hide immediately if still hovering
            if (!isHovering && !hoverBox.isHoveringInternal) {
                hoverBox.classList.remove('visible');
            }
        } else {
            // Pin
            App.pinnedStations.add(stationId);
            hoverBox.classList.add('pinned');
            positionHoverBox(hoverBox, markerRef);
            hoverBox.classList.add('visible');
        }
    };

    if (useAdvanced) {
        const marker = new google.maps.marker.AdvancedMarkerElement({
            position,
            map: App.map,
            title: '',
            content: markerEl,
            zIndex: hasAlert ? 100 : 10
        });

        marker.stationId = stationId;
        markerRef = marker;

        marker.addListener('click', () => togglePin());

        App.markers.push(marker);
    } else {
        const marker = new google.maps.Marker({
            position,
            map: App.map,
            zIndex: hasAlert ? 100 : 10
        });

        marker.stationId = stationId;
        markerRef = marker;

        marker.addListener('click', () => togglePin());
        marker.addListener('mouseover', () => showHoverBox());
        marker.addListener('mouseout', () => hideHoverBox());

        App.markers.push(marker);
    }

    // Direct listeners on markerEl for maximum reliability
    if (markerEl) {
        markerEl.addEventListener('mouseenter', (e) => {
            e.stopPropagation();
            showHoverBox();
        });
        markerEl.addEventListener('mouseleave', (e) => {
            e.stopPropagation();
            hideHoverBox();
        });
        markerEl.addEventListener('click', (e) => {
            e.stopPropagation();
            togglePin();
        });
    }
}

// Single global hover listener that pierces Shadow DOM
function initHoverDelegation() {
    if (!App.map || !App.map.getDiv()) return;
    const mapDiv = App.map.getDiv();

    const handleHover = (e, isOver) => {
        let node = e.target;
        while (node && node !== mapDiv) {
            if (node.classList && node.classList.contains('custom-station-marker')) {
                const id = node.dataset.stationId;
                const marker = App.markers.find(m => m.stationId === id);
                if (marker) {
                    // This is slightly tricky as we need the local handlers
                    // But we can trigger a custom event or just let the direct listeners handle it
                    // For now, let's just make sure markerEl has pointers enabled
                }
                break;
            }
            node = node.parentNode || (node.getRootNode && node.getRootNode().host);
        }
    };

    mapDiv.addEventListener('mouseover', e => handleHover(e, true), true);
    mapDiv.addEventListener('mouseout', e => handleHover(e, false), true);
}


// Global debug access and convenience tools
window.AppDebug = {
    App,
    reposition: repositionAllHoverBoxes,
    // Force show the first available box for testing
    forceShow: () => {
        const firstId = Array.from(App.hoverBoxes.keys())[0];
        if (firstId) {
            console.log(`Force showing box for: ${firstId}`);
            const box = App.hoverBoxes.get(firstId);
            const marker = App.markers.find(m => m.stationId === firstId);
            if (box && marker) {
                positionHoverBox(box, marker);
                box.classList.add('visible');
            }
        }
    },
    status: () => {
        console.table(Array.from(App.hoverBoxes.entries()).map(([id, box]) => ({
            id,
            visible: box.classList.contains('visible'),
            pinned: box.classList.contains('pinned'),
            top: box.style.top,
            left: box.style.left
        })));
    }
};


// Build modern popup HTML for station - dark theme
function buildStationPopup(station, observation, hasAlert) {
    let html = `
        <div style="font-family:-apple-system,BlinkMacSystemFont,sans-serif;padding:12px;min-width:240px;background:rgba(15,15,25,0.95);backdrop-filter:blur(24px);border-radius:32px;border:1px solid rgba(255,255,255,0.1);box-shadow:0 8px 32px rgba(0,0,0,0.5);overflow:hidden;">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:12px;">
                <strong style="font-size:15px;color:#f5f5f7;font-weight:600;">${station.name}</strong>
                ${hasAlert ? '<span style="background:rgba(239,68,68,0.2);color:#ef4444;padding:3px 10px;border-radius:12px;font-size:10px;font-weight:600;border:1px solid rgba(239,68,68,0.3);">⚠️ ALERT</span>' : ''}
            </div>
    `;

    if (observation) {
        html += `<div style="display:grid;grid-template-columns:repeat(2,1fr);gap:8px;margin-bottom:12px;">`;

        if (observation.tempC != null) {
            const tempColor = observation.tempC < 0 ? '#06b6d4' : '#22c55e';
            html += `
                <div style="background:rgba(255,255,255,0.05);padding:10px;border-radius:16px;text-align:center;border:1px solid rgba(255,255,255,0.08);">
                    <div style="font-size:20px;font-weight:700;color:${tempColor};">${observation.tempC.toFixed(1)}°</div>
                    <div style="font-size:10px;color:#a1a1a6;text-transform:uppercase;margin-top:4px;">Temp</div>
                </div>
            `;
        }

        if (observation.windMs != null) {
            const windColor = observation.windMs > 15 ? '#ef4444' : observation.windMs > 8 ? '#fbbf24' : '#22c55e';
            html += `
                <div style="background:rgba(255,255,255,0.05);padding:10px;border-radius:16px;text-align:center;border:1px solid rgba(255,255,255,0.08);">
                    <div style="font-size:20px;font-weight:700;color:${windColor};">${observation.windMs.toFixed(0)}</div>
                    <div style="font-size:10px;color:#a1a1a6;text-transform:uppercase;margin-top:4px;">m/s Wind</div>
                </div>
            `;
        }

        if (observation.gustMs != null && observation.gustMs > observation.windMs) {
            const gustColor = observation.gustMs > 20 ? '#ef4444' : '#fbbf24';
            html += `
                <div style="background:rgba(255,255,255,0.05);padding:10px;border-radius:16px;text-align:center;grid-column:span 2;border:1px solid rgba(255,255,255,0.08);">
                    <div style="font-size:16px;font-weight:700;color:${gustColor};">💨 ${observation.gustMs.toFixed(0)} m/s gusts</div>
                </div>
            `;
        }

        html += `</div>`;
    } else {
        html += `<div style="color:#a1a1a6;font-size:13px;margin-bottom:12px;padding:8px;background:rgba(255,255,255,0.03);border-radius:16px;">No recent data available</div>`;
    }

    // Extract station ID for vedur.is link
    const stationNum = station.id.replace('imo:', '').replace('veg:', '');
    html += `
        <a href="https://vedur.is/vedur/stodvar/?sid=${stationNum}" target="_blank" 
           style="display:block;text-align:center;color:#5e9fff;font-size:13px;text-decoration:none;padding:8px;background:rgba(94,159,255,0.1);border-radius:16px;border:1px solid rgba(94,159,255,0.2);margin-bottom:12px;transition:all 0.2s;font-weight:500;">
            View on vedur.is →
        </a>
    `;

    // Add AI Advice if available for this station
    const adviceText = App.stationAdvice[station.name];
    if (adviceText) {
        html += `
            <div style="margin-top:12px;padding:10px;background:rgba(94,159,255,0.1);border-left:3px solid #5e9fff;border-radius:16px;font-size:12px;color:#f5f5f7;line-height:1.5;">
                <strong style="display:block;margin-bottom:6px;color:#5e9fff;text-transform:uppercase;font-size:10px;letter-spacing:0.05em;">🤖 AI Insight</strong>
                ${adviceText}
            </div>
        `;
    }

    html += `</div>`;

    return html;
}

function getLatestObservation(stationId, observations) {
    const stationObs = observations.filter(o => o.stationId === stationId);
    if (stationObs.length === 0) return null;
    return stationObs.reduce((latest, curr) =>
        new Date(curr.timestamp) > new Date(latest.timestamp) ? curr : latest
    );
}

// =============================================================================
// Custom Hover Box System
// =============================================================================
function createStationHoverBox(station, observation, hasAlert) {
    const stationId = station.id || station.name;

    // Create hover box element
    const hoverBox = document.createElement('div');
    hoverBox.className = 'station-hover-box';
    hoverBox.dataset.stationId = stationId;

    // Build content
    let content = `
        <div class="station-hover-box-content">
            <div class="station-hover-box-header">
                <div class="station-hover-box-title">${station.name}</div>
                ${hasAlert ? '<div class="station-hover-box-alert">⚠️ ALERT</div>' : ''}
            </div>
    `;

    if (observation) {
        content += '<div class="station-hover-box-stats">';

        if (observation.tempC != null) {
            const tempColor = observation.tempC < 0 ? '#06b6d4' : '#22c55e';
            content += `
                <div class="station-hover-box-stat">
                    <div class="station-hover-box-stat-value" style="color:${tempColor};">${observation.tempC.toFixed(1)}°</div>
                    <div class="station-hover-box-stat-label">Temp</div>
                </div>
            `;
        }

        if (observation.windMs != null) {
            const windColor = observation.windMs > 15 ? '#ef4444' : observation.windMs > 8 ? '#fbbf24' : '#22c55e';
            content += `
                <div class="station-hover-box-stat">
                    <div class="station-hover-box-stat-value" style="color:${windColor};">${observation.windMs.toFixed(0)}</div>
                    <div class="station-hover-box-stat-label">m/s Wind</div>
                </div>
            `;
        }

        if (observation.gustMs != null && observation.gustMs > observation.windMs) {
            const gustColor = observation.gustMs > 20 ? '#ef4444' : '#fbbf24';
            content += `
                <div class="station-hover-box-stat" style="grid-column: span 2;">
                    <div class="station-hover-box-stat-value" style="color:${gustColor}; font-size:16px;">💨 ${observation.gustMs.toFixed(0)} m/s gusts</div>
                </div>
            `;
        }

        content += '</div>';
    } else {
        content += '<div style="color:#a1a1a6;font-size:13px;margin-bottom:12px;padding:8px;background:rgba(255,255,255,0.03);border-radius:12px;">No recent data available</div>';
    }

    // Extract station ID for vedur.is link
    const stationNum = station.id.replace('imo:', '').replace('veg:', '');
    content += `
        <a href="https://vedur.is/vedur/stodvar/?sid=${stationNum}" target="_blank" class="station-hover-box-link">
            View on vedur.is →
        </a>
    `;

    // Add AI Advice if available for this station
    const adviceText = App.stationAdvice[station.name];
    if (adviceText) {
        content += `
            <div class="station-hover-box-advice">
                <span class="station-hover-box-advice-title">🤖 AI Insight</span>
                ${adviceText}
            </div>
        `;
    }

    content += '</div>';
    hoverBox.innerHTML = content;

    // Add hover listeners to keep box visible when hovering over it
    hoverBox.addEventListener('mouseenter', () => {
        hoverBox.isHoveringInternal = true;
        hoverBox.classList.add('visible');
    });

    hoverBox.addEventListener('mouseleave', () => {
        hoverBox.isHoveringInternal = false;
        if (!App.pinnedStations.has(stationId)) {
            setTimeout(() => {
                if (!App.pinnedStations.has(stationId) && !hoverBox.isHoveringInternal) {
                    hoverBox.classList.remove('visible');
                }
            }, 300);
        }
    });

    // Add click listener to toggle pin
    hoverBox.addEventListener('click', (e) => {
        // Don't toggle if clicking on a link
        if (e.target.tagName === 'A' || e.target.closest('a')) {
            return;
        }

        if (App.pinnedStations.has(stationId)) {
            App.pinnedStations.delete(stationId);
            hoverBox.classList.remove('pinned');
            hoverBox.classList.remove('visible');
        } else {
            App.pinnedStations.add(stationId);
            hoverBox.classList.add('pinned');
            positionHoverBox(hoverBox, App.markers.find(m => m.stationId === stationId)); // Reposition on pin
            hoverBox.classList.add('visible');
        }
    });

    // Append to body
    document.body.appendChild(hoverBox);

    // Store in map
    App.hoverBoxes.set(stationId, hoverBox);

    return hoverBox;
}

function positionHoverBox(hoverBox, marker) {
    if (!marker || !App.map) return;

    // 1. Try marker element first (Advanced Marker)
    const markerEl = marker.element || marker.content;
    if (markerEl && markerEl.getBoundingClientRect) {
        const rect = markerEl.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
            // Position to the right center of marker
            const offsetX = 20;
            // Center box vertically relative to marker
            const boxHeight = hoverBox.offsetHeight || 250;

            hoverBox.style.left = `${rect.right + offsetX}px`;
            hoverBox.style.top = `${rect.top + (rect.height / 2) - (boxHeight / 2)}px`;

            console.log(`📍 [DEBUG] Positioned hover box via Rect: ${hoverBox.style.left}, ${hoverBox.style.top}`);
            return;
        }
    }

    // 2. Fallback: Convert lat/lng to screen pixels
    let markerPosition;
    if (marker.position) {
        markerPosition = marker.position;
    } else if (marker.getPosition) {
        markerPosition = marker.getPosition();
    } else {
        return;
    }

    const projection = App.map.getProjection();
    if (!projection) return;

    const mapDiv = App.map.getDiv();
    const mapRect = mapDiv.getBoundingClientRect();

    const worldPoint = projection.fromLatLngToPoint(markerPosition);
    if (!worldPoint) return;

    // Use a simple overlay projection if available, or manual calculation
    // This part is the most common point of failure for fixed positioning
    // We'll use the getProjection logic but ensure it's translated to window space
    const scale = Math.pow(2, App.map.getZoom());
    const bounds = App.map.getBounds();
    if (!bounds) return;

    const ne = bounds.getNorthEast();
    const sw = bounds.getSouthWest();
    const nw = new google.maps.LatLng(ne.lat(), sw.lng());
    const nwWorld = projection.fromLatLngToPoint(nw);

    const x = (worldPoint.x - nwWorld.x) * scale;
    const y = (worldPoint.y - nwWorld.y) * scale;

    const offsetX = 50;
    const offsetY = -100;

    hoverBox.style.left = `${mapRect.left + x + offsetX}px`;
    hoverBox.style.top = `${mapRect.top + y + offsetY}px`;

    console.log(`📍 [DEBUG] Positioned hover box via Coord: ${hoverBox.style.left}, ${hoverBox.style.top}`);
}

function repositionAllHoverBoxes() {
    // Reposition all visible or pinned hover boxes
    App.hoverBoxes.forEach((hoverBox, stationId) => {
        if (hoverBox.classList.contains('visible') || hoverBox.classList.contains('pinned')) {
            // Find the corresponding marker
            const marker = App.markers.find(m => m.stationId === stationId);

            if (marker) {
                positionHoverBox(hoverBox, marker);
            }
        }
    });
}



// =============================================================================
// Weather Panel
// =============================================================================
function displayWeatherPanel(data) {
    const panel = document.getElementById('weatherPanel');
    const content = document.getElementById('weatherContent');

    const analysis = analyzeWeather(data);

    let html = `
        <div class="weather-stats">
            <div class="weather-stat ${analysis.wind.status}">
                <span class="weather-stat-icon">💨</span>
                <span class="weather-stat-value">${analysis.wind.value}</span>
                <span class="weather-stat-label">Max Wind</span>
            </div>
            <div class="weather-stat ${analysis.temp.status}">
                <span class="weather-stat-icon">🌡️</span>
                <span class="weather-stat-value">${analysis.temp.value}</span>
                <span class="weather-stat-label">Temp Range</span>
            </div>
            <div class="weather-stat ${analysis.precip.status}">
                <span class="weather-stat-icon">❄️</span>
                <span class="weather-stat-value">${analysis.precip.value}</span>
                <span class="weather-stat-label">Conditions</span>
            </div>
            <div class="weather-stat ${analysis.road.status}">
                <span class="weather-stat-icon">🛣️</span>
                <span class="weather-stat-value">${analysis.road.value}</span>
                <span class="weather-stat-label">Roads</span>
            </div>
        </div>
    `;

    // Station list
    const stations = data.stations || [];
    if (stations.length > 0) {
        // Use details/summary for collapsible dropdown
        html += `<details class="station-details" open>
            <summary>Weather Stations (${stations.length})</summary>
            <div class="station-list">`;

        stations.forEach(station => {
            const obs = getLatestObservation(station.id, data.observations || []);
            const hasAlert = data.alerts?.[station.id]?.length > 0;
            const dotClass = hasAlert ? 'warning' : (obs?.gustMs > 15 ? 'caution' : '');

            let dataStr = 'No data';
            if (obs) {
                const parts = [];
                if (obs.tempC != null) parts.push(`${obs.tempC.toFixed(0)}°C`);
                if (obs.windMs != null) parts.push(`${obs.windMs.toFixed(0)} m/s`);
                dataStr = parts.join(' • ') || 'No data';
            }

            html += `
                <div class="station-item" onclick="focusStation('${station.id}', ${station.latitude}, ${station.longitude})">
                    <div class="station-dot ${dotClass}"></div>
                    <div class="station-info">
                        <div class="station-name">${station.name}</div>
                        <div class="station-data">${dataStr}</div>
                    </div>
                </div>
            `;
        });

        html += `</div></details>`;
    }

    // Road closures section
    const closures = data.closures || [];
    if (closures.length > 0) {
        html += `<details class="station-details closures-details">
            <summary>&#9940; Road Closures (${closures.length})</summary>
            <div class="closures-list">`;
        closures.forEach(c => {
            const title = c.title || '';
            const desc = c.description ? ` — ${c.description}` : '';
            const date = c.published ? `<span class="closure-pub-date">${c.published}</span>` : '';
            html += `<div class="closure-entry">
                <div class="closure-title">${escapeHtml(title)}</div>
                ${desc ? `<div class="closure-desc">${escapeHtml(c.description)}</div>` : ''}
                ${date}
            </div>`;
        });
        html += `</div></details>`;
    }

    content.innerHTML = html;
    panel.classList.remove('hidden');
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function analyzeWeather(data) {
    let maxWind = 0, minTemp = 100, maxTemp = -100;
    let hasIce = false;

    (data.observations || []).forEach(obs => {
        if (obs.windMs) maxWind = Math.max(maxWind, obs.windMs);
        if (obs.gustMs) maxWind = Math.max(maxWind, obs.gustMs);
        if (obs.tempC != null) {
            minTemp = Math.min(minTemp, obs.tempC);
            maxTemp = Math.max(maxTemp, obs.tempC);
        }
    });

    if (minTemp < 2) hasIce = true;

    return {
        wind: {
            status: maxWind > 15 ? 'warning' : maxWind > 8 ? 'caution' : 'ok',
            value: maxWind > 0 ? `${maxWind.toFixed(0)} m/s` : 'Calm'
        },
        temp: {
            status: minTemp < -10 ? 'warning' : minTemp < 0 ? 'caution' : 'ok',
            value: minTemp < 100 ? `${minTemp.toFixed(0)}° to ${maxTemp.toFixed(0)}°` : '--'
        },
        precip: {
            status: hasIce ? 'caution' : 'ok',
            value: hasIce ? 'Ice Risk' : 'Clear'
        },
        road: {
            status: hasIce ? 'caution' : 'ok',
            value: hasIce ? 'Slippery' : 'Clear'
        }
    };
}

// Focus on station from list
window.focusStation = function (id, lat, lng) {
    App.map.panTo({ lat, lng });
    App.map.setZoom(11);

    // Find and click marker
    const marker = App.markers.find(m => m.title === App.routeData?.stations?.find(s => s.id === id)?.name);
    if (marker) {
        google.maps.event.trigger(marker, 'click');
    }
};

// =============================================================================
// Route Summary
// =============================================================================
function displayRouteSummary(data) {
    const summary = document.getElementById('routeSummary');

    if (data.routeInfo) {
        document.getElementById('routeDistance').textContent = Math.round(data.routeInfo.distanceKm);

        const mins = data.routeInfo.durationMinutes;
        const h = Math.floor(mins / 60);
        const m = mins % 60;
        document.getElementById('routeDuration').textContent = h > 0 ? `${h}h ${m}m` : `${mins}m`;
    }

    document.getElementById('stationCount').textContent = data.stations?.length || 0;

    // Weather status
    const analysis = analyzeWeather(data);
    const hasWarning = Object.values(analysis).some(a => a.status === 'warning');
    const hasCaution = Object.values(analysis).some(a => a.status === 'caution');

    const statusEl = document.getElementById('weatherStatus');
    statusEl.innerHTML = ''; // Clear previous content

    if (hasWarning) {
        statusEl.innerHTML = `<span class="status-dot red"></span><span class="summary-value">Warnings</span>`;
        // Attach warning tooltip logic
        attachWarningTooltip(statusEl, data);
    } else if (hasCaution) {
        statusEl.innerHTML = `<span class="status-dot yellow"></span><span class="summary-value">Caution</span>`;
        // Optional: could attach tooltip for cautions too
        attachWarningTooltip(statusEl, data);
    } else {
        statusEl.innerHTML = `<span class="status-dot green"></span><span class="summary-value">All Clear</span>`;
    }

    // Route summary should never be hidden - only minimized
    summary.classList.remove('hidden');
    // Ensure it's visible (remove minimized state if it was minimized)
    // But don't force expand if user minimized it - just ensure it's not hidden
}

function attachWarningTooltip(element, data) {
    // Collect alerts and windy stations
    const alerts = [];
    (data.stations || []).forEach(station => {
        const obs = getLatestObservation(station.id, data.observations || []);
        const stationAlerts = data.alerts?.[station.id] || [];

        if (stationAlerts.length > 0) {
            alerts.push({ name: station.name, reason: 'Alert' });
        } else if (obs && (obs.windMs > 15 || obs.gustMs > 20)) {
            alerts.push({ name: station.name, reason: `Wind ${Math.max(obs.windMs, obs.gustMs).toFixed(0)}m/s` });
        } else if (obs && obs.tempC < -5) {
            alerts.push({ name: station.name, reason: `Temp ${obs.tempC.toFixed(0)}°` });
        }
    });

    if (alerts.length === 0) return;

    // Create tooltip html
    let tooltip = document.getElementById('warningTooltip');
    if (!tooltip) {
        tooltip = document.createElement('div');
        tooltip.id = 'warningTooltip';
        tooltip.className = 'warning-tooltip';
        element.appendChild(tooltip); // Append to status element which is relative/flex
    }

    tooltip.innerHTML = `
        <div class="warning-tooltip-title">Affected Areas</div>
        <div class="warning-list">
            ${alerts.map(a => `<div class="warning-item"><span>📍</span>${a.name} <span style="opacity:0.7">(${a.reason})</span></div>`).join('')}
        </div>
    `;

    element.style.position = 'relative'; // Ensure tooltip positions correctly
    element.style.cursor = 'help';

    // Use properties to avoid stacking listeners on re-render
    element.onmouseenter = () => tooltip.classList.add('visible');
    element.onmouseleave = () => tooltip.classList.remove('visible');
}

// =============================================================================
// AI Advice Panel
// =============================================================================
function displayAdvice(data) {
    const panel = document.getElementById('advicePanel');
    // Clear previous advice map
    App.stationAdvice = {};

    const adviceList = data.advice || [];

    // If no advice, hide panel and return
    if (adviceList.length === 0) {
        panel.classList.add('hidden');
        return;
    }

    // New logic: Check if we can parse advice into station buckets
    // Typical format: "*StationName*: message"
    const generalAdvice = [];

    adviceList.forEach(item => {
        // Regex to find *Name*: at start
        const match = item.match(/^\*+([^*:]+)\*+:?\s*(.*)/) || item.match(/^([^*:]+):\s*(.*)/);
        if (match) {
            const stationName = match[1].trim(); // e.g. "Stehe (Steingrímsfjarðarheiði)"
            const adviceText = match[2].trim();

            // Try to map to known station names in App.weatherStations
            // We might need fuzzy match if the AI name is slightly different, 
            // but for now let's assume exact or substring match.
            // Storing directly by the name found in the prompt which usually matches station.name

            // Clean up name if needed (sometimes prompts adding extra info)
            // But let's trust the AI output mirrors the input names we sent it.
            App.stationAdvice[stationName] = adviceText;

            // Also try matching by just the code if present "CODE (Name)"
            const codeMatch = stationName.match(/^([A-Z]+)\s/);
            if (codeMatch) {
                App.stationAdvice[codeMatch[1]] = adviceText;
            }
        } else {
            generalAdvice.push(item);
        }
    });

    // If we have general advice that didn't fit a station, show it in the panel.
    // MODIFICATION: User explicitly requested to NEVER show the global panel.
    // So even if we have general advice, we log it but do NOT show the panel.
    // FORCE HIDE - User request: never show global panel
    if (generalAdvice.length > 0) {
        console.log("Hidden General Advice:", generalAdvice);
    }
    // panel.classList.add('hidden'); // Ensure it stays hidden
    // The panel is hidden by default. We just ensure we DON'T remove the class.
    // actually, we should ensure it's added just in case
    panel.classList.add('hidden');

    // We can stop here.
    return;

}

// =============================================================================
// Panel Handlers
// =============================================================================
function setupPanelHandlers() {
    // Weather panel - minimize instead of close
    const weatherPanel = document.getElementById('weatherPanel');
    const weatherContent = document.getElementById('weatherContent');
    document.getElementById('toggleWeatherPanel')?.addEventListener('click', () => {
        const isMinimized = weatherContent.style.display === 'none';
        weatherContent.style.display = isMinimized ? 'block' : 'none';
        weatherPanel.classList.toggle('minimized', !isMinimized);
        const btn = document.getElementById('toggleWeatherPanel');
        btn.textContent = isMinimized ? '▼' : '▲';
        btn.classList.toggle('expanded', !isMinimized);
    });

    document.getElementById('toggleAdvice')?.addEventListener('click', (e) => {
        const content = document.getElementById('adviceContent');
        const isHidden = content.style.display === 'none';
        content.style.display = isHidden ? 'block' : 'none';
        e.target.classList.toggle('expanded', isHidden);
    });

    document.getElementById('closeError')?.addEventListener('click', () => {
        document.getElementById('errorToast').classList.add('hidden');
    });

    // Route summary - prevent any hiding, always visible and fixed size
    const routeSummary = document.getElementById('routeSummary');
    if (routeSummary) {
        // Prevent route summary from being hidden or minimized
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
                    if (routeSummary.classList.contains('hidden') || routeSummary.classList.contains('minimized')) {
                        routeSummary.classList.remove('hidden', 'minimized');
                    }
                }
            });
        });
        observer.observe(routeSummary, { attributes: true, attributeFilter: ['class'] });

        // Override classList methods to prevent hiding
        const originalRemove = routeSummary.classList.remove.bind(routeSummary.classList);
        routeSummary.classList.remove = function (...args) {
            const filtered = args.filter(arg => arg !== 'hidden' && arg !== 'minimized');
            return originalRemove(...filtered);
        };

        const originalAdd = routeSummary.classList.add.bind(routeSummary.classList);
        routeSummary.classList.add = function (...args) {
            const filtered = args.filter(arg => arg !== 'hidden' && arg !== 'minimized');
            return originalAdd(...filtered);
        };
    }
}

// =============================================================================
// Loading & Error
// =============================================================================
function showLoading(text = 'Loading...') {
    document.getElementById('loadingText').textContent = text;
    document.getElementById('loadingOverlay').classList.remove('hidden');
}

function hideLoading() {
    document.getElementById('loadingOverlay').classList.add('hidden');
}

function showError(message) {
    document.getElementById('errorMessage').textContent = message;
    document.getElementById('errorToast').classList.remove('hidden');

    setTimeout(() => {
        document.getElementById('errorToast').classList.add('hidden');
    }, 5000);
}
