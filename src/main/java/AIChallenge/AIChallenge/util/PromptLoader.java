package AIChallenge.AIChallenge.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PromptLoader {

    @Value("${prompts.questions-template}")
    private String questionsTemplatePath;

    @Value("${prompts.learning-template}")
    private String learningTemplatePath;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 면접 질문 프롬프트 로드
     */
    public String loadQuestionsPrompt() {
        return loadTemplate(questionsTemplatePath);
    }

    /**
     * 학습 경로 프롬프트 로드
     */
    public String loadLearningPrompt() {
        return loadTemplate(learningTemplatePath);
    }

    /**
     * 실제 템플릿 로딩 (캐싱 포함)
     */
    private String loadTemplate(String path) {
        return cache.computeIfAbsent(path, p -> {
            try {
                ClassPathResource resource = new ClassPathResource(p);
                byte[] bytes = resource.getInputStream().readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("프롬프트 템플릿 로드 실패: " + path, e);
            }
        });
    }
}
