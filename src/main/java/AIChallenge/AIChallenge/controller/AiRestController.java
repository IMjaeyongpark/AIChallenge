package AIChallenge.AIChallenge.controller;


import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import AIChallenge.AIChallenge.service.QuestionsService;
import AIChallenge.AIChallenge.service.LearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@Slf4j
@RequiredArgsConstructor
public class AiRestController {

    private final QuestionsService questionsService;
    private final LearningService learningService;


    @PostMapping("/questions")
    public ResponseEntity<AiResponse> generate(@RequestBody AiRequest req) {
        if (req.getCount() <= 0) {
            return ResponseEntity.badRequest()
                    .build();
        }

        AiResponse response = questionsService.generate(req);
        return ResponseEntity.ok(response); // 200 OK + JSON
    }

    @PostMapping("/learning-path")
    public ResponseEntity<AiResponse> learningPath(
            @RequestBody AiRequest req,
            @RequestParam(name = "weeks", required = false) Integer weeks,
            @RequestParam(name = "hoursPerWeek", required = false) Integer hoursPerWeek
    ) {
        if (req.getResume() == null || req.getResume().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(learningService.generate(req, weeks, hoursPerWeek));
    }

}
