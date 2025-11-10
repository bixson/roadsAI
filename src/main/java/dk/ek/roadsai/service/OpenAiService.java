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
import java.util.stream.Collectors;

@Service
public class OpenAiService {
    private final WebClient webClient;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;
    
    @Value("${openai.api.timeout:30000}")
    private int timeout;

    public OpenAiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<String> ask(String systemPrompt, String userPrompt) {
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
                return List.of("No response from AI", "Check API configuration", "Try again later", "Error occurred");
            }

            String content = response.choices.get(0).message.content;
            return parseAdvicePoints(content);

        } catch (Exception e) {
            System.out.println("OpenAI error: " + e.getMessage());
            return List.of("AI service unavailable", "Network error", "API timeout", "Service error");
        }
    }

    private List<String> parseAdvicePoints(String content) {
        if (content == null || content.isBlank()) {
            return List.of("Empty response", "No advice available", "Try again", "Error parsing");
        }

        List<String> lines = Arrays.stream(content.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            line = line.replaceFirst("^\\d+[.)]\\s*", "");
            line = line.replaceFirst("^[-*•]\\s*", "");
            line = line.replaceFirst("^[-\\s]+", "");
            
            if (!line.isBlank() && line.length() > 10) {
                cleaned.add(line);
            }
        }

        if (cleaned.isEmpty()) {
            return List.of("Could not parse response", "Invalid format", "Try again", "Parse error");
        }

        if (cleaned.size() >= 4) {
            return cleaned.subList(0, 4);
        }

        while (cleaned.size() < 4) {
            cleaned.add("Drive carefully");
        }
        return cleaned;
    }
}
