/**
 * Leaflet Map Module
 * Handles all Leaflet map initialization, routing, and visualization
 */
// Load Leaflet library dynamically
function loadLeaflet() {
    return new Promise((resolve, reject) => {
        // Check if Leaflet is already loaded
        if (typeof L !== 'undefined') {
            resolve();
            return;
        }
        
        const script = document.createElement('script');
        script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
        script.integrity = 'sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=';
        script.crossOrigin = '';
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Failed to load Leaflet library'));
        document.head.appendChild(script);
    });
}

// Map state for advice section  
let adviceMap = null;
let adviceRouteLayer = null;
let adviceMapMarkers = [];

/**
 * Get road-following route from OSRM
 */
async function getRoadRoute(waypoints) {
    // OSRM route service - uses free demo server
    // Format: lon,lat;lon,lat;...
    const coordinates = waypoints.map(coord => `${coord[0]},${coord[1]}`).join(';');
    const url = `https://router.project-osrm.org/route/v1/driving/${coordinates}?overview=full&geometries=geojson`;
    
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error(`OSRM API error: ${response.status}`);
    }
    
    const data = await response.json();
    
    if (data.code !== 'Ok' || !data.routes || data.routes.length === 0) {
        throw new Error(`OSRM route error: ${data.code || 'Unknown'}`);
    }
    
    // Extract geometry from first route
    const geometry = data.routes[0].geometry;
    if (geometry && geometry.coordinates) {
        // Convert from [lon, lat] to [lat, lon] for Leaflet
        return geometry.coordinates.map(coord => [coord[1], coord[0]]);
    }
    
    throw new Error('No geometry in OSRM response');
}

/**
 * Get road route with retry logic
 */
async function getRoadRouteWithRetry(waypoints, maxRetries = 3) {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            const route = await getRoadRoute(waypoints);
            if (route && route.length > 0) {
                return route;
            }
        } catch (error) {
            console.warn(`OSRM attempt ${attempt}/${maxRetries} failed:`, error.message);
            if (attempt < maxRetries) {
                // Wait before retry (exponential backoff)
                await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
            } else {
                console.error('All OSRM retry attempts failed');
            }
        }
    }
    return null;
}

/**
 * Clear the advice map
 */
function clearMap() {
    if (adviceMap) {
        adviceMap.remove();
        adviceMap = null;
        adviceRouteLayer = null;
        adviceMapMarkers = [];
    }
}

/**
 * Highlight a station marker on the advice map
 */
function highlightStation(stationIndex) {
    if (!adviceMap || !adviceMapMarkers || stationIndex < 0 || stationIndex >= adviceMapMarkers.length) {
        return;
    }
    
    // Reset all markers
    adviceMapMarkers.forEach((marker, idx) => {
        const normalIcon = L.divIcon({
            className: 'station-marker',
            html: `<div class="station-marker-inner"><span class="station-number">${idx + 1}</span></div>`,
            iconSize: [24, 24],
            iconAnchor: [12, 12]
        });
        marker.setIcon(normalIcon);
    });
    
    // Highlight selected marker
    const selectedMarker = adviceMapMarkers[stationIndex];
    if (selectedMarker) {
        const highlightedIcon = L.divIcon({
            className: 'station-marker station-marker-highlighted',
            html: `<div class="station-marker-inner"><span class="station-number">${stationIndex + 1}</span></div>`,
            iconSize: [32, 32],
            iconAnchor: [16, 16]
        });
        selectedMarker.setIcon(highlightedIcon);
        
        // Pan to marker if needed
        const bounds = adviceMap.getBounds();
        if (!bounds.contains(selectedMarker.getLatLng())) {
            adviceMap.setView(selectedMarker.getLatLng(), adviceMap.getZoom(), { animate: true, duration: 0.5 });
        } else {
            // Just open popup
            selectedMarker.openPopup();
        }
    }
}

/**
 * Initialize map for advice section
 */
async function initializeAdviceMap(mapData) {
    // Ensure Leaflet is loaded
    if (typeof L === 'undefined') {
        console.error('Leaflet not loaded');
        return;
    }
    
    const mapContainer = document.getElementById('adviceMap');
    if (!mapContainer) {
        console.error('Advice map container not found');
        return;
    }
    
    // Clear existing map if present
    if (adviceMap) {
        adviceMap.remove();
        adviceMap = null;
        adviceRouteLayer = null;
        adviceMapMarkers = [];
    }
    
    if (!mapData || !mapData.route || !mapData.stations) {
        console.warn('Missing map data');
        return;
    }
    
    // Get route coordinates (waypoints) - backend sends [lon, lat] format
    const routeCoords = mapData.route.coordinates || [];
    if (routeCoords.length === 0) {
        return;
    }
    
    // Convert coordinates from [lon, lat] to [lat, lon] for Leaflet display
    const leafletRouteCoords = routeCoords.map(coord => [coord[1], coord[0]]);
    
    // Initialize map to show all of Iceland
    try {
        adviceMap = L.map('adviceMap', {
            zoomControl: true,
            attributionControl: true
        }).setView([64.8, -19.0], 6); // Center of Iceland, zoomed out to show whole country
        
        // Add satellite tile layer (Esri World Imagery)
        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            attribution: '&copy; <a href="https://www.esri.com/">Esri</a>',
            maxZoom: 19
        }).addTo(adviceMap);
        
        // Wait for map to be ready
        adviceMap.whenReady(async () => {
            // Get road-following route using OSRM - ALWAYS use OSRM, retry if needed
            const roadRoute = await getRoadRouteWithRetry(routeCoords);
            
            if (roadRoute && roadRoute.length > 0) {
                // Use road-following route
                adviceRouteLayer = L.polyline(roadRoute, {
                    color: '#5e9fff',
                    weight: 4,
                    opacity: 0.8,
                    smoothFactor: 1
                }).addTo(adviceMap);
            } else {
                console.error('Failed to get OSRM route after retries');
                // Only use direct polyline as absolute last resort
                adviceRouteLayer = L.polyline(leafletRouteCoords, {
                    color: '#5e9fff',
                    weight: 4,
                    opacity: 0.8,
                    smoothFactor: 1
                }).addTo(adviceMap);
            }
            
            // Add station markers with enhanced styling
            adviceMapMarkers = [];
            if (mapData.stations && Array.isArray(mapData.stations)) {
                mapData.stations.forEach((station, index) => {
                    const marker = L.marker([station.lat, station.lon], {
                        icon: L.divIcon({
                            className: 'station-marker',
                            html: `<div class="station-marker-inner"><span class="station-number">${index + 1}</span></div>`,
                            iconSize: [24, 24],
                            iconAnchor: [12, 12]
                        })
                    }).addTo(adviceMap);
                    
                    // Add popup with station name
                    marker.bindPopup(`<strong>${station.name}</strong><br>${station.id}`);
                    
                    // Add click handler to highlight card
                    marker.on('click', () => {
                        const event = new CustomEvent('stationMarkerClick', { detail: { index } });
                        document.dispatchEvent(event);
                    });
                    
                    adviceMapMarkers.push(marker);
                });
            }
            
            // Keep map showing all of Iceland (don't auto-fit to route)
            // This ensures users see the full country context
        });
    } catch (error) {
        console.error('Error initializing advice map:', error);
    }
}

// Export public API
window.LeafletMap = {
    loadLeaflet,
    initializeAdviceMap,
    clearMap,
    highlightStation
};

