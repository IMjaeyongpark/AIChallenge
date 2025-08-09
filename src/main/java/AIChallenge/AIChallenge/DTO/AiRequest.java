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
public class AiRequest {
    // 지원자 이력서 내용
    private String resume;

    // 목표 직무
    private String targetRole;

    // 기술 스택
    private List<String> techStack;

    // 질문 개수
    private int count;
}
