function displaySummary(summaryStats) {
    const summaryContent = document.getElementById('summaryContent');
    summaryContent.innerHTML = '';
    
    // Create message box
    const message = document.createElement('div');
    message.className = 'summary-message';
    
    // subtle heading
    const heading = document.createElement('div');
    heading.className = 'summary-message-heading';
    
    const title = document.createElement('div');
    title.className = 'summary-title';
    title.textContent = 'Route Summary';
    heading.appendChild(title);
    
    const subtitle = document.createElement('div');
    subtitle.className = 'summary-subtitle';
    subtitle.textContent = 'Weather Data Overview';
    heading.appendChild(subtitle);
    
    message.appendChild(heading);
    
    // content wrapper
    const contentWrapper = document.createElement('div');
    contentWrapper.className = 'summary-message-content';
    
    if (summaryStats && summaryStats.stationsUsed !== undefined) {
        contentWrapper.textContent = `Stations Used: ${summaryStats.stationsUsed}`;
    } else {
        contentWrapper.textContent = 'No summary data available';
    }
    
    message.appendChild(contentWrapper);
    
    // external links section
    const linksSection = document.createElement('div');
    linksSection.className = 'summary-links';
    
    const linksTitle = document.createElement('div');
    linksTitle.className = 'summary-links-title';
    linksTitle.textContent = 'More Detailed Weather Information:';
    linksSection.appendChild(linksTitle);
    
    const linksList = document.createElement('div');
    linksList.className = 'summary-links-list';
    
    // External weather links
    const externalLinks = [
        { url: 'https://vedur.is/', display: 'vedur.is' },
        { url: 'https://umferdin.is/', display: 'umferdin.is' },
        { url: 'https://www.yr.no/', display: 'yr.no' },
        { url: 'https://spakort.vedur.is/kort/spakort/', display: 'spakort.vedur.is' },
        { url: 'https://belgingur.is/', display: 'belgingur.is' },
        { url: 'https://www.windy.com/?64.996,-19.378,7,p:temp', display: 'windy.com' }
    ];
    
    externalLinks.forEach(link => {
        const linkElement = document.createElement('a');
        linkElement.href = link.url;
        linkElement.textContent = link.display;
        linkElement.target = '_blank';
        linkElement.rel = 'noopener noreferrer';
        linkElement.className = 'summary-link';
        linksList.appendChild(linkElement);
    });
    
    linksSection.appendChild(linksList);
    message.appendChild(linksSection);
    
    summaryContent.appendChild(message);
}
