package dk.ek.roadsai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Official CAP (Common Alerting Protocol) alert from Veður.is
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CapAlert {
    @JsonProperty("severity")
    public String severity; // "Minor", "Moderate", "Severe", "Extreme"
    
    @JsonProperty("eventType")
    public String eventType; // "Wind", "Snow", "Ice"
    
    @JsonProperty("description")
    public String description; // Alert description
    
    @JsonProperty("headline")
    public String headline; // Alert headline
}

