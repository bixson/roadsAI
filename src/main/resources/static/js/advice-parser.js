// Parse advice text to extract road conditions and official alerts
function parseAdviceText(adviceText) {
    const colonIndex = adviceText.indexOf(':');
    const contentText = colonIndex !== -1 ? adviceText.substring(colonIndex + 1).trim() : adviceText;
    
    // Extract official alerts
    let officialAlert = '';
    let roadConditions = contentText;
    const alertMatch = contentText.match(/OFFICIAL ALERT:\s*([^.]+)/i);
    if (alertMatch) {
        officialAlert = alertMatch[1].trim();
        roadConditions = contentText.replace(/OFFICIAL ALERT:\s*[^.]+\.?\s*/i, '').trim();
    }
    
    // Clean up road conditions
    roadConditions = roadConditions.replace(/^[,\s\.]+|[,\s\.]+$/g, '').trim();
    
    return {
        officialAlert: officialAlert || null,
        roadConditions: roadConditions || adviceText
    };
}

