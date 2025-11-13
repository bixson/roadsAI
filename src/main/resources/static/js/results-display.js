function displayResults(data) {
    // Convert alerts Map to array format for hazards display
    // Format: [heading, ...alertDescriptions]
    const alertsArray = [];
    if (data.alerts && Object.keys(data.alerts).length > 0) {
        alertsArray.push('Official Weather Warnings (Icelandic Road Safety Office)');
        Object.entries(data.alerts).forEach(([stationId, alerts]) => {
            alerts.forEach(alert => {
                let alertText = '';
                if (alert.headline) alertText += alert.headline;
                if (alert.severity) alertText += ` [${alert.severity}]`;
                if (alert.eventType) alertText += ` (${alert.eventType})`;
                if (alert.description) alertText += `: ${alert.description}`;
                if (alertText) alertsArray.push(alertText);
            });
        });
    }
    displayHazards(alertsArray);
    
    // Create summary object from stations
    const summaryStats = {
        stationsUsed: data.stations ? data.stations.length : 0
    };
    displaySummary(summaryStats);
    
    // Create mapData object from route and stations
    const mapData = {
        route: {
            coordinates: data.route || []
        },
        stations: (data.stations || []).map(station => ({
            id: station.id,
            name: station.name,
            lat: station.latitude,
            lon: station.longitude
        }))
    };
    
    // Initialize advice map
    if (window.LeafletMap && mapData.route.coordinates.length > 0) {
        setTimeout(() => {
            window.LeafletMap.initializeAdviceMap(mapData);
        }, 100);
    }
    
    // Group observations by stationId for easy lookup
    const observationsByStation = {};
    if (data.observations && Array.isArray(data.observations)) {
        data.observations.forEach(obs => {
            if (!observationsByStation[obs.stationId]) {
                observationsByStation[obs.stationId] = [];
            }
            observationsByStation[obs.stationId].push(obs);
        });
    }
    
    // Display advice with observation data
    if (data.advice && data.advice.length > 0) {
        displayAdvice(data.advice, data.stations || [], observationsByStation);
    }
    
    // Show results section
    const results = document.getElementById('results');
    results.classList.remove('hidden');
}
