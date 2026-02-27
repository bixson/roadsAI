package dk.ek.roadsai.service.ai;

import dk.ek.roadsai.dto.openai.OpenAiRequest;
import dk.ek.roadsai.dto.openai.OpenAiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


/// OpenAI API integration for observation-based driving advice.
// sends prompts to OpenAI and parses response into advice points
@Service
public class ObservationAiService {
    private static final Logger log = LoggerFactory.getLogger(ObservationAiService.class);
    private final WebClient webClient;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.api.timeout:30000}")
    private int timeout;

    public ObservationAiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    /**
     * Check if OpenAI API is configured.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // asks OpenAI for driving advice based on prompts
    // returns list of advice points, or fallback messages on error
    public List<String> ask(String systemPrompt, String userPrompt, int expectedCount) {
        // Check if API key is configured
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured! Set OPENAI_API_KEY environment variable.");
            return generateFallback(expectedCount);
        }
        
        log.info("Calling OpenAI API with model: {}, key starts with: {}...", 
                model, apiKey.substring(0, Math.min(8, apiKey.length())));
        
        try {
            OpenAiRequest request = new OpenAiRequest();
            request.model = model;
            request.messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            );

            OpenAiResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                log.warn("OpenAI returned empty response");
                return generateFallback(expectedCount);
            }

            String content = response.choices.getFirst().message.content;
            if (content == null || content.isBlank()) {
                log.warn("OpenAI returned empty content");
                return generateFallback(expectedCount);
            }
            
            log.info("OpenAI response received successfully ({} chars)", content.length());
            return parseAdvicePoints(content, expectedCount);

        } catch (Exception e) {
            log.error("OpenAI API call failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return generateFallback(expectedCount);
        }
    }

    // parses AI response into clean advice points
    private List<String> parseAdvicePoints(String content, int expectedCount) {
        List<String> lines = Arrays.stream(content.split("\n")) // split AI response into lines
                .map(String::trim) // trim whitespace
                .filter(line -> !line.isBlank()) // remove empty lines
                .toList();

        List<String> cleaned = new ArrayList<>();
        List<String> genericPhrases = List.of("drive carefully", "be careful", "stay safe", "take care");

        for (String line : lines) {
            // Remove common formatting
            line = line.replaceFirst("^\\d+[.)]\\s*", "");
            line = line.replaceFirst("^[-*•]\\s*", "");
            line = line.replaceFirst("^[-\\s]+", "");

            // Skip generic phrases
            String lowerLine = line.toLowerCase();
            boolean isGeneric = genericPhrases.stream().anyMatch(lowerLine::contains); // check if line contains generic phrases
            if (isGeneric && line.length() < 30) {
                continue;
            }

            if (!line.isBlank() && line.length() > 10) {
                cleaned.add(line);
            }
        }

        if (cleaned.isEmpty()) {
            return generateFallback(expectedCount);
        }

        // Trim to expected count
        if (cleaned.size() >= expectedCount) {
            return cleaned.subList(0, expectedCount);
        }

        // Pad if needed (fill missing points with generic advice)
        while (cleaned.size() < expectedCount) {
            cleaned.add("Monitor weather conditions along the route and adjust speed accordingly");
        }
        return cleaned;
    }

    private List<String> generateFallback(int count) {
        List<String> fallback = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            fallback.add("AI service unavailable - please review weather observations manually");
        }
        return fallback;
    }
}

