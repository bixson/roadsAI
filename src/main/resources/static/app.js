// Form handling + API communication
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('adviceForm');
    const loading = document.getElementById('loading');
    const error = document.getElementById('error');
    const results = document.getElementById('results');
    const timeInput = document.getElementById('time');

    // Set default time to current UTC time
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    timeInput.value = now.toISOString().slice(0, 16);

    // Handle route direction switching
    const fromSelect = document.getElementById('from');
    const toSelect = document.getElementById('to');

    fromSelect.addEventListener('change', () => {
        const fromValue = fromSelect.value;
        const toValue = toSelect.value;
        
        if (fromValue === toValue) {
            // Switch to the other option
            toSelect.value = fromValue === 'RVK' ? 'IFJ' : 'RVK';
        }
    });

    toSelect.addEventListener('change', () => {
        const fromValue = fromSelect.value;
        const toValue = toSelect.value;
        
        if (fromValue === toValue) {
            // Switch to the other option
            fromSelect.value = toValue === 'RVK' ? 'IFJ' : 'RVK';
        }
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Hide previous results/errors
        error.classList.add('hidden');
        results.classList.add('hidden');
        loading.classList.remove('hidden');
        
        // Get form values
        const from = fromSelect.value;
        const to = toSelect.value;
        const mode = document.getElementById('mode').value;
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
            const response = await fetch('/api/advice', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(request)
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            displayResults(data);
            
        } catch (err) {
            showError(`Failed to fetch advice: ${err.message}`);
        } finally {
            loading.classList.add('hidden');
        }
    });
    
    function showError(message) {
        error.textContent = message;
        error.classList.remove('hidden');
    }
    
    function displayResults(data) {
        // Display hazards
        const hazardsList = document.getElementById('hazardsList');
        hazardsList.innerHTML = '';
        
        if (data.summaryStats && data.summaryStats.hazards) {
            data.summaryStats.hazards.forEach(hazard => {
                const li = document.createElement('li');
                li.textContent = hazard;
                hazardsList.appendChild(li);
            });
        }
        
        // Display advice
        const adviceList = document.getElementById('adviceList');
        adviceList.innerHTML = '';
        
        if (data.advice && data.advice.length > 0) {
            data.advice.forEach(advice => {
                const li = document.createElement('li');
                li.textContent = advice;
                adviceList.appendChild(li);
            });
        }
        
        // Display summary
        const summaryInfo = document.getElementById('summaryInfo');
        summaryInfo.innerHTML = '';
        
        if (data.summaryStats) {
            const stats = data.summaryStats;
            
            if (stats.stationsUsed !== undefined) {
                const item = document.createElement('div');
                item.className = 'summary-item';
                item.innerHTML = `<strong>Stations Used</strong>${stats.stationsUsed}`;
                summaryInfo.appendChild(item);
            }
            
            if (stats.window) {
                const item = document.createElement('div');
                item.className = 'summary-item';
                const fromTime = new Date(stats.window.from).toLocaleString();
                const toTime = new Date(stats.window.to).toLocaleString();
                item.innerHTML = `<strong>Time Window</strong>${fromTime} - ${toTime}`;
                summaryInfo.appendChild(item);
            }
        }
        
        results.classList.remove('hidden');
    }
});

