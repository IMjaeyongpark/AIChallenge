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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final PromptLoader promptLoader;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public AiResponse callAiApi(AiRequest request) {
        // 1) 프롬프트 렌더
        String prompt = promptLoader.loadTemplate()
                .replace("{{targetRole}}", request.getTargetRole())
                .replace("{{count}}", String.valueOf(request.getCount()))
                .replace("{{resume}}", request.getResume())
                .replace("{{techStack}}", String.join(", ", request.getTechStack()));


        // 2) JSON 강제 generationConfig
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );

        Map<String, Object> result;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = webClient.post()
                    .uri("/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();
            result = res;
        } catch (WebClientResponseException e) {
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: HTTP %d %s".formatted(e.getRawStatusCode(), e.getStatusText())))
                    .build();
        } catch (Exception e) {
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: " + e.getMessage()))
                    .build();
        }

        // 3) 우선 JSON 파싱, 실패 시 라인 폴백
        List<String> questions = extractQuestions(result);
        if (questions.isEmpty()) {
            String rawText = extractPlainText(result);
            questions = fallbackFromLines(rawText, Math.max(1, request.getCount()));
        }

        questions = questions.stream()
                .map(String::trim).filter(s -> !s.isBlank())
                .limit(Math.max(1, request.getCount()))
                .collect(Collectors.toList());

        if (questions.isEmpty()) {
            questions = List.of("질문을 생성하지 못했습니다.");
        }
        return AiResponse.builder().questions(questions).build();
    }

    // ---------- helpers ----------
    private static String nullSafe(String s) { return s == null ? "" : s; }

    @SuppressWarnings("unchecked")
    private static List<String> extractQuestions(Map<String, Object> result) {
        try {
            if (result == null) return List.of();
            var candidates = (List<Map<String, Object>>) result.get("candidates");
            if (candidates == null || candidates.isEmpty()) return List.of();
            var content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return List.of();
            var parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return List.of();
            var text = (String) parts.get(0).get("text");
            if (text == null || text.isBlank()) return List.of();

            // {"questions":[...]} 기대
            Map<String, Object> json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(text, Map.class);
            var arr = (List<Object>) json.get("questions");
            if (arr == null) return List.of();
            return arr.stream().filter(Objects::nonNull)
                    .map(Object::toString).filter(s -> !s.isBlank()).toList();
        } catch (Exception ignore) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractPlainText(Map<String, Object> result) {
        try {
            var candidates = (List<Map<String, Object>>) result.get("candidates");
            var content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> fallbackFromLines(String rawText, int limit) {
        if (rawText == null) return List.of();
        return rawText.lines()
                .map(s -> s.replaceAll("^[\\s\\-•]*", ""))
                .map(s -> s.replaceAll("^[0-9]+[.)\\-:]\\s*", ""))
                .filter(s -> !s.isBlank())
                .limit(Math.max(1, limit))
                .toList();
    }
}
