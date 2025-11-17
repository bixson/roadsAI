// Store forecastTime globally to pass to display functions
let currentForecastTime = null;

function initializeForm() {
    const form = document.getElementById('adviceForm');
    const dateSelect = document.getElementById('date');
    const timeSelect = document.getElementById('time');
    const submitBtn = document.getElementById('submitBtn');
    const fromSelect = document.getElementById('from');
    const toSelect = document.getElementById('to');

    function initializeDateTimePickers() {
        const dateInput = document.getElementById('dateInput');
        const calendarPopup = document.getElementById('calendarPopup');
        let selectedDateStr = '';
        // Yr.no API caps at 10 days forecast
        const FORECAST_DAYS = 10;
        const MAX_TIME_MS = FORECAST_DAYS * 24 * 60 * 60 * 1000; // 10 days in ms
        
        function getTodayUTC() {
            const today = new Date();
            today.setUTCHours(0, 0, 0, 0);
            return today;
        }
        
        function buildCalendar() {
            const now = new Date();
            const today = getTodayUTC();
            const todayStr = today.toISOString().slice(0, 10);
            const maxTime = new Date(now.getTime() + MAX_TIME_MS);
            
            // Calculate first day of month and start of calendar grid
            const currentMonth = new Date(today);
            const year = currentMonth.getUTCFullYear();
            const month = currentMonth.getUTCMonth();
            const firstDayOfMonth = new Date(Date.UTC(year, month, 1));
            const startDate = new Date(firstDayOfMonth);
            const dayOfWeek = firstDayOfMonth.getUTCDay();
            startDate.setUTCDate(startDate.getUTCDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1)); // Adjust to Monday
            
            // Build calendar HTML
            const monthName = currentMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
            const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
            const weekdaysHTML = weekdays.map(day => `<div>${day}</div>`).join('');
            
            calendarPopup.innerHTML = `
                <div class="calendar-header"><span>${monthName}</span></div>
                <div class="calendar-weekdays">${weekdaysHTML}</div>
                <div class="calendar-grid"></div>
            `;
            
            const grid = calendarPopup.querySelector('.calendar-grid');
            
            // Build 42 days (6 weeks)
            let currentDate = new Date(startDate);
            for (let i = 0; i < 42; i++) {
                const dateStr = currentDate.toISOString().slice(0, 10);
                const isCurrentMonth = currentDate.getUTCMonth() === month;
                const isInRange = currentDate >= today && new Date(dateStr + 'T23:59:59Z') <= maxTime;
                const isSelectable = isCurrentMonth && isInRange;
                
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'calendar-day';
                btn.textContent = currentDate.getUTCDate();
                
                if (!isCurrentMonth || !isSelectable) {
                    btn.classList.add('disabled');
                }
                if (dateStr === todayStr) {
                    btn.classList.add('today');
                }
                if (dateStr === selectedDateStr) {
                    btn.classList.add('selected');
                }
                
                if (isSelectable) {
                    btn.onclick = () => {
                        selectedDateStr = dateStr;
                        dateSelect.value = dateStr;
                        const formattedDate = new Date(dateStr + 'T00:00:00Z').toLocaleDateString('en-US', { 
                            month: 'short', 
                            day: 'numeric', 
                            year: 'numeric' 
                        });
                        dateInput.value = formattedDate;
                        calendarPopup.classList.add('hidden');
                        const firstTime = updateTimeOptions();
                        // auto-select first available time as fallback
                        if (firstTime) {
                            timeSelect.value = firstTime;
                        }
                        validateForm();
                    };
                }
                
                grid.appendChild(btn);
                currentDate.setUTCDate(currentDate.getUTCDate() + 1);
            }
        }
        // Update time options based on selected date
        function updateTimeOptions() {
            const selectedDate = dateSelect.value;
            timeSelect.innerHTML = '<option value="">Time</option>';
            
            if (!selectedDate) return null;
            
            const now = new Date();
            const maxTime = new Date(now.getTime() + MAX_TIME_MS);
            const todayStr = getTodayUTC().toISOString().slice(0, 10);
            const isToday = selectedDate === todayStr;
            
            let startHour = 0;
            let startMinute = 0;
            
            if (isToday) {
                const currentHour = now.getUTCHours();
                const currentMinute = now.getUTCMinutes();
                const roundedMinute = Math.floor(currentMinute / 15) * 15;
                
                // Allow times within last 15 minutes
                if (currentMinute - roundedMinute <= 15) {
                    startHour = currentHour;
                    startMinute = roundedMinute;
                } else {
                    startHour = currentHour;
                    startMinute = roundedMinute + 15;
                    if (startMinute >= 60) {
                        startMinute = 0;
                        startHour++;
                    }
                }
            }
            
            let firstAvailableTime = null;
            
            // Generate time options (15-minute intervals)
            for (let hour = startHour; hour < 24; hour++) {
                const startMinForHour = (hour === startHour) ? startMinute : 0;
                
                for (let minute = startMinForHour; minute < 60; minute += 15) {
                    const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`; // HH:MM format
                    const timeDate = new Date(`${selectedDate}T${timeStr}:00Z`); // UTC time
                    
                    if (timeDate > maxTime) break;
                    
                    timeSelect.innerHTML += `<option value="${timeStr}">${timeStr}</option>`;
                    
                    // Store first available time
                    if (firstAvailableTime === null) {
                        firstAvailableTime = timeStr;
                    }
                }
            }
            
            return firstAvailableTime;
        }
        
        dateInput.onclick = () => {
            calendarPopup.classList.remove('hidden');
            buildCalendar();
        };
        
        document.addEventListener('click', (e) => {
            const dateWrapper = dateInput.closest('.datetime-date-wrapper');
            if (!dateWrapper?.contains(e.target) && !calendarPopup.contains(e.target)) {
                calendarPopup.classList.add('hidden');
            }
        });
        
        timeSelect.addEventListener('change', validateForm);
        timeSelect.addEventListener('focus', updateTimeOptions);
        timeSelect.addEventListener('mousedown', updateTimeOptions);
        timeSelect.addEventListener('click', updateTimeOptions);
    }
    
    initializeDateTimePickers();
    
    // Form validation function
    function validateForm() {
        const from = fromSelect.value;
        const to = toSelect.value;
        const date = dateSelect.value;
        const time = timeSelect.value;
        
        // If date/time provided, both must be selected
        const timeValid = (!date && !time) || (date && time);
        
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
    
    timeSelect.addEventListener('change', validateForm);
    
    // Initial validation
    validateForm();
    
    // Form submission handler
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Disable submit button during request
        submitBtn.disabled = true;
        
        // Hide results/errors
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
        const date = dateSelect.value;
        const time = timeSelect.value;
        
        // Convert date and time to ISO-8601 UTC (If empty, send null)
        currentForecastTime = (date && time) ? new Date(`${date}T${time}:00Z`).toISOString() : null;
        
        // Prepare request
        const request = {
            from,
            to,
            forecastTime: currentForecastTime
        };
        
        try {
            const data = await fetchObservations(request);
            displayResults(data, currentForecastTime);
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

