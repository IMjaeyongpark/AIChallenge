package AIChallenge.AIChallenge.service;

import AIChallenge.AIChallenge.DTO.AiRequest;
import AIChallenge.AIChallenge.DTO.AiResponse;
import AIChallenge.AIChallenge.client.ChatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {


    private final ChatClient chatClient;

    public AiResponse chat(AiRequest request) {
        return chatClient.askChatBot(request);
    }
}