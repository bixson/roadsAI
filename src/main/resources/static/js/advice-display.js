function cleanStationName(name) {
    if (!name) return name;

    // (e.g., "STEHE (Steingrímsfjarðarheiði)" → "Steingrímsfjarðarheiði")
    const parenMatch = name.match(/\(([^)]+)\)/);
    if (parenMatch) {
        return parenMatch[1].trim();
    }

    // Remove common prefixes (e.g., "vedur.is ", "veg:", "imo:")
    let cleaned = name
        .replace(/^(vedur\.is|veg:|imo:)\s*/i, '')
        .trim();

    return cleaned;
}

function displayAdvice(adviceArray, stations, observationsByStation, forecastTime, alerts) {
    const adviceContent = document.getElementById('adviceContent');
    adviceContent.innerHTML = '';

    // 'future' forecast vs current observation
    const isForecast = forecastTime !== null;

    // Match stations with observations and AI advice
    const stationData = [];
    stations.forEach((station, index) => {
        // Get latest observation for station
        const stationObs = observationsByStation[station.id] || [];
        let latestObs = null;
        if (stationObs.length > 0) {
            latestObs = stationObs.reduce((latest, current) =>
                new Date(current.timestamp) > new Date(latest.timestamp) ? current : latest
            );
        }

        // Get AI advice for station (by index)
        const adviceText = adviceArray[index] || '';
        const parsed = parseAdviceText(adviceText);

        stationData.push({
            stationName: cleanStationName(station.name),
            stationId: station.id,
            temperature: latestObs?.tempC != null ? `${latestObs.tempC.toFixed(1)}°C` : 'N/A',
            wind: latestObs?.windMs != null ? `${latestObs.windMs.toFixed(1)} m/s` : 'N/A',
            gusts: latestObs?.gustMs != null ? `${latestObs.gustMs.toFixed(1)} m/s` : 'N/A',
            roadConditions: parsed.roadConditions || 'N/A',
            officialAlert: parsed.officialAlert,
            latestObs: latestObs,
            stationAlerts: alerts?.[station.id] || []
        });
    });

    if (stationData.length === 0) return;

    // Create unified table container
    const tableContainer = document.createElement('div');
    tableContainer.className = 'advice-table-container';

    // Create table header
    const tableHeader = document.createElement('div');
    tableHeader.className = 'advice-table-header';

    const headerCells = [
        {icon: null, label: 'Station'},
        {icon: 'images/png/chatprompt/temperature.png', label: 'Temperature'},
        {icon: 'images/png/chatprompt/wind.png', label: 'Wind'},
        {icon: 'images/png/chatprompt/gusts.png', label: 'Gusts'},
        {icon: 'images/png/chatprompt/road-conditions.png', label: 'Road Conditions'}
    ];

    headerCells.forEach((cell, index) => {
        const headerCell = document.createElement('div');
        headerCell.className = 'advice-table-header-cell';
        if (cell.icon) {
            const icon = document.createElement('img');
            icon.src = cell.icon;
            icon.alt = cell.label;
            icon.className = 'advice-table-icon';
            headerCell.appendChild(icon);
        }
        const labelSpan = document.createElement('span');
        labelSpan.textContent = cell.label;
        headerCell.appendChild(labelSpan);
        
        // Add data type badge to Station header cell
        if (index === 0) {
            const dataBadge = document.createElement('span');
            dataBadge.className = isForecast ? 'data-badge forecast' : 'data-badge current';
            dataBadge.textContent = isForecast ? 'Future Forecast' : 'Current Observations';
            headerCell.appendChild(dataBadge);
        }
        
        tableHeader.appendChild(headerCell);
    });

    tableContainer.appendChild(tableHeader);

    // Create table rows for each station
    stationData.forEach(data => {
        const tableRow = document.createElement('div');
        tableRow.className = 'advice-table-row';

        // Station name cell
        const stationCell = document.createElement('div');
        stationCell.className = 'advice-table-cell advice-table-cell-station';
        const stationNameSpan = document.createElement('span');
        stationNameSpan.className = 'advice-station-name';
        stationNameSpan.textContent = data.stationName;
        stationCell.appendChild(stationNameSpan);

        // Add warning badge inline next to station name
        const hasCap = data.stationAlerts.length > 0;
        const obs = data.latestObs;
        const isCautious = obs && (obs.windMs > 15 || obs.gustMs > 20 || (obs.visibilityM && obs.visibilityM < 1000) || (obs.tempC && obs.tempC < -10));
        if (hasCap || isCautious) {
            const warningBadge = document.createElement('span');
            warningBadge.className = hasCap ? 'data-badge warning' : 'data-badge caution';
            warningBadge.textContent = hasCap ? 'WARNING' : 'CAUTION';
            stationCell.appendChild(warningBadge);
        }

        // official alert badge, if present
        if (data.officialAlert) {
            const alertBadge = document.createElement('span');
            alertBadge.className = 'advice-official-alert';
            alertBadge.textContent = '⚠️ ' + data.officialAlert;
            stationCell.appendChild(alertBadge);
        }

        tableRow.appendChild(stationCell);

        const dataValues = [
            data.temperature,
            data.wind,
            data.gusts,
            data.roadConditions
        ];

        dataValues.forEach(value => {
            const dataCell = document.createElement('div');
            dataCell.className = 'advice-table-cell';
            dataCell.textContent = value;
            tableRow.appendChild(dataCell);
        });

        tableContainer.appendChild(tableRow);
    });

    adviceContent.appendChild(tableContainer);
}

