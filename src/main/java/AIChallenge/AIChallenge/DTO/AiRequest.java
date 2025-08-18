package AIChallenge.AIChallenge.DTO;

import lombok.Data;

import java.util.List;

@Data
public class AiRequest {
    private String resume;
    private String targetRole;
    private List<String> techStack;
    private Integer count;

    // 학습 경로/챗 공통
    private Integer weeks;
    private Integer hoursPerWeek;

    // 챗 전용
    private String question;

    // ★ 대화 컨텍스트 식별자
    private String conversationId;
}
