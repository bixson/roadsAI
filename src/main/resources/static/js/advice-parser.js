// Parse advice text to extract road conditions
function parseAdviceText(adviceText) {
    const colonIndex = adviceText.indexOf(':');
    const contentText = colonIndex !== -1 ? adviceText.substring(colonIndex + 1).trim() : adviceText; // Get text after first colon
    
    // Clean up road conditions (remove leading/trailing punctuation)
    const roadConditions = contentText.replace(/^[,\s\.]+|[,\s\.]+$/g, '').trim();
    
    return {
        roadConditions: roadConditions || adviceText
    };
}

