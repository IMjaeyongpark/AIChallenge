// src/main/java/AIChallenge/AIChallenge/util/PromptLoader.java
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

    @Value("${prompts.questions-template:prompts/interview_questions_ko.txt}")
    private String questionsTemplatePath;

    @Value("${prompts.learning-template:prompts/learning_path_ko.txt}")
    private String learningTemplatePath;

    @Value("${prompts.learning-chat-template:prompts/learning_chat_ko.txt}")
    private String learningChatTemplatePath;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String loadQuestionsPrompt() {
        return loadTemplate(questionsTemplatePath);
    }

    public String loadLearningPrompt() {
        return loadTemplate(learningTemplatePath);
    }

    public String loadLearningChatPrompt() {
        return loadTemplate(learningChatTemplatePath);
    }

    public String loadTemplate(String path) {
        return cache.computeIfAbsent(path, p -> {
            try {
                ClassPathResource res = new ClassPathResource(p);
                if (!res.exists()) {
                    throw new RuntimeException("클래스패스에 파일이 없습니다: " + p);
                }
                return new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("프롬프트 템플릿 로드 실패: " + p, e);
            }
        });
    }
}
