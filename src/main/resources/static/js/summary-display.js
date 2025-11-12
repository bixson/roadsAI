function displaySummary(summaryStats) {
    const summaryInfo = document.getElementById('summaryInfo');
    summaryInfo.innerHTML = '';
    
    if (!summaryStats) {
        return;
    }
    
    if (summaryStats.stationsUsed !== undefined) {
        const item = document.createElement('div');
        item.className = 'summary-item';
        item.innerHTML = `<strong>Stations Used</strong>${summaryStats.stationsUsed}`;
        summaryInfo.appendChild(item);
    }
    
    if (summaryStats.window) {
        const item = document.createElement('div');
        item.className = 'summary-item';
        const fromTime = new Date(summaryStats.window.from).toLocaleString();
        const toTime = new Date(summaryStats.window.to).toLocaleString();
        item.innerHTML = `<strong>Time Window</strong>${fromTime} - ${toTime}`;
        summaryInfo.appendChild(item);
    }
}

