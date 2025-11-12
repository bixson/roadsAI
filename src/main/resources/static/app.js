// Form handling + API communication
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('adviceForm');
    const loading = document.getElementById('loading');
    const error = document.getElementById('error');
    const results = document.getElementById('results');
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

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Disable submit button during request
        submitBtn.disabled = true;
        
        // Hide previous results/errors
        error.classList.add('hidden');
        results.classList.add('hidden');
        loading.classList.remove('hidden');
        
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
            validateForm(); // Re-enable button if form is still valid
        }
    });
    
    function showError(message) {
        error.textContent = message;
        error.classList.remove('hidden');
    }
    
    function displayResults(data) {
        // Display hazards - extract heading from first element, rest as message
        const hazardsContent = document.getElementById('hazardsContent');
        hazardsContent.innerHTML = '';
        hazardsContent.classList.remove('has-warnings');
        
        // Create message box
        const message = document.createElement('div');
        message.className = 'hazards-message';
        
        if (data.summaryStats && data.summaryStats.hazards && data.summaryStats.hazards.length > 0) {
            const hazards = data.summaryStats.hazards;
            
            // First element is the heading text - put it INSIDE the message box
            const headingText = hazards[0];
            const heading = document.createElement('div');
            heading.className = 'hazards-message-heading';
            heading.innerHTML = '⚠️ ' + headingText;
            message.appendChild(heading);
            
            // Rest of the array is the actual warnings
            if (hazards.length > 1) {
                hazardsContent.classList.add('has-warnings');
                const warnings = hazards.slice(1).join(' ');
                const warningsText = document.createElement('div');
                warningsText.textContent = warnings;
                message.appendChild(warningsText);
            } else {
                const noHazardsText = document.createElement('div');
                noHazardsText.textContent = 'No hazards detected for given route - conditions are within safe limits';
                message.appendChild(noHazardsText);
            }
        } else {
            // Default heading inside message box
            const heading = document.createElement('div');
            heading.className = 'hazards-message-heading';
            heading.innerHTML = '⚠️ Official Weather Warnings (Icelandic Road Safety Office) ⚠️:';
            message.appendChild(heading);
            
            const noHazardsText = document.createElement('div');
            noHazardsText.textContent = 'No hazards detected for given route - conditions are within safe limits';
            message.appendChild(noHazardsText);
        }
        
        hazardsContent.appendChild(message);
        
        // Display advice - unified chat-style response with breaks
        const adviceContent = document.getElementById('adviceContent');
        adviceContent.innerHTML = '';
        
        if (data.advice && data.advice.length > 0) {
            // Create single chat bubble
            const avatar = document.createElement('div');
            avatar.className = 'advice-avatar';
            adviceContent.appendChild(avatar);
            
            const messageBubble = document.createElement('div');
            messageBubble.className = 'advice-message';
            
            // Group advice into segments (every 3 stations = 1 segment)
            const segmentSize = 3;
            const segments = [];
            
            for (let i = 0; i < data.advice.length; i += segmentSize) {
                segments.push(data.advice.slice(i, i + segmentSize));
            }
            
            segments.forEach((segment, segmentIndex) => {
                const segmentDiv = document.createElement('div');
                segmentDiv.className = 'advice-segment';
                
                const header = document.createElement('div');
                header.className = 'advice-segment-header';
                header.textContent = `Segment ${segmentIndex + 1}`;
                segmentDiv.appendChild(header);
                
                const content = document.createElement('div');
                content.className = 'advice-segment-content';
                content.innerHTML = segment.map(advice => `<div>${advice}</div>`).join('');
                segmentDiv.appendChild(content);
                
                messageBubble.appendChild(segmentDiv);
            });
            
            adviceContent.appendChild(messageBubble);
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

