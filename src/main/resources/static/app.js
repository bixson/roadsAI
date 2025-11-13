// Initialize the application
async function initialize() {
    // Load Leaflet before initializing
    if (window.LeafletMap) {
        await window.LeafletMap.loadLeaflet();
    }
    
    // Initialize form handling
    initializeForm();
}

// Start initialization when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize);
} else {
    initialize();
}
