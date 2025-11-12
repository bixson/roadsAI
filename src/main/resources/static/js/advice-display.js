function displayAdvice(adviceArray) {
    const adviceContent = document.getElementById('adviceContent');
    adviceContent.innerHTML = '';
    
    const chatContainer = document.createElement('div');
    chatContainer.className = 'advice-chat';
    
    const label = document.createElement('div');
    label.className = 'advice-chat-label';
    label.textContent = 'AI chat response:';
    chatContainer.appendChild(label);
    
    // Parse all advice first
    const parsedStations = [];
    adviceArray.forEach((adviceText) => {
        const parsed = parseAdviceText(adviceText);
        if (parsed.stationName) {
            parsedStations.push(parsed);
        }
    });
    
    // Skip if no valid stations
    if (parsedStations.length === 0) {
        const messageBubble = document.createElement('div');
        messageBubble.className = 'advice-message';
        const paragraph = document.createElement('p');
        paragraph.className = 'advice-paragraph';
        paragraph.textContent = adviceArray.join(' ');
        messageBubble.appendChild(paragraph);
        chatContainer.appendChild(messageBubble);
        adviceContent.appendChild(chatContainer);
        return;
    }
    
    // Create unified table container
    const tableContainer = document.createElement('div');
    tableContainer.className = 'advice-table-container';
    
    // Create table header
    const tableHeader = document.createElement('div');
    tableHeader.className = 'advice-table-header';
    
    const headerCells = [
        { icon: null, label: 'Station' },
        { icon: 'images/png/chatprompt/temperature.png', label: 'Temperature' },
        { icon: 'images/png/chatprompt/wind.png', label: 'Wind' },
        { icon: 'images/png/chatprompt/gusts.png', label: 'Gusts' },
        { icon: 'images/png/chatprompt/road-conditions.png', label: 'Road Conditions' }
    ];
    
    headerCells.forEach(cell => {
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
        tableHeader.appendChild(headerCell);
    });
    
    tableContainer.appendChild(tableHeader);
    
    // Create table rows for each station
    parsedStations.forEach(parsed => {
        const tableRow = document.createElement('div');
        tableRow.className = 'advice-table-row';
        
        // Station name cell
        const stationCell = document.createElement('div');
        stationCell.className = 'advice-table-cell advice-table-cell-station';
        const stationNameSpan = document.createElement('span');
        stationNameSpan.className = 'advice-station-name';
        stationNameSpan.textContent = parsed.stationName;
        stationCell.appendChild(stationNameSpan);
        
        // Add official alert badge if present
        if (parsed.officialAlert) {
            const alertBadge = document.createElement('span');
            alertBadge.className = 'advice-official-alert';
            alertBadge.textContent = '⚠️ ' + parsed.officialAlert;
            stationCell.appendChild(alertBadge);
        }
        
        tableRow.appendChild(stationCell);
        
        // Data cells
        const dataValues = [
            parsed.temperature || 'N/A',
            parsed.wind || 'N/A',
            parsed.gusts || 'N/A',
            parsed.roadConditions || 'N/A'
        ];
        
        dataValues.forEach(value => {
            const dataCell = document.createElement('div');
            dataCell.className = 'advice-table-cell';
            dataCell.textContent = value;
            tableRow.appendChild(dataCell);
        });
        
        tableContainer.appendChild(tableRow);
    });
    
    chatContainer.appendChild(tableContainer);
    adviceContent.appendChild(chatContainer);
}

