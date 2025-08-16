package AIChallenge.AIChallenge.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ViewController {

    // 기본 페이지 요청 처리
    @GetMapping("/")
    public String home() {
        return "ai"; // /WEB-INF/jsp/ai.jsp 로 이동
    }

}
