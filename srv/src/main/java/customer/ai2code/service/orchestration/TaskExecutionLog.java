package customer.ai2code.service.orchestration;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 */
@Data
@Builder
public class TaskExecutionLog {
    private String mainTaskId;
    private Long jobExecutionId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // 扩展字段
    private String errorMessage;
    private Long duration; // 执行时长（毫秒）
    private Integer totalSteps;
    private Integer completedSteps;
    private Integer failedSteps;
}
