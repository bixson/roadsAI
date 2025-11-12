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

// Map state
let map = null;
let routeLayer = null;
let stationMarkers = [];

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
 * Initialize Leaflet map with route and stations
 * @param {Object} mapData - Map data containing route and stations
 */
async function initializeMap(mapData) {
    // Ensure Leaflet is loaded
    if (typeof L === 'undefined') {
        console.error('Leaflet not loaded');
        return;
    }
    
    const mapContainer = document.getElementById('hazardsMap');
    if (!mapContainer) {
        console.error('Map container not found');
        return;
    }
    
    // Clear existing map if present
    if (map) {
        map.remove();
        map = null;
        routeLayer = null;
        stationMarkers = [];
    }
    
    if (!mapData || !mapData.route || !mapData.stations) {
        console.warn('Missing map data');
        return;
    }
    
    // Get route coordinates (waypoints)
    const routeCoords = mapData.route.coordinates || [];
    if (routeCoords.length === 0) {
        console.warn('No route coordinates');
        return;
    }
    
    // Convert coordinates from [lon, lat] to [lat, lon] for Leaflet
    const leafletRouteCoords = routeCoords.map(coord => [coord[1], coord[0]]);
    
    // Initialize map centered on first route point
    const centerLat = leafletRouteCoords[0][0];
    const centerLon = leafletRouteCoords[0][1];
    
    try {
        map = L.map('hazardsMap', {
            zoomControl: true,
            attributionControl: true
        }).setView([centerLat, centerLon], 8);
        
        // Add satellite tile layer (Esri World Imagery)
        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            attribution: '&copy; <a href="https://www.esri.com/">Esri</a>',
            maxZoom: 19
        }).addTo(map);
        
        // Wait for map to be ready
        map.whenReady(async () => {
            // Get road-following route using OSRM - ALWAYS use OSRM, retry if needed
            const roadRoute = await getRoadRouteWithRetry(routeCoords);
            
            if (roadRoute && roadRoute.length > 0) {
                // Use road-following route
                routeLayer = L.polyline(roadRoute, {
                    color: '#5e9fff',
                    weight: 4,
                    opacity: 0.8,
                    smoothFactor: 1
                }).addTo(map);
            } else {
                console.error('Failed to get OSRM route after retries');
                // Only use direct polyline as absolute last resort
                routeLayer = L.polyline(leafletRouteCoords, {
                    color: '#5e9fff',
                    weight: 4,
                    opacity: 0.8,
                    smoothFactor: 1
                }).addTo(map);
            }
            
            // Add station markers
            stationMarkers = [];
            if (mapData.stations && Array.isArray(mapData.stations)) {
                mapData.stations.forEach(station => {
                    const marker = L.marker([station.lat, station.lon], {
                        icon: L.divIcon({
                            className: 'station-marker',
                            html: '<div class="station-marker-inner"></div>',
                            iconSize: [12, 12],
                            iconAnchor: [6, 6]
                        })
                    }).addTo(map);
                    
                    // Add popup with station name
                    marker.bindPopup(`<strong>${station.name}</strong><br>${station.id}`);
                    stationMarkers.push(marker);
                });
            }
            
            // Fit map bounds to show route and stations
            if (routeLayer) {
                const bounds = routeLayer.getBounds();
                if (stationMarkers.length > 0) {
                    const group = new L.featureGroup([routeLayer, ...stationMarkers]);
                    map.fitBounds(group.getBounds().pad(0.1));
                } else {
                    map.fitBounds(bounds.pad(0.1));
                }
            }
        });
    } catch (error) {
        console.error('Error initializing map:', error);
    }
}

/**
 * Clear the map
 */
function clearMap() {
    if (map) {
        map.remove();
        map = null;
        routeLayer = null;
        stationMarkers = [];
    }
}

// Export public API
window.LeafletMap = {
    loadLeaflet,
    initializeMap,
    clearMap
};

