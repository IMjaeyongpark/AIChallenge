package AIChallenge.AIChallenge.controller;


import AIChallenge.AIChallenge.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ViewController {

    @GetMapping("/")
    public String home() {
        return "index"; // /WEB-INF/jsp/index.jsp 로 이동
    }

}
