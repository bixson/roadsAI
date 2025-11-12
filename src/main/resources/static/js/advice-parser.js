function parseAdviceText(adviceText) {
    const colonIndex = adviceText.indexOf(':');
    let stationName = colonIndex !== -1 
        ? adviceText.substring(0, colonIndex).trim()
        : '';
    
    // Clean station name
    const parenMatch = stationName.match(/\(([^)]+)\)/);
    if (parenMatch) {
        stationName = parenMatch[1];
    }
    stationName = stationName
        .replace(/\b(imo|ved):\s*-?\s*\w+\s*/gi, '')
        .replace(/^[A-Z]+\s+/i, '')
        .trim();
    
    const contentText = colonIndex !== -1 
        ? adviceText.substring(colonIndex + 1).trim()
        : adviceText;
    
    // Extract official alerts
    let officialAlert = '';
    let remainingText = contentText;
    const alertMatch = contentText.match(/OFFICIAL ALERT:\s*([^.]+)/i);
    if (alertMatch) {
        officialAlert = alertMatch[1].trim();
        remainingText = contentText.replace(/OFFICIAL ALERT:\s*[^.]+\.?\s*/i, '').trim();
    }
    
    // Extract temperature - matches "Temperature X.X°C" or "Temperature X°C"
    // Also handle cases where temp is mentioned in road conditions like "Temperature -1.0°C, Normal conditions..."
    let temperature = '';
    const tempMatch = remainingText.match(/Temperature\s+([\d.-]+°C)/i);
    if (tempMatch) {
        temperature = tempMatch[1].trim();
        // Remove temperature from remaining text, including the comma/period after it
        remainingText = remainingText.replace(/Temperature\s+[\d.-]+°C[,\s\.]*/i, '').trim();
    }
    
    // Extract wind - matches "wind X.X m/s" or "calm"
    let wind = '';
    if (remainingText.toLowerCase().includes('calm')) {
        wind = 'calm';
        remainingText = remainingText.replace(/calm[,\s\.]*/i, '').trim();
    } else {
        const windMatch = remainingText.match(/wind\s+([\d.]+(?:\s+m\/s)?)/i);
        if (windMatch) {
            wind = windMatch[1].trim() + (windMatch[1].includes('m/s') ? '' : ' m/s');
            remainingText = remainingText.replace(/wind\s+[\d.]+(?:\s+m\/s)?[,\s\.]*/i, '').trim();
        }
    }
    
    // Extract gusts - matches "gusts X.X m/s"
    let gusts = '';
    const gustsMatch = remainingText.match(/gusts\s+([\d.]+(?:\s+m\/s)?)/i);
    if (gustsMatch) {
        gusts = gustsMatch[1].trim() + (gustsMatch[1].includes('m/s') ? '' : ' m/s');
        remainingText = remainingText.replace(/gusts\s+[\d.]+(?:\s+m\/s)?[,\s\.]*/i, '').trim();
    }
    
    // Road conditions and remaining advice
    // After extracting temperature, wind, and gusts, remaining text is typically road conditions
    // Also check if temperature was mentioned in road conditions text (like "Temperature -1.0°C, Normal conditions...")
    let roadConditions = '';
    if (remainingText) {
        // Clean up any leading/trailing punctuation artifacts
        roadConditions = remainingText.replace(/^[,\s\.]+|[,\s\.]+$/g, '').trim();
    }
    
    // If temperature wasn't found earlier but is in the original text, extract it
    if (!temperature) {
        const tempInContent = contentText.match(/Temperature\s+([\d.-]+°C)/i);
        if (tempInContent) {
            temperature = tempInContent[1].trim();
            // Remove temperature from road conditions if it's there
            roadConditions = roadConditions.replace(/Temperature\s+[\d.-]+°C[,\s\.]*/i, '').trim();
        }
    }
    
    // Clean up road conditions - remove any remaining temperature mentions
    if (roadConditions) {
        roadConditions = roadConditions.replace(/^[,\s\.]+|[,\s\.]+$/g, '').trim();
    }
    
    return {
        stationName,
        officialAlert,
        temperature,
        wind,
        gusts,
        roadConditions
    };
}

