package AIChallenge.AIChallenge.client;

import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import AIChallenge.AIChallenge.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChatClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final PromptLoader promptLoader;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public AiResponse askChatBot(AiRequest req) {
        String prompt = promptLoader.loadLearningPrompt().formatted(
                nullSafe(req.getResume()),
                String.join(", ", Optional.ofNullable(req.getTechStack()).orElse(List.of())),
                nullSafe(req.getQuestion())
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            Map<String, Object> result = webClient.post()
                    .uri("/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String text = extractText(result);
            return AiResponse.builder().questions(List.of(text)).build();

        } catch (Exception e) {
            return AiResponse.builder()
                    .questions(List.of("AI 응답 실패: " + e.getMessage()))
                    .build();
        }
    }

    private String extractText(Map<String, Object> result) {
        try {
            var candidates = (List<Map<String, Object>>) result.get("candidates");
            var content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "";
        }
    }

    private String nullSafe(String str) {
        return str == null ? "" : str;
    }
}

