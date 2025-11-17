function displayHazards(hazards) {
    const hazardsContent = document.getElementById('hazardsContent');
    hazardsContent.innerHTML = '';
    hazardsContent.classList.remove('has-warnings');
    
    const message = document.createElement('div');
    message.className = 'hazards-message';
    
    const heading = document.createElement('div');
    heading.className = 'hazards-message-heading';
    heading.innerHTML = `
        <div class="heading-line">
            <span class="warning-icon">⚠️</span>
            <span class="heading-line-primary">Official Weather Warnings</span>
            <span class="warning-icon">⚠️</span>
        </div>
        <div class="heading-line-secondary">(ICELANDIC METEOROLOGICAL OFFICE)</div>
    `;
    message.appendChild(heading);
    
    // Add content
    const contentWrapper = document.createElement('div');
    contentWrapper.className = 'hazards-message-content';
    
    if (hazards && hazards.length > 0) {
        hazardsContent.classList.add('has-warnings');
        contentWrapper.innerHTML = hazards.join('<br><br>');
    } else {
        contentWrapper.textContent = 'No hazards detected for given route - conditions are within safe limits';
    }
    
    message.appendChild(contentWrapper);
    hazardsContent.appendChild(message);
}

