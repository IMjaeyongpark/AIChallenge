package AIChallenge.AIChallenge.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class AiRequest {
    private String resume;
    private String targetRole;
    private List<String> techStack;
    private int count;

    private String question;
}

