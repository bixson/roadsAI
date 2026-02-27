/**
 * Google Maps Module
 * Primary map implementation using Google Maps JavaScript API.
 * Falls back to Leaflet if Google Maps fails to load.
 * 
 * Uses AdvancedMarkerElement when Map ID is configured (no deprecation warnings).
 * Falls back to legacy Marker API when Map ID is not set.
 */

let googleMap = null;
let googleRouteLine = null;
let googleMarkers = [];
let googleInfoWindows = [];

/**
 * Initialize Google Maps when the API loads.
 */
window.initGoogleMaps = function() {
    console.log('Google Maps API loaded');
};

/**
 * Check if Google Maps is available and ready.
 */
function isGoogleMapsAvailable() {
    return typeof google !== 'undefined' && 
           google.maps && 
           google.maps.Map;
}

/**
 * Check if AdvancedMarkerElement is available (requires Map ID).
 */
function canUseAdvancedMarkers() {
    return window.GOOGLE_MAPS_MAP_ID && 
           google.maps.marker && 
           google.maps.marker.AdvancedMarkerElement;
}

/**
 * Clear the Google Map.
 */
function clearGoogleMap() {
    if (googleRouteLine) {
        googleRouteLine.setMap(null);
        googleRouteLine = null;
    }
    googleMarkers.forEach(marker => marker.setMap(null));
    googleMarkers = [];
    googleInfoWindows.forEach(iw => iw.close());
    googleInfoWindows = [];
}

/**
 * Create a colored pin element for AdvancedMarkerElement.
 */
function createPinElement(color, size = 'normal') {
    const scale = size === 'large' ? 1.3 : (size === 'small' ? 0.8 : 1);
    
    const pin = new google.maps.marker.PinElement({
        scale: scale,
        background: color,
        borderColor: '#ffffff',
        glyphColor: '#ffffff'
    });
    
    return pin.element;
}

/**
 * Create a legacy marker icon.
 */
function createLegacyIcon(color, scale = 10) {
    return {
        path: google.maps.SymbolPath.CIRCLE,
        scale: scale,
        fillColor: color,
        fillOpacity: 1,
        strokeColor: '#ffffff',
        strokeWeight: 2
    };
}

/**
 * Initialize Google Map for advice section.
 * @param {Object} mapData - Contains route coordinates and stations
 */
