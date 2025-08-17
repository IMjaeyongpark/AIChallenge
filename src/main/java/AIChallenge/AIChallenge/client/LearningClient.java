package AIChallenge.AIChallenge.client;

import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import AIChallenge.AIChallenge.util.PromptLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class LearningClient {

    private final PromptLoader promptLoader;

    @Value("${gemini.api.key}")
    private String apiKey;

    // 타임아웃/헤더는 builder에서 설정
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /** 학습 경로 제안을 받아 AiResponse(문장 리스트)로 반환 */
    public AiResponse suggest(AiRequest req, int weeks, int hoursPerWeek) {
        String tpl = promptLoader.loadLearningPrompt();
        String prompt = tpl
                .replace("{role}", nullSafe(req.getTargetRole()))
                .replace("{techStack}", String.join(", ",
                        Optional.ofNullable(req.getTechStack()).orElse(List.of())))
                .replace("{resume}", nullSafe(req.getResume()))
                .replace("{weeks}", String.valueOf(weeks))
                .replace("{hoursPerWeek}", String.valueOf(hoursPerWeek));

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );

        Map<String, Object> result;
        try {
            result = webClient.post()
                    .uri("/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(25))
                    .block();
        } catch (WebClientResponseException e) {
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: HTTP %d %s".formatted(
                            e.getRawStatusCode(), e.getStatusText())))
                    .build();
        } catch (Exception e) {
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: " + e.getMessage()))
                    .build();
        }

        String json = extractText(result);
        List<String> lines = extractLinesFromJson(json);

        if (lines.isEmpty()) {
            // 폴백: 원문 텍스트를 줄 단위로 나눠서 반환
            lines = Optional.ofNullable(json)
                    .map(t -> t.lines()
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.toList()))
                    .orElse(List.of("학습 경로 응답이 비어 있습니다."));
        }

        return AiResponse.builder().questions(lines).build();
    }

    public AiResponse chat(AiRequest request) {
        // 질문이 없으면 방어
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return AiResponse.builder()
                    .questions(List.of("질문을 입력해 주세요."))
                    .build();
        }

        // 프롬프트: 이력서/직무/스택 + 사용자 질문 기반의 짧은 코칭
        String prompt = promptLoader.loadLearningChatPrompt().formatted(
                nullSafe(request.getTargetRole()),
                nullSafe(request.getResume()),
                String.join(", ",
                        Optional.ofNullable(request.getTechStack()).orElse(List.of())),
                nullSafe(request.getQuestion())
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        Map<String, Object> result;
        try {
            result = webClient.post()
                    .uri("/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();
        } catch (WebClientResponseException e) {
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: HTTP %d %s".formatted(
                            e.getRawStatusCode(), e.getStatusText())))
                    .build();
        } catch (Exception e) {
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: " + e.getMessage()))
                    .build();
        }

        String text = extractText(result);
        List<String> lines = (text == null ? "" : text).lines()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (lines.isEmpty()) {
            lines = List.of("답변이 비어 있습니다. 입력 내용을 다시 확인해 주세요.");
        }
        return AiResponse.builder().questions(lines).build();
    }

    // --------- Helpers ---------

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> result) {
        if (result == null) return "";
        try {
            var candidates = (List<Map<String, Object>>) result.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "";
            var content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return "";
            var parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return "";
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "";
        }
    }

    /** JSON(요약/kpi/주차별 계획 등)을 읽어 사람이 보기 좋은 문장 리스트로 변환 */
    private List<String> extractLinesFromJson(String json) {
        if (json == null || json.isBlank()) return List.of();

        try {
            Map<String, Object> root = mapper.readValue(json, new TypeReference<>() {});
            List<String> out = new ArrayList<>();

            // summary
            Object summary = root.get("summary");
            if (summary instanceof String s && !s.isBlank()) {
                out.add("요약: " + s.trim());
            }

            // KPIs
            Object kpisObj = root.get("kpis");
            if (kpisObj instanceof List<?> kpis && !kpis.isEmpty()) {
                String k = kpis.stream().map(Object::toString)
                        .collect(Collectors.joining(", "));
                out.add("KPI: " + k);
            }

            // plan (weeks)
            Object planObj = root.get("plan");
            if (planObj instanceof List<?> planList) {
                for (Object w : planList) {
                    if (!(w instanceof Map)) continue;
                    Map<?, ?> wm = (Map<?, ?>) w;

                    Integer week = toInt(wm.get("week"));
                    String theme = Objects.toString(wm.get("theme"), "");

                    String objectives = joinList(wm.get("objectives"));
                    String topics = joinList(wm.get("topics"));
                    String exercises = joinList(wm.get("exercises"));

                    Integer hours = toInt(wm.get("estimatedHours"));
                    String difficulty = Objects.toString(wm.get("difficulty"), "");

                    StringBuilder sb = new StringBuilder();
                    sb.append("Week ").append(week != null ? week : "?");
                    if (!theme.isBlank()) sb.append(" - ").append(theme);
                    if (!objectives.isBlank()) sb.append(" | 목표: ").append(objectives);
                    if (!topics.isBlank()) sb.append(" | 토픽: ").append(topics);
                    if (!exercises.isBlank()) sb.append(" | 과제: ").append(exercises);
                    if (hours != null) sb.append(" | 시간: ").append(hours).append("h");
                    if (!difficulty.isBlank()) sb.append(" | 난이도: ").append(difficulty);

                    out.add(sb.toString());
                }
            }

            // communication (optional)
            Object commObj = root.get("communication");
            if (commObj instanceof Map<?, ?> cm) {
                String checkpoints = joinList(cm.get("checkpoints"));
                String demos = joinList(cm.get("suggestedDemos"));
                if (!checkpoints.isBlank()) out.add("체크포인트: " + checkpoints);
                if (!demos.isBlank()) out.add("데모 아이디어: " + demos);
            }

            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        try {
            return (o instanceof Number n) ? n.intValue() : Integer.parseInt(o.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static String joinList(Object obj) {
        if (obj instanceof List<?> list && !list.isEmpty()) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(", "));
        }
        return "";
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
