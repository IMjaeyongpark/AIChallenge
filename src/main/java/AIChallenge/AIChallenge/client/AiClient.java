package AIChallenge.AIChallenge.client;

import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    // 필요 시 외부에서 bean 주입으로 바꿔도 됨
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public AiResponse callAiApi(AiRequest request) {
        // --- 1) 프롬프트(구조화 + 출력 JSON 강제 지시) ---
        String prompt = """
                당신은 시니어 %s 면접관입니다.

                [목표]
                - 아래 지원자 정보를 바탕으로 '개인 맞춤형' 면접 질문 %d개 생성.

                [지원자 정보]
                - 이력서 요약:
                %s
                - 기술 스택:
                %s

                [규칙]
                - 각 질문은 한 문장, 한국어.
                - 지나치게 일반적인 질문 금지(지원자 정보와 연결).
                - 정확히 %d개 생성.

                [출력 형식(JSON)]
                다음 JSON 스키마로만 출력:
                {
                  "questions": ["질문1", "질문2", "..."]
                }
                기타 텍스트/설명 금지.
                """.formatted(
                nullSafe(request.getTargetRole()),
                Math.max(1, request.getCount()),
                nullSafe(request.getResume()),
                String.join(", ", Optional.ofNullable(request.getTechStack()).orElse(List.of())),
                Math.max(1, request.getCount())
        );

        // --- 2) 요청 바디(JSON 강제) ---
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );

        Map<String, Object> result;
        try {
            // --- 3) 호출 + 타임아웃/에러 처리 ---
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
            // HTTP 에러 (4xx/5xx)
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: HTTP %d %s".formatted(e.getRawStatusCode(), e.getStatusText())))
                    .build();
        } catch (Exception e) {
            // 타임아웃/네트워크 등
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: " + e.getMessage()))
                    .build();
        }

        // --- 4) 응답 파싱(우선 JSON, 실패 시 라인 폴백) ---
        List<String> questions = extractQuestions(result);
        if (questions.isEmpty()) {
            String rawText = extractPlainText(result);
            questions = fallbackFromLines(rawText, Math.max(1, request.getCount()));
        }

        // 최종 개수 제한 및 공백 정리
        questions = questions.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(Math.max(1, request.getCount()))
                .collect(Collectors.toList());

        if (questions.isEmpty()) {
            questions = List.of("질문을 생성하지 못했습니다.");
        }

        return AiResponse.builder().questions(questions).build();
    }

    // ---------- Helpers ----------

    private static String nullSafe(@Nullable String s) {
        return s == null ? "" : s;
    }

    /** candidates[0].content.parts[0].text 에 JSON이 담겨오는 전제를 우선 시도 */
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

            // text 자체가 {"questions":[...]} 형식일 것으로 기대
            Map<String, Object> json = parseJsonObject(text);
            if (json == null) return List.of();

            var arr = (List<Object>) json.get("questions");
            if (arr == null) return List.of();

            return arr.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());

        } catch (Exception ignore) {
            return List.of();
        }
    }

    /** JSON이 아니거나 파싱 실패 시 원문 텍스트 뽑기 */
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

    /** 라인 단위 폴백 파서 */
    private static List<String> fallbackFromLines(String rawText, int limit) {
        if (rawText == null) return List.of();
        return rawText.lines()
                .map(s -> s.replaceAll("^[\\s\\-•]*", ""))        // 불릿/공백 제거
                .map(s -> s.replaceAll("^[0-9]+[.)\\-:]\\s*", ""))// "1. ", "1) " 등 제거
                .filter(s -> !s.isBlank())
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    /** 간단 JSON 파서 (의존성 없이 Map으로) */
    private static Map<String, Object> parseJsonObject(String text) {
        // 최소 의존성 유지용. Jackson 쓰면 더 안전:
        // new ObjectMapper().readValue(text, new TypeReference<>() {})
        try {
            // 매우 간단한 케이스만 처리. 실제로는 Jackson 사용 권장.
            if (!text.trim().startsWith("{")) return null;
            // 여기선 보수적으로 Jackson 사용 권장 메시지 남김
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(text, Map.class);
        } catch (Throwable t) {
            return null;
        }
    }
}
