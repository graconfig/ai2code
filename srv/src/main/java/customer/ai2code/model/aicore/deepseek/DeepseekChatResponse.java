package customer.ai2code.model.aicore.deepseek;

import lombok.Data;
import java.util.List;

@Data
public class DeepseekChatResponse {
    private List<Choice> choices;
    private Usage usage;

    @Data
    public static class Choice {
        private int index;
        private DeepseekMessage message; // 统一用Message，覆盖流式/非流式场景
    }

    @Data
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }
}
