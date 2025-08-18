package AIChallenge.AIChallenge.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiResponse {
    private List<String> questions;   // UI에서 그대로 뿌리는 라인들
    private String conversationId;    // ★ 새로 발급/유지할 대화 ID
}
