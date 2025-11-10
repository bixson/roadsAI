package dk.ek.roadsai.service;

import dk.ek.roadsai.dto.OpenAiRequest;
import dk.ek.roadsai.dto.OpenAiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * OpenAI API integration
 * sends weather and road data to OpenAI and retrieves driving advice
 *
 */
@Service
public class OpenAiService {
    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.api.timeout:30000}")
    private int timeout;

    // initialize WebClient w. OpenAI base URL and headers
    public OpenAiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Sends system and user prompts to OpenAI, returns exactly 4 advice points
    // Returns fallback messages on API errors or timeouts
    public List<String> ask(String systemPrompt, String userPrompt) {
        try { // Build request with system context and user query
            OpenAiRequest request = new OpenAiRequest();
            request.model = model;
            request.messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            );

            // Call OpenAI API with configured timeout
            OpenAiResponse response = webClient.post()
                    .uri("/chat/completions") // Chat completions endpoint
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey) // Add auth header
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class) // Deserialize response
                    .timeout(Duration.ofMillis(timeout))
                    .block(); // close stream

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                return List.of("No response from AI", "Check API configuration", "Try again later", "Error occurred");
            }

            String content = response.choices.get(0).message.content; // Extract content from first choice
            return parseAdvicePoints(content); // return after parsing method

        } catch (Exception e) {
            System.out.println("OpenAI error: " + e.getMessage());
            return List.of("AI service unavailable", "Network error", "API timeout", "Service error");
        }
    }

    // Parses OpenAI response text into exactly 4 clean advice points
    // Strips numbering, bullets, and formatting characters
    // Pads with generic advice if fewer than 4 points returned
    private List<String> parseAdvicePoints(String content) {
        if (content == null || content.isBlank()) {
            return List.of("Empty response", "No advice available", "Try again", "Error parsing");
        }

        // Split into lines and filter out blank entries
        List<String> lines = Arrays.stream(content.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            // Remove common GPT list formatting: "1. ", "- ", "• ", etc.
            line = line.replaceFirst("^\\d+[.)]\\s*", "");
            line = line.replaceFirst("^[-*•]\\s*", "");
            line = line.replaceFirst("^[-\\s]+", "");

            // Keep only meaningful content (length > 10)
            if (!line.isBlank() && line.length() > 10) {
                cleaned.add(line);
            }
        }
        // Return error messages if parsing failed
        if (cleaned.isEmpty()) {
            return List.of("Could not parse response", "Invalid format", "Try again", "Parse error");
        }
        // Trim to 4 points
        if (cleaned.size() >= 4) {
            return cleaned.subList(0, 4);
        }
        // generic advice if fewer than 4 points
        while (cleaned.size() < 4) {
            cleaned.add("Drive carefully");
        }
        return cleaned;
    }
}
