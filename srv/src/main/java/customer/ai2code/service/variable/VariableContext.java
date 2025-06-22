package customer.ai2code.service.variable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 变量解析上下文
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariableContext {
    private String botInstanceId;
    private String mainTaskId; // 从botInstanceId推导出来
    private Object currentInstance; // BotInstances 实例
}