function displayResults(data) {
    // Display hazards
    const hazards = data.summaryStats?.hazards || [];
    displayHazards(hazards);
    
    // Initialize advice map
    if (window.LeafletMap && data.mapData) {
        setTimeout(() => {
            window.LeafletMap.initializeAdviceMap(data.mapData);
        }, 100);
    }
    
    // Display advice
    if (data.advice && data.advice.length > 0) {
        displayAdvice(data.advice);
    }
    
    // Display summary
    displaySummary(data.summaryStats);
    
    // Show results section
    const results = document.getElementById('results');
    results.classList.remove('hidden');
    
    // Initialize Leaflet map - ensure Leaflet is loaded and container is visible
    if (window.LeafletMap) {
        setTimeout(() => {
            window.LeafletMap.initializeMap(data.mapData);
        }, 100);
    }
}

