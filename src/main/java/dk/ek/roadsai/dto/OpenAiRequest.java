package dk.ek.roadsai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiRequest {
    @JsonProperty("model")
    public String model;
    
    @JsonProperty("messages")
    public List<Map<String, String>> messages;
    
    @JsonProperty("temperature")
    public Double temperature = 0.7;
    
    @JsonProperty("max_tokens")
    public Integer maxTokens = 200;
}

