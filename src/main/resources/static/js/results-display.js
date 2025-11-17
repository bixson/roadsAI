function displayResults(data, forecastTime) {
    // Convert alerts Map to array format for hazards display
    const alertsArray = Object.values(data.alerts || {})
        .flatMap(alerts => alerts.map(alert => {
            // Build alert text from available fields
            const parts = [];
            if (alert.headline) parts.push(alert.headline);
            if (alert.severity) parts.push(`[${alert.severity}]`);
            if (alert.eventType) parts.push(`(${alert.eventType})`);
            if (alert.description) parts.push(`: ${alert.description}`);
            return parts.join(' ');
        }))
        .filter(text => text);
    displayHazards(alertsArray);
    
    displaySummary({ stationsUsed: data.stations?.length ?? 0 });
    
    const mapData = {
        route: { coordinates: data.route ?? [] },
        stations: (data.stations || []).map(station => ({
            id: station.id,
            name: station.name,
            lat: station.latitude,
            lon: station.longitude
        }))
    };
    
    // Initialize advice map -- performance optimization
    if (window.LeafletMap && mapData.route.coordinates.length > 0) {
        setTimeout(async () => {
            // Load Leaflet library only when map is actually needed
            if (typeof L === 'undefined') {
                await window.LeafletMap.loadLeaflet();
            }
            window.LeafletMap.initializeAdviceMap(mapData);
        }, 100);
    }
    
    // Group observations by stationId for easy lookup
    const observationsByStation = (data.observations || []).reduce((acc, obs) => { // acc= object building, obs= current obs
        if (!acc[obs.stationId]) acc[obs.stationId] = [];
        acc[obs.stationId].push(obs);
        return acc;
    }, {});
    
    // Display advice with observation data and forecastTime for badge
    if (data.advice?.length > 0) {
        displayAdvice(data.advice, data.stations || [], observationsByStation, forecastTime, data.alerts);
    }
    
    document.getElementById('results').classList.remove('hidden');
}
