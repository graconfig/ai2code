package customer.ai2code.model.aicore.deepseek;

import lombok.AllArgsConstructor;
import lombok.Data;

// 简化：直接用基础类封装消息，删除消息工厂
@Data
@AllArgsConstructor
public class DeepseekMessage {
    private String role; // system/user/assistant
    private String content; // 消息内容
}