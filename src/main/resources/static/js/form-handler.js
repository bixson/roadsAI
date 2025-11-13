function initializeForm() {
    const form = document.getElementById('adviceForm');
    const timeInput = document.getElementById('time');
    const submitBtn = document.getElementById('submitBtn');
    const fromSelect = document.getElementById('from');
    const toSelect = document.getElementById('to');
    
    // Set max time to current time + 48 hours (for 1hr increments)
    const now = new Date();
    const maxTime = new Date(now.getTime() + 48 * 60 * 60 * 1000);
    timeInput.max = maxTime.toISOString().slice(0, 16);
    timeInput.min = now.toISOString().slice(0, 16);
    
    // Form validation function
    function validateForm() {
        const from = fromSelect.value;
        const to = toSelect.value;
        const time = timeInput.value;
        
        // validate time, when provided (within 48hrs)
        let timeValid = true;
        if (time) {
            const currentTime = new Date();
            const selectedTime = new Date(time + 'Z');
            const minTime = new Date(currentTime.getTime());
            const maxTime = new Date(currentTime.getTime() + 48 * 60 * 60 * 1000);
            timeValid = selectedTime >= minTime && selectedTime <= maxTime;
        }
        
        const isValid = from && to && from !== to && timeValid;
        submitBtn.disabled = !isValid;
    }
    
    // Validate on input changes
    fromSelect.addEventListener('change', () => {
        const fromValue = fromSelect.value;
        const toValue = toSelect.value;
        
        if (fromValue === toValue) {
            toSelect.value = fromValue === 'RVK' ? 'IFJ' : 'RVK';
        }
        validateForm();
    });
    
    toSelect.addEventListener('change', () => {
        const fromValue = fromSelect.value;
        const toValue = toSelect.value;
        
        if (fromValue === toValue) {
            fromSelect.value = toValue === 'RVK' ? 'IFJ' : 'RVK';
        }
        validateForm();
    });
    
    timeInput.addEventListener('input', validateForm);
    
    // Initial validation
    validateForm();
    
    // Form submission handler
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Disable submit button during request
        submitBtn.disabled = true;
        
        // Hide previous results/errors
        const error = document.getElementById('error');
        const results = document.getElementById('results');
        const loading = document.getElementById('loading');
        
        error.classList.add('hidden');
        results.classList.add('hidden');
        loading.classList.remove('hidden');
        
        // Clear map if it exists
        if (window.LeafletMap) {
            window.LeafletMap.clearMap();
        }
        
        // Get form values
        const from = fromSelect.value;
        const to = toSelect.value;
        const timeLocal = timeInput.value;
        
        // Convert datetime-local input (treated as UTC per label) to ISO-8601 UTC
        // Append 'Z' to indicate UTC, since the form label says "Time (UTC)"
        // If empty, send null (current obs only)
        const forecastTime = timeLocal ? new Date(timeLocal + 'Z').toISOString() : null;
        
        // Prepare request
        const request = {
            from,
            to,
            forecastTime
        };
        
        try {
            const data = await fetchObservations(request);
            displayResults(data);
        } catch (err) {
            showError(`Failed to fetch observations: ${err.message}`);
        } finally {
            loading.classList.add('hidden');
            validateForm(); // Re-enable button (if forms valid)
        }
    });
}

function showError(message) {
    const error = document.getElementById('error');
    error.textContent = message;
    error.classList.remove('hidden');
}