async function initializeGoogleMap(mapData) {
    const mapContainer = document.getElementById('adviceMap');
    if (!mapContainer) {
        console.error('Map container not found');
        return false;
    }

    if (!isGoogleMapsAvailable()) {
        console.warn('Google Maps not available, will use fallback');
        return false;
    }

    // Get route coordinates - backend sends [lon, lat] format
    const routeCoords = mapData.route?.coordinates || mapData.route || [];
    if (routeCoords.length === 0) {
        console.warn('No route coordinates');
        return false;
    }

    console.log(`Initializing Google Map with ${routeCoords.length} route points`);

    try {
        // Clear existing map
        clearGoogleMap();

        // Convert [lon, lat] to Google LatLng format
        const googlePath = routeCoords.map(coord => {
            if (Array.isArray(coord)) {
                return { lat: coord[1], lng: coord[0] };
            }
            return { lat: coord.latitude || coord.lat, lng: coord.longitude || coord.lon };
        });

        // Calculate bounds
        const bounds = new google.maps.LatLngBounds();
        googlePath.forEach(point => bounds.extend(point));

        // Map options - include mapId if available
        const mapOptions = {
            zoom: 7,
            center: bounds.getCenter(),
            mapTypeId: google.maps.MapTypeId.HYBRID,
            mapTypeControl: true,
            mapTypeControlOptions: {
                style: google.maps.MapTypeControlStyle.DROPDOWN_MENU,
                mapTypeIds: ['roadmap', 'satellite', 'hybrid', 'terrain']
            },
            zoomControl: true,
            streetViewControl: false,
            fullscreenControl: true
        };
        
        // Add Map ID if configured (required for AdvancedMarkerElement)
        if (window.GOOGLE_MAPS_MAP_ID) {
            mapOptions.mapId = window.GOOGLE_MAPS_MAP_ID;
            console.log('Using Map ID for AdvancedMarkerElement');
        }

        // Initialize map
        googleMap = new google.maps.Map(mapContainer, mapOptions);

        // Draw route polyline
        googleRouteLine = new google.maps.Polyline({
            path: googlePath,
            geodesic: true,
            strokeColor: '#5e9fff',
            strokeOpacity: 0.9,
            strokeWeight: 5
        });
        googleRouteLine.setMap(googleMap);

        const useAdvanced = canUseAdvancedMarkers();
        console.log(`Using ${useAdvanced ? 'AdvancedMarkerElement' : 'legacy Marker'} API`);

        // Add start marker (green)
        if (googlePath.length > 0) {
            addMarker(googlePath[0], 'Start', '#22c55e', 'large', useAdvanced);
        }

        // Add end marker (red)
        if (googlePath.length > 1) {
            addMarker(googlePath[googlePath.length - 1], 'Destination', '#ef4444', 'large', useAdvanced);
        }

        // Add weather station markers (orange)
        if (mapData.stations && Array.isArray(mapData.stations)) {
            mapData.stations.forEach(station => {
                const position = { lat: station.lat, lng: station.lon };
                
                // Build info window content
                let infoContent = `<div style="color:#333;font-family:system-ui;padding:4px;">
                    <strong>${station.name}</strong><br>`;
                
                if (station.infoUrl) {
                    infoContent += `<a href="${station.infoUrl}" target="_blank" style="color:#1a73e8;">View station data →</a>`;
                } else {
                    const fallbackUrl = station.provider === 'Vegagerðin' 
                        ? 'https://vegasja.vegagerdin.is/'
                        : `https://www.vedur.is/vedur/stodvar/?s=${station.id}`;
                    infoContent += `<a href="${fallbackUrl}" target="_blank" style="color:#1a73e8;">View station data →</a>`;
                }
                infoContent += '</div>';
                
                addMarker(position, station.name, '#ff6b35', 'small', useAdvanced, infoContent);
            });
        }

        // Fit map to route bounds with padding
        googleMap.fitBounds(bounds, { top: 50, right: 50, bottom: 50, left: 50 });

        console.log('Google Map initialized successfully');
        return true;

    } catch (error) {
        console.error('Error initializing Google Map:', error);
        return false;
    }
}

/**
 * Add a marker to the map.
 */
function addMarker(position, title, color, size, useAdvanced, customInfoContent = null) {
    const infoContent = customInfoContent || 
        `<div style="color:#333;font-weight:bold;padding:4px;">${title}</div>`;
    
    const infoWindow = new google.maps.InfoWindow({ content: infoContent });
    googleInfoWindows.push(infoWindow);
    
    if (useAdvanced) {
        // Use AdvancedMarkerElement (no deprecation warnings)
        const marker = new google.maps.marker.AdvancedMarkerElement({
            position: position,
            map: googleMap,
            title: title,
            content: createPinElement(color, size)
        });
        
        marker.addListener('click', () => {
            googleInfoWindows.forEach(iw => iw.close());
            infoWindow.open(googleMap, marker);
        });
        
        googleMarkers.push(marker);
    } else {
        // Use legacy Marker API
        const scale = size === 'large' ? 12 : (size === 'small' ? 7 : 10);
        const marker = new google.maps.Marker({
            position: position,
            map: googleMap,
            title: title,
            icon: createLegacyIcon(color, scale),
            zIndex: size === 'large' ? 1000 : 500
        });
        
        marker.addListener('click', () => {
            googleInfoWindows.forEach(iw => iw.close());
            infoWindow.open(googleMap, marker);
        });
        
        googleMarkers.push(marker);
    }
}

// Export for use in other modules
window.GoogleMap = {
    initialize: initializeGoogleMap,
    clear: clearGoogleMap,
    isAvailable: isGoogleMapsAvailable
};
