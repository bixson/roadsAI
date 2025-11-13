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
    
    // Extract temperature - "Temperature X.X°C", "temperatures at X.X°C", "temp X.X°C"
    let temperature = '';
    const tempPatterns = [
        /(?:temperature|temp|temperatures)\s+(?:at\s+)?([\d.-]+°C)/i,
        /([\d.-]+°C).*?(?:temperature|temp)/i
    ];
    for (const pattern of tempPatterns) {
        const tempMatch = remainingText.match(pattern);
        if (tempMatch) {
            temperature = tempMatch[1].trim();
            remainingText = remainingText.replace(pattern, '').trim();
            break;
        }
    }
    
    // Extract wind - "wind X.X m/s", "winds (X.X m/s)", "winds X.X m/s", or "calm"
    let wind = '';
    if (remainingText.toLowerCase().includes('calm')) {
        wind = 'calm';
        remainingText = remainingText.replace(/calm[,\s\.]*/i, '').trim();
    } else {
        const windPatterns = [
            /(?:wind|winds)\s*\(?\s*([\d.]+)\s*m\/s\s*\)?/i,
            /(?:wind|winds)\s+([\d.]+)(?:\s+m\/s)?/i,
            /([\d.]+)\s*m\/s.*?(?:wind|winds)/i
        ];
        for (const pattern of windPatterns) {
            const windMatch = remainingText.match(pattern);
            if (windMatch) {
                wind = windMatch[1].trim() + ' m/s';
                remainingText = remainingText.replace(pattern, '').trim();
                break;
            }
        }
    }
    
    // Extract gusts - "gusts X.X m/s", "gusts of X.X m/s", "gusts (X.X m/s)"
    let gusts = '';
    const gustsPatterns = [
        /gusts\s*(?:of\s*)?\(?\s*([\d.]+)\s*m\/s\s*\)?/i,
        /gusts\s+([\d.]+)(?:\s+m\/s)?/i,
        /([\d.]+)\s*m\/s.*?gusts/i
    ];
    for (const pattern of gustsPatterns) {
        const gustsMatch = remainingText.match(pattern);
        if (gustsMatch) {
            gusts = gustsMatch[1].trim() + ' m/s';
            remainingText = remainingText.replace(pattern, '').trim();
            break;
        }
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
        for (const pattern of tempPatterns) {
            const tempInContent = contentText.match(pattern);
            if (tempInContent) {
                temperature = tempInContent[1].trim();
                // Remove temperature from road conditions if it's there
                roadConditions = roadConditions.replace(pattern, '').trim();
                break;
            }
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

