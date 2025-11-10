package dk.ek.roadsai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * OpenAI API request DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiRequest {
    @JsonProperty("model")
    public String model;

    @JsonProperty("messages")
    public List<Map<String, String>> messages;
    // temp 0.0-1.0 more creative response with higher values
    @JsonProperty("temperature")
    public Double temperature = 0.7;

    @JsonProperty("max_tokens")
    public Integer maxTokens = 600;
}

