package AIChallenge.AIChallenge.controller;


import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import AIChallenge.AIChallenge.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@Slf4j
@RequiredArgsConstructor
public class AiRestController {

    private final AIService aiService;

    @PostMapping("/questions")
    public ResponseEntity<AiResponse> generate(@RequestBody AiRequest req) {
        if (req.getCount() <= 0) {
            return ResponseEntity.badRequest()
                    .build();
        }

        AiResponse response = aiService.generate(req);
        return ResponseEntity.ok(response); // 200 OK + JSON
    }

}
