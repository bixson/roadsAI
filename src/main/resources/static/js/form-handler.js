function initializeForm() {
    const form = document.getElementById('adviceForm');
    const timeInput = document.getElementById('time');
    const submitBtn = document.getElementById('submitBtn');
    const fromSelect = document.getElementById('from');
    const toSelect = document.getElementById('to');
    
    // Set default time to current UTC time
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    timeInput.value = now.toISOString().slice(0, 16);
    
    // Form validation function
    function validateForm() {
        const from = fromSelect.value;
        const to = toSelect.value;
        const mode = document.querySelector('input[name="mode"]:checked');
        const time = timeInput.value;
        
        const isValid = from && to && mode && time && from !== to;
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
    
    // Validate on radio button change
    document.querySelectorAll('input[name="mode"]').forEach(radio => {
        radio.addEventListener('change', validateForm);
    });
    
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
        const mode = document.querySelector('input[name="mode"]:checked').value;
        const timeLocal = timeInput.value;
        
        // Convert local datetime to ISO-8601 UTC
        const timeIso = new Date(timeLocal).toISOString();
        
        // Prepare request
        const request = {
            from,
            to,
            mode,
            timeIso
        };
        
        try {
            const data = await fetchAdvice(request);
            displayResults(data);
        } catch (err) {
            showError(`Failed to fetch advice: ${err.message}`);
        } finally {
            loading.classList.add('hidden');
            validateForm(); // Re-enable button if form is still valid
        }
    });
}

function showError(message) {
    const error = document.getElementById('error');
    error.textContent = message;
    error.classList.remove('hidden');
}

