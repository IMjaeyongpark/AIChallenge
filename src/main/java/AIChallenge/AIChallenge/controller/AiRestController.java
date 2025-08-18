package AIChallenge.AIChallenge.controller;


import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import AIChallenge.AIChallenge.service.QuestionsService;
import AIChallenge.AIChallenge.service.LearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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



    @Operation(
            summary = "AI 질문 생성",
            description = "AI를 사용하여 지정된 수의 질문을 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "질문 생성 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            }
    )
    @PostMapping("/questions")
    public ResponseEntity<AiResponse> generate(@RequestBody AiRequest req) {
        if (req.getCount() <= 0) {
            return ResponseEntity.badRequest()
                    .build();
        }

        AiResponse response = questionsService.generate(req);
        return ResponseEntity.ok(response); // 200 OK + JSON
    }

    @Operation(
            summary = "AI 학습 경로 생성",
            description = "AI를 사용하여 학습 경로를 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "학습 경로 생성 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            }
    )
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

    @Operation(
            summary = "AI 학습 경로 대화",
            description = "AI를 사용하여 학습 경로에 대한 질문을 합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "대화 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청")
            }
    )
    @PostMapping("/learning-path/chat")
    public ResponseEntity<AiResponse> learningPathChat(@RequestBody AiRequest req) {
        // 필수값: resume + question
        if (req.getResume() == null || req.getResume().isBlank()
                || req.getQuestion() == null || req.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(learningService.chat(req));
    }

}
