/**
 * Loads a script dynamically and returns a Promise
 */
function loadScript(src) {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = src;
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
    });
}

/**
 * Loads all required JavaScript modules in order
 */
async function loadModules() {
    const modules = [
        'js/advice-parser.js',
        'js/advice-display.js',
        'js/hazards-display.js',
        'js/summary-display.js',
        'js/results-display.js',
        'js/api-client.js',
        'js/form-handler.js'
    ];
    
    for (const module of modules) {
        await loadScript(module);
    }
}

/**
 * Initialize the application after all modules are loaded
 */
async function initialize() {
    // Load Leaflet before initializing
    if (window.LeafletMap) {
        await window.LeafletMap.loadLeaflet();
    }
    
    // Load all application modules
    await loadModules();
    
    // Initialize form handling
    initializeForm();
}

// Start initialization when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize);
} else {
    initialize();
}
