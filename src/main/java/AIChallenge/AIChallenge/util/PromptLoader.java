package AIChallenge.AIChallenge.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {

    @Value("${prompts.questions-template}")
    private String templatePath;

    public String loadTemplate() {
        try {
            return new String(
                    new ClassPathResource(templatePath).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw new RuntimeException("프롬프트 템플릿 로드 실패: " + templatePath, e);
        }
    }
}
