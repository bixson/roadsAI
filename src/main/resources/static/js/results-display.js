function displayResults(data) {
    // Display hazards
    const hazards = data.summaryStats?.hazards || [];
    displayHazards(hazards);
    
    // Display summary in right box
    displaySummary(data.summaryStats);
    
    // Initialize advice map only
    if (window.LeafletMap && data.mapData) {
        setTimeout(() => {
            window.LeafletMap.initializeAdviceMap(data.mapData);
        }, 100);
    }
    
    // Display advice
    if (data.advice && data.advice.length > 0) {
        displayAdvice(data.advice);
    }
    
    // Show results section
    const results = document.getElementById('results');
    results.classList.remove('hidden');
}
