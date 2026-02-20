package me.girish.spellingbeeapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public Optional<VocabData> generateVocabQuestion(String word) {
        if (!isEnabled())
            return Optional.empty();

        try {
            String prompt = String.format(
                    "Generate a vocabulary question for the word '%s' for a spelling bee app. " +
                            "Return valid JSON ONLY with this structure: " +
                            "{ \"definition\": \"simple definition\", \"distractors\": [\"wrong def 1\", \"wrong def 2\", \"wrong def 3\"] }",
                    word);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + prompt.replace("\"", "\\\"")
                    + "\" }] }] }";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_URL + apiKey, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

                // Clean markdown if present
                text = text.replace("```json", "").replace("```", "").trim();

                JsonNode json = objectMapper.readTree(text);
                String def = json.get("definition").asText();
                List<String> distractors = new ArrayList<>();
                json.get("distractors").forEach(d -> distractors.add(d.asText()));

                return Optional.of(new VocabData(def, distractors));
            }
        } catch (Exception e) {
            System.out.println("Gemini generation failed for " + word + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public static class VocabData {
        public String definition;
        public List<String> distractors;

        public VocabData(String definition, List<String> distractors) {
            this.definition = definition;
            this.distractors = distractors;
        }
    }
}
