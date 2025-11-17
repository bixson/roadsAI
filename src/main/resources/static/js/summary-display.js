function displaySummary(summaryStats) {
    const summaryContent = document.getElementById('summaryContent');
    summaryContent.innerHTML = '';

    // Create message container
    const message = document.createElement('div');
    message.className = 'summary-message';
    
    // create heading
    const heading = document.createElement('div');
    heading.className = 'summary-message-heading';
    heading.innerHTML = `
        <div class="summary-title">Route Summary</div>
        <div class="summary-subtitle">Weather Data Overview</div>
    `;
    message.appendChild(heading);
    
    // Create content
    const contentWrapper = document.createElement('div');
    contentWrapper.className = 'summary-message-content';
    contentWrapper.textContent = summaryStats?.stationsUsed !== undefined  // check for undefined
        ? `Stations Used: ${summaryStats.stationsUsed}`  // display stations used
        : 'No summary data available'; // if none, fallback msg
    message.appendChild(contentWrapper);
    
    // Create external links section
    const linksSection = document.createElement('div');
    linksSection.className = 'summary-links';
    linksSection.innerHTML = `
        <div class="summary-links-title">More Detailed Weather Information:</div>
        <div class="summary-links-list">
            ${[
                { url: 'https://vedur.is/', display: 'vedur.is' },
                { url: 'https://umferdin.is/', display: 'umferdin.is' },
                { url: 'https://www.yr.no/', display: 'yr.no' },
                { url: 'https://spakort.vedur.is/kort/spakort/', display: 'spakort.vedur.is' },
                { url: 'https://belgingur.is/', display: 'belgingur.is' },
                { url: 'https://www.windy.com/?64.996,-19.378,7,p:temp', display: 'windy.com' }
            ].map(link => `<a href="${link.url}" target="_blank" rel="noopener noreferrer" class="summary-link">${link.display}</a>`).join('')}
        </div>
    `;
    message.appendChild(linksSection);
    
    summaryContent.appendChild(message);
}
