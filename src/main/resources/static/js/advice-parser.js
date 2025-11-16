// Parse advice text to extract road conditions and official alerts
function parseAdviceText(adviceText) {
    const colonIndex = adviceText.indexOf(':');
    const contentText = colonIndex !== -1 ? adviceText.substring(colonIndex + 1).trim() : adviceText; // Get text after first colon
    
    // Extract official alerts
    let officialAlert = '';
    let roadConditions = contentText;
    const alertMatch = contentText.match(/OFFICIAL ALERT:\s*([^.]+)/i); // Match "OFFICIAL ALERT: <text>."
    if (alertMatch) {
        officialAlert = alertMatch[1].trim(); // Extract alert text
        roadConditions = contentText.replace(/OFFICIAL ALERT:\s*[^.]+\.?\s*/i, '').trim(); // Remove alert from road conditions
    }
    
    // Clean up road conditions
    roadConditions = roadConditions.replace(/^[,\s\.]+|[,\s\.]+$/g, '').trim();
    
    return {
        officialAlert: officialAlert || null,
        roadConditions: roadConditions || adviceText
    };
}

