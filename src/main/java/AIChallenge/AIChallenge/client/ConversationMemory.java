package AIChallenge.AIChallenge.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationMemory {

    @Data
    @AllArgsConstructor
    public static class ChatMsg {
        private String role;
        private String content;
        private Instant ts;
    }

    @Data
    public static class Conversation {
        private final String id;
        private volatile String planSummary; // 추천 학습경로(요약/압축)
        private final Deque<ChatMsg> recent = new ArrayDeque<>(); // 최근 N개 롤링
    }

    private final Map<String, Conversation> store = new ConcurrentHashMap<>();
    private static final int MAX_RECENT = 20;
    private static final int MAX_PLAN_CHARS = 1800;

    public Conversation start(String planSummary) {
        String id = UUID.randomUUID().toString();
        Conversation c = new Conversation(id);
        c.setPlanSummary(compact(planSummary));
        store.put(id, c);
        return c;
    }

    public Conversation get(String id) {
        return id == null ? null : store.get(id);
    }

    public void append(String id, String role, String content) {
        Conversation c = store.get(id);
        if (c == null) return;
        Deque<ChatMsg> q = c.getRecent();
        q.addLast(new ChatMsg(role, content, Instant.now()));
        while (q.size() > MAX_RECENT) q.removeFirst();
    }

    public void setPlan(String id, String planSummary) {
        Conversation c = store.get(id);
        if (c != null) c.setPlanSummary(compact(planSummary));
    }

    private String compact(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= MAX_PLAN_CHARS) return s;
        return s.substring(0, MAX_PLAN_CHARS) + " …";
    }
}
