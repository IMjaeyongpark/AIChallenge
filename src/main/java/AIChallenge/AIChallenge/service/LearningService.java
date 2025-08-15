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

    public AiResponse generate(AiRequest request, Integer weeks, Integer hoursPerWeek) {
        int w = (weeks == null || weeks <= 0) ? 6 : weeks;
        int h = (hoursPerWeek == null || hoursPerWeek <= 0) ? 8 : hoursPerWeek;
        // LearningClient는 AiResponse를 반환하도록 이미 변경됨
        return learningClient.suggest(request, w, h);
    }
}
