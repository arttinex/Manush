package com.manush.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiAiProvider implements AiProvider {

    private final String apiKey;
    private final String modelName;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MockAiProvider fallbackProvider;

    public GeminiAiProvider(
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:gemini-3.8-flash}") String modelName,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.modelName = (modelName != null && !modelName.isBlank()) ? modelName.trim() : "gemini-3.8-flash";
        this.objectMapper = objectMapper;
        
        // Configure timeout for faster responses
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(factory)
                .build();
        this.fallbackProvider = new MockAiProvider();
    }

    @Override
    public AiResponse generate(String prompt) {
        if (apiKey.isBlank()) {
            log.warn("Gemini API key is missing. Falling back to local assistant.");
            return fallbackProvider.generate(prompt);
        }

        try {
            String systemInstruction = "You are Manush AI, an intelligent community assistant for Bangladesh. "
                    + "You fluently understand English, Bengali (বাংলা), and Banglish. "
                    + "You assist users with: Blood Bank donation and blood requests, finding or posting local help requests "
                    + "(plumbing, electrician, emergency, tutor, food sharing), local job openings, and earning helping points (+20 for completing help, +50 for blood donation). "
                    + "Keep your answers warm, concise, and helpful. Use markdown formatting when appropriate.";

            String combinedPrompt = systemInstruction + "\n\nUser Question: " + prompt;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", combinedPrompt)))
                    )
            );

            String uri = String.format("/models/%s:generateContent", modelName);

            String responseJson = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseJson != null) {
                JsonNode root = objectMapper.readTree(responseJson);
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode parts = firstCandidate.path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        String reply = parts.get(0).path("text").asText("");
                        int tokens = root.path("usageMetadata").path("totalTokenCount").asInt(prompt.length() / 4 + 30);
                        return new AiResponse(reply, tokens);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate response from Gemini API: {}. Using fallback provider.", e.getMessage());
        }

        return fallbackProvider.generate(prompt);
    }

    @Override
    public HelpExtraction extractHelpRequest(String prompt) {
        if (apiKey.isBlank()) {
            return fallbackProvider.extractHelpRequest(prompt);
        }

        try {
            String extractionPrompt = "You are an NLP extractor for a Bangladesh community assistance platform named Manush.\n"
                    + "The user provided the following natural language request (in English, Bangla, or Banglish):\n"
                    + "\"" + prompt + "\"\n\n"
                    + "Extract the details and return ONLY a valid JSON object without markdown fences, with these exact keys:\n"
                    + "{\n"
                    + "  \"category\": \"one of: PLUMBER, ELECTRICIAN, BLOOD_DONATION, FOOD_SHARING, EMERGENCY_HELP, TUTOR, CLEANING, OTHER\",\n"
                    + "  \"location\": \"detected city/area in Bangladesh, e.g. Uttara, Dhanmondi, Mirpur, Chittagong, or Dhaka if unspecified\",\n"
                    + "  \"urgency\": \"one of: HIGH, NORMAL, LOW\",\n"
                    + "  \"date\": \"e.g. TODAY, ASAP, TOMORROW, or SPECIFIC_DATE\",\n"
                    + "  \"summary\": \"concise 1-sentence summary of what is needed\"\n"
                    + "}\n"
                    + "Output raw JSON ONLY:";

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", extractionPrompt)))
                    )
            );

            String uri = String.format("/models/%s:generateContent", modelName);

            String responseJson = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseJson != null) {
                JsonNode root = objectMapper.readTree(responseJson);
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String rawText = candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
                    // Strip any accidental markdown formatting e.g. ```json ... ```
                    String cleanJson = rawText.replaceAll("(?s)```[a-zA-Z]*\\s*", "").replaceAll("```", "").trim();

                    JsonNode parsed = objectMapper.readTree(cleanJson);
                    return new HelpExtraction(
                            parsed.path("category").asText("OTHER").toUpperCase(Locale.ROOT),
                            parsed.path("location").asText("Dhaka"),
                            parsed.path("urgency").asText("NORMAL").toUpperCase(Locale.ROOT),
                            parsed.path("date").asText("TODAY"),
                            parsed.path("summary").asText(prompt)
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract help request via Gemini: {}. Using heuristic fallback.", e.getMessage());
        }

        return fallbackProvider.extractHelpRequest(prompt);
    }
}
