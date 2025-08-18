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
    private final ConversationMemory memory;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /** 학습 경로 제안 + 대화 세션 시작 */
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
                    .conversationId(null)
                    .build();
        } catch (Exception e) {
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: " + e.getMessage()))
                    .conversationId(null)
                    .build();
        }

        String json = extractText(result);
        List<String> lines = extractLinesFromJson(json);

        if (lines.isEmpty()) {
            lines = Optional.ofNullable(json)
                    .map(t -> t.lines().map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList()))
                    .orElse(List.of("학습 경로 응답이 비어 있습니다."));
        }

        // ★ 대화 세션 생성 & 초기 컨텍스트 저장
        String planSummaryForCtx = String.join("\n", lines);
        ConversationMemory.Conversation conv = memory.start(planSummaryForCtx);
        // 초깃값(시스템/어시스턴트 역할 기록)
        memory.append(conv.getId(), "system", "You are a helpful learning coach.");
        memory.append(conv.getId(), "assistant", planSummaryForCtx);

        return AiResponse.builder()
                .questions(lines)
                .conversationId(conv.getId())   // ★ 프론트에 전달 → 이후 /chat 에서 함께 보냄
                .build();
    }

    /** 추천된 학습경로 + 이전 대화를 포함해 이어서 코칭 */
    public AiResponse chat(AiRequest request) {
        String userMsg = nullSafe(request.getQuestion());

        if (userMsg.isBlank()) {
            return AiResponse.builder()
                    .questions(List.of("질문을 입력해 주세요."))
                    .conversationId(request.getConversationId())
                    .build();
        }

        // ★ 컨버세이션 로드 (없으면 임시 세션 생성)
        ConversationMemory.Conversation conv = memory.get(request.getConversationId());
        if (conv == null) {
            conv = memory.start(
                    // 최소 컨텍스트: 이력서/직무/스택로 간단 요약
                    ("[Ephemeral Session]\n" +
                            "Role: " + nullSafe(request.getTargetRole()) + "\n" +
                            "Tech: " + String.join(", ", Optional.ofNullable(request.getTechStack()).orElse(List.of())) + "\n" +
                            "Resume: " + nullSafe(request.getResume())).trim()
            );
            // 시스템 역할 기록
            memory.append(conv.getId(), "system", "You are a helpful learning coach.");
        }

        // 프롬프트 구성: planSummary + recent turns + 이번 질문
        StringBuilder ctx = new StringBuilder();
        if (conv.getPlanSummary() != null && !conv.getPlanSummary().isBlank()) {
            ctx.append("### Previously suggested learning path (summary)\n")
                    .append(conv.getPlanSummary()).append("\n\n");
        }
        if (!conv.getRecent().isEmpty()) {
            ctx.append("### Conversation so far\n");
            conv.getRecent().forEach(m ->
                    ctx.append(m.getRole().toUpperCase()).append(": ").append(m.getContent()).append("\n")
            );
            ctx.append("\n");
        }

        // 사용자의 현재 질문 추가
        ctx.append("### User question\n").append(userMsg).append("\n\n")
                .append("### Instructions\n")
                .append("- Answer concretely, referencing the learning path above when relevant.\n")
                .append("- If suggesting tasks, align them with the weeks/topics already proposed.\n")
                .append("- Keep answers concise and actionable.\n");

        String prompt = promptLoader.loadLearningChatPrompt().formatted(
                nullSafe(request.getTargetRole()),
                nullSafe(request.getResume()),
                String.join(", ", Optional.ofNullable(request.getTechStack()).orElse(List.of())),
                ctx.toString()
        );

        Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

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
                    .questions(List.of("AI 호출 실패: HTTP %d %s".formatted(e.getRawStatusCode(), e.getStatusText())))
                    .conversationId(conv.getId())
                    .build();
        } catch (Exception e) {
            return AiResponse.builder()
                    .questions(List.of("AI 호출 실패: " + e.getMessage()))
                    .conversationId(conv.getId())
                    .build();
        }

        String text = extractText(result);
        List<String> lines = (text == null ? "" : text).lines()
                .map(String::trim).filter(s -> !s.isBlank()).toList();

        if (lines.isEmpty()) {
            lines = List.of("답변이 비어 있습니다. 입력 내용을 다시 확인해 주세요.");
        }

        // ★ 히스토리 반영
        memory.append(conv.getId(), "user", userMsg);
        memory.append(conv.getId(), "assistant", String.join("\n", lines));

        return AiResponse.builder()
                .questions(lines)
                .conversationId(conv.getId())
                .build();
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

    private List<String> extractLinesFromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            Map<String, Object> root = mapper.readValue(json, new TypeReference<>() {});
            List<String> out = new ArrayList<>();

            Object summary = root.get("summary");
            if (summary instanceof String s && !s.isBlank()) out.add("요약: " + s.trim());

            Object kpisObj = root.get("kpis");
            if (kpisObj instanceof List<?> kpis && !kpis.isEmpty()) {
                String k = kpis.stream().map(Object::toString).collect(Collectors.joining(", "));
                out.add("KPI: " + k);
            }

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
        } catch (Exception ignored) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static String joinList(Object obj) {
        if (obj instanceof List<?> list && !list.isEmpty()) {
            return list.stream().filter(Objects::nonNull).map(Object::toString)
                    .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.joining(", "));
        }
        return "";
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
