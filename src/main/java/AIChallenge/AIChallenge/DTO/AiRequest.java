package AIChallenge.AIChallenge.DTO;


import lombok.*;

import java.util.List;

@Data
@Getter
public class AiRequest {
    private String resume;
    private String targetRole;
    private List<String> techStack;
    private int count;

    private String question;
}

