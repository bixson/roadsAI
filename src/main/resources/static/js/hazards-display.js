function displayHazards(hazards) {
    const hazardsContent = document.getElementById('hazardsContent');
    hazardsContent.innerHTML = '';
    hazardsContent.classList.remove('has-warnings');
    
    // Create message box
    const message = document.createElement('div');
    message.className = 'hazards-message';
    
    if (hazards && hazards.length > 0) {
        // First element is the heading text - put it INSIDE the message box
        const headingText = hazards[0];
        const heading = document.createElement('div');
        heading.className = 'hazards-message-heading';
        
        // Parse heading text - split at "(" if present
        let primaryText = headingText;
        let secondaryText = '';
        const parenIndex = headingText.indexOf('(');
        if (parenIndex !== -1) {
            primaryText = headingText.substring(0, parenIndex).trim();
            secondaryText = headingText.substring(parenIndex).trim();
        }
        
        // Create primary line with icons
        const primaryLine = document.createElement('div');
        primaryLine.className = 'heading-line';
        
        const icon1 = document.createElement('span');
        icon1.className = 'warning-icon';
        icon1.textContent = '⚠️';
        const primaryTextSpan = document.createElement('span');
        primaryTextSpan.className = 'heading-line-primary';
        primaryTextSpan.textContent = primaryText;
        const icon2 = document.createElement('span');
        icon2.className = 'warning-icon';
        icon2.textContent = '⚠️';
        
        primaryLine.appendChild(icon1);
        primaryLine.appendChild(primaryTextSpan);
        primaryLine.appendChild(icon2);
        heading.appendChild(primaryLine);
        
        // Add secondary line if exists
        if (secondaryText) {
            const secondaryLine = document.createElement('div');
            secondaryLine.className = 'heading-line-secondary';
            secondaryLine.textContent = secondaryText;
            heading.appendChild(secondaryLine);
        }
        
        message.appendChild(heading);
        
        // Rest of the array is the actual warnings
        if (hazards.length > 1) {
            hazardsContent.classList.add('has-warnings');
            const contentWrapper = document.createElement('div');
            contentWrapper.className = 'hazards-message-content';
            contentWrapper.textContent = hazards.slice(1).join(' ');
            message.appendChild(contentWrapper);
        } else {
            const contentWrapper = document.createElement('div');
            contentWrapper.className = 'hazards-message-content';
            contentWrapper.textContent = 'No hazards detected for given route - conditions are within safe limits';
            message.appendChild(contentWrapper);
        }
    } else {
        // Default heading inside message box
        const heading = document.createElement('div');
        heading.className = 'hazards-message-heading';
        
        // Create primary line with icons
        const primaryLine = document.createElement('div');
        primaryLine.className = 'heading-line';
        
        const icon1 = document.createElement('span');
        icon1.className = 'warning-icon';
        icon1.textContent = '⚠️';
        const primaryTextSpan = document.createElement('span');
        primaryTextSpan.className = 'heading-line-primary';
        primaryTextSpan.textContent = 'Official Weather Warnings';
        const icon2 = document.createElement('span');
        icon2.className = 'warning-icon';
        icon2.textContent = '⚠️';
        
        primaryLine.appendChild(icon1);
        primaryLine.appendChild(primaryTextSpan);
        primaryLine.appendChild(icon2);
        heading.appendChild(primaryLine);
        
        // Add secondary line
        const secondaryLine = document.createElement('div');
        secondaryLine.className = 'heading-line-secondary';
        secondaryLine.textContent = '(Icelandic Road Safety Office)';
        heading.appendChild(secondaryLine);
        
        message.appendChild(heading);
        
        const contentWrapper = document.createElement('div');
        contentWrapper.className = 'hazards-message-content';
        contentWrapper.textContent = 'No hazards detected for given route - conditions are within safe limits';
        message.appendChild(contentWrapper);
    }
    
    hazardsContent.appendChild(message);
}

