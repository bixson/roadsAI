package dk.ek.roadsai.dto.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

///  openAI response format
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiResponse {
    @JsonProperty("choices")
    public List<Choice> choices;

    @JsonProperty("error")
    public Error error;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        @JsonProperty("message")
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        @JsonProperty("content")
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        @JsonProperty("message")
        public String message;

        @JsonProperty("type")
        public String type;
    }
}

