package AIChallenge.AIChallenge.service;


import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import AIChallenge.AIChallenge.client.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {
    private final AiClient aiClient; // 외부 AI API 호출 전용

    public AiResponse generate(AiRequest request) {
        // 여기서 입력 검증 가능
        if (request.getCount() <= 0) {
            return AiResponse.builder()
                    .questions(List.of("count는 1 이상이어야 합니다."))
                    .build();
        }

        // 외부 AI API 호출
        return aiClient.callAiApi(request);
    }
}
