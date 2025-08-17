package AIChallenge.AIChallenge.service;


import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import AIChallenge.AIChallenge.client.LearningClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LearningService {

    private final LearningClient learningClient;

    // AI 학습 경로 생성 메서드
    public AiResponse generate(AiRequest request, Integer weeks, Integer hoursPerWeek) {
        int w = (weeks == null || weeks <= 0) ? 6 : weeks;
        int h = (hoursPerWeek == null || hoursPerWeek <= 0) ? 8 : hoursPerWeek;

        return learningClient.suggest(request, w, h);
    }

    public AiResponse chat(AiRequest req) {
        log.info("채팅임: question='{}'", req.getQuestion());
        return learningClient.chat(req);
    }
}
