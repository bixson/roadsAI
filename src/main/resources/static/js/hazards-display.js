function displayHazards(hazards) {
    const hazardsContent = document.getElementById('hazardsContent');
    hazardsContent.innerHTML = '';
    hazardsContent.classList.remove('has-warnings');
    
    const message = document.createElement('div');
    message.className = 'hazards-message';
    
    const heading = document.createElement('div');
    heading.className = 'hazards-message-heading';
    
    // Determine heading text
    let primaryText = 'Official Weather Warnings';
    let secondaryText = '(ICELANDIC METEOROLOGICAL OFFICE)';
    
    if (hazards && hazards.length > 0) {
        const headingText = hazards[0];
        const parenIndex = headingText.indexOf('(');
        if (parenIndex !== -1) {
            primaryText = headingText.substring(0, parenIndex).trim();
            secondaryText = headingText.substring(parenIndex).trim();
        } else {
            primaryText = headingText;
            secondaryText = '';
        }
    }
    
    // Create heading HTML
    heading.innerHTML = `
        <div class="heading-line">
            <span class="warning-icon">⚠️</span>
            <span class="heading-line-primary">${primaryText}</span>
            <span class="warning-icon">⚠️</span>
        </div>
        ${secondaryText ? `<div class="heading-line-secondary">${secondaryText}</div>` : ''}
    `;
    
    message.appendChild(heading);
    
    // Add content
    const contentWrapper = document.createElement('div');
    contentWrapper.className = 'hazards-message-content';
    
    if (hazards && hazards.length > 1) {
        hazardsContent.classList.add('has-warnings');
        contentWrapper.innerHTML = hazards.slice(1).join('<br><br>');
    } else {
        contentWrapper.textContent = 'No hazards detected for given route - conditions are within safe limits';
    }
    
    message.appendChild(contentWrapper);
    hazardsContent.appendChild(message);
}

