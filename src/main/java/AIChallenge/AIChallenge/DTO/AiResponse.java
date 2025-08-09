package AIChallenge.AIChallenge.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiResponse {
    // 생성된 질문 목록
    private List<String> questions;

//    // 선택적으로 토큰 사용량, 비용, 처리 시간 같은 메타데이터 추가 가능
//    private long promptTokens;
//    private long completionTokens;
//    private double cost;
}
