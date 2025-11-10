package dk.ek.roadsai.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenAiService {
    public List<String> ask(String systemPrompt, String userPrompt) {
        // TODO: Implement OpenAI API call
        // for now, return a placeholder response:
        return List.of("This is a placeholder response from OpenAI.");
    }
}
