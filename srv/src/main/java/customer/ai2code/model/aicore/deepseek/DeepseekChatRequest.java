package customer.ai2code.model.aicore.deepseek;

import lombok.Data;
import java.util.List;

@Data
public class DeepseekChatRequest {
    private String model;
    private List<DeepseekMessage> messages;
    private Integer max_tokens;
    private Double temperature;
    private boolean stream = false;
    private Boolean enable_thinking; // Deepseek专属参数
}
