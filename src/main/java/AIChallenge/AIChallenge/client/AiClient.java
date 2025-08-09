package AIChallenge.AIChallenge.client;

import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiClient {
    public AiResponse callAiApi(AiRequest request) {
        List<String> tmpquestions = new ArrayList<>();
        for(int i = 0; i < 5; i++){
            tmpquestions.add("test questions : " + i);
        }
        return new AiResponse(tmpquestions);
    }
}
