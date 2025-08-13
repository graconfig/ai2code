package customer.ai2code.service.batch;

import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import customer.ai2code.service.impl.TaskBotCacheManager;
import customer.ai2code.service.BotService;
import customer.ai2code.model.tree.TaskBotNode;
import customer.ai2code.model.tree.TaskBotNode.NodeType;
import customer.ai2code.model.bot.Bot;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.ContextNodes;

import java.util.*;

/**
 * Spring Batch步骤和流配置类
 * 统一管理ChatBot Step、Function Call Bot Flow、SubTask Partitioning等组件
 */
@Configuration
public class BatchStepFlowConfiguration {

    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private TaskBotCacheManager taskBotCacheManager;
    
    @Autowired
    private BotService botService;
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    /**
     * 为主任务构建完整的Job
     */
    public Job buildMainTaskJob(String mainTaskId, TaskBotNode mainTaskNode) {
        String jobName = "mainTask-" + mainTaskId;
        
        JobBuilder jobBuilder = new JobBuilder(jobName, jobRepository);
        
        // 构建主任务的Flow
        Flow mainTaskFlow = buildTaskFlow(mainTaskNode);
        
        return jobBuilder
            .start(mainTaskFlow)
            .build()
            .build();
    }
    
    /**
     * 为任务节点构建Flow
     */
    public Flow buildTaskFlow(TaskBotNode taskNode) {
        String flowName = "taskFlow-" + taskNode.getId();
        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>(flowName);
        
        // 获取任务下的Bot节点
        List<TaskBotNode> botNodes = taskBotCacheManager.getChildren(taskNode);
        
        Flow currentFlow = null;
        
        for (int i = 0; i < botNodes.size(); i++) {
            TaskBotNode botNode = botNodes.get(i);
            if (botNode.getType() == NodeType.BOT_INSTANCE) {
                Bot bot = botNode.getBotObject();
                String functionType = bot.getBotType().getFunctionTypeCode();
                
                if ("A".equals(functionType)) {
                    // Chat Bot - 使用单个Step
                    Step chatBotStep = buildChatBotStep(botNode);
                    
                    if (currentFlow == null) {
                        currentFlow = flowBuilder.start(chatBotStep).build();
                    } else {
                        FlowBuilder<Flow> extendedBuilder = new FlowBuilder<>(flowName + "-extended-" + i);
                        currentFlow = extendedBuilder.start(currentFlow).next(chatBotStep).build();
                    }
                    
                } else if ("F".equals(functionType)) {
                    // Function Call Bot - 使用Flow (包含execute + subTasks)
                    Flow functionBotFlow = buildFunctionBotFlow(botNode);
                    
                    if (currentFlow == null) {
                        currentFlow = functionBotFlow;
                    } else {
                        FlowBuilder<Flow> extendedBuilder = new FlowBuilder<>(flowName + "-extended-" + i);
                        currentFlow = extendedBuilder.start(currentFlow).next(functionBotFlow).build();
                    }
                }
            }
        }
        
        // 如果没有Bot，创建空Flow
        if (currentFlow == null) {
            Step emptyStep = buildEmptyStep("emptyTask-" + taskNode.getId());
            currentFlow = flowBuilder.start(emptyStep).build();
        }
        
        return currentFlow;
    }
    
    /**
     * 构建Chat Bot步骤
     */
    public Step buildChatBotStep(TaskBotNode botNode) {
        String stepName = "chatBot-" + botNode.getId();
        
        Tasklet chatBotTasklet = new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                return executeChatBotTasklet(botNode, contribution, chunkContext);
            }
        };
        
        return new StepBuilder(stepName, jobRepository)
            .tasklet(chatBotTasklet, transactionManager)
            .allowStartIfComplete(true)
            .build();
    }
    
    /**
     * 构建Function Call Bot Flow (包含execute + subTasks两个步骤)
     */
    public Flow buildFunctionBotFlow(TaskBotNode botNode) {
        String flowName = "functionBotFlow-" + botNode.getId();
        
        // Step 1: 执行Function Call Bot的execute方法
        Step executeStep = buildFunctionBotExecuteStep(botNode);
        
        // Step 2: 执行SubTasks (使用Partitioning)
        Step subTasksStep = buildSubTasksPartitioningStep(botNode);
        
        return new FlowBuilder<Flow>(flowName)
            .start(executeStep)
            .next(createSubTaskDecision(botNode)) // 决定是否执行SubTasks
            .on("HAS_SUBTASKS").to(subTasksStep)
            .on("NO_SUBTASKS").end()
            .build();
    }
    
    /**
     * 构建Function Call Bot的execute步骤
     */
    public Step buildFunctionBotExecuteStep(TaskBotNode botNode) {
        String stepName = "functionBotExecute-" + botNode.getId();
        
        Tasklet executeTasklet = new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                return executeFunctionBotTasklet(botNode, contribution, chunkContext);
            }
        };
        
        return new StepBuilder(stepName, jobRepository)
            .tasklet(executeTasklet, transactionManager)
            .allowStartIfComplete(true)
            .build();
    }
    
    /**
     * 构建SubTasks分区步骤
     */
    public Step buildSubTasksPartitioningStep(TaskBotNode botNode) {
        String stepName = "subTasksPartitioning-" + botNode.getId();
        
        return new StepBuilder(stepName, jobRepository)
            .partitioner("subTaskPartition-" + botNode.getId(), createSubTaskPartitioner(botNode))
            .partitionHandler(createSubTaskPartitionHandler(botNode))
            .build();
    }
    
    /**
     * 创建SubTask分区器
     */
    public Partitioner createSubTaskPartitioner(TaskBotNode botNode) {
        return new Partitioner() {
            @Override
            public Map<String, ExecutionContext> partition(int gridSize) {
                Map<String, ExecutionContext> partitions = new HashMap<>();
                
                // 获取Bot节点下的子任务
                List<TaskBotNode> subTaskNodes = taskBotCacheManager.getChildren(botNode);
                
                int partitionIndex = 0;
                for (TaskBotNode subTaskNode : subTaskNodes) {
                    if (subTaskNode.getType() == NodeType.TASK) {
                        ExecutionContext context = new ExecutionContext();
                        context.putString("subTaskId", subTaskNode.getId());
                        context.putString("parentBotId", botNode.getId());
                        
                        String partitionKey = "subTask-" + partitionIndex++;
                        partitions.put(partitionKey, context);
                    }
                }
                
                return partitions;
            }
        };
    }
    
    /**
     * 创建SubTask分区处理器
     */
    public PartitionHandler createSubTaskPartitionHandler(TaskBotNode botNode) {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setTaskExecutor(taskExecutor);
        partitionHandler.setStep(buildSubTaskExecutionStep(botNode));
        partitionHandler.setGridSize(10); // 最大并行度
        
        return partitionHandler;
    }
    
    /**
     * 构建SubTask执行步骤 (每个分区执行)
     */
    public Step buildSubTaskExecutionStep(TaskBotNode parentBotNode) {
        String stepName = "subTaskExecution-" + parentBotNode.getId();
        
        Tasklet subTaskTasklet = new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                return executeSubTaskTasklet(contribution, chunkContext);
            }
        };
        
        return new StepBuilder(stepName, jobRepository)
            .tasklet(subTaskTasklet, transactionManager)
            .allowStartIfComplete(true)
            .build();
    }
    
    /**
     * 创建SubTask决策器 - 判断是否有子任务需要执行
     */
    public JobExecutionDecider createSubTaskDecision(TaskBotNode botNode) {
        return new JobExecutionDecider() {
            @Override
            public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
                // 检查Bot执行后是否产生了子任务
                List<TaskBotNode> subTaskNodes = taskBotCacheManager.getChildren(botNode);
                
                boolean hasSubTasks = subTaskNodes.stream()
                    .anyMatch(node -> node.getType() == NodeType.TASK);
                
                if (hasSubTasks) {
                    System.out.println("发现子任务，将执行SubTasks步骤: " + botNode.getId());
                    return new FlowExecutionStatus("HAS_SUBTASKS");
                } else {
                    System.out.println("无子任务，跳过SubTasks步骤: " + botNode.getId());
                    return new FlowExecutionStatus("NO_SUBTASKS");
                }
            }
        };
    }
    
    /**
     * 构建空步骤
     */
    public Step buildEmptyStep(String stepName) {
        return new StepBuilder(stepName, jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println("执行空步骤: " + stepName);
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }
    
    // ==================== Tasklet执行方法 ====================
    
    /**
     * 执行Chat Bot Tasklet
     */
    private RepeatStatus executeChatBotTasklet(TaskBotNode botNode, StepContribution contribution, ChunkContext chunkContext) {
        try {
            String botInstanceId = botNode.getId();
            System.out.println("执行Chat Bot: " + botInstanceId);
            
            // 检查是否已完成 (支持重启)
            if (isBotCompleted(botNode)) {
                System.out.println("跳过已完成的Chat Bot: " + botInstanceId);
                return RepeatStatus.FINISHED;
            }
            
            // 标记开始执行
           // taskBotCacheManager.updateBotStatus(botInstanceId, "R");
            
            // 1. 执行chat方法
            String chatContent = "继续执行任务";
            BotMessages chatResult = botService.chat(botInstanceId, chatContent);
            
            // 2. 调用adopt方法
            ContextNodes adoptResult = botService.adopt(botInstanceId, chatResult.getId());
            
            // 3. 记录执行结果
            chunkContext.getStepContext().getStepExecution().getExecutionContext()
                .put("chatResult", chatResult.getMessage());
            chunkContext.getStepContext().getStepExecution().getExecutionContext()
                .put("adoptResult", adoptResult.getId());
            
            // 4. 标记完成
            //taskBotCacheManager.updateBotStatus(botInstanceId, "S");
            
            System.out.println("Chat Bot执行完成: " + botInstanceId);
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            // 标记失败
           // taskBotCacheManager.updateBotStatus(botNode.getId(), "F");
            throw new RuntimeException("Chat Bot执行失败: " + botNode.getId(), e);
        }
    }
    
    /**
     * 执行Function Call Bot Tasklet
     */
    private RepeatStatus executeFunctionBotTasklet(TaskBotNode botNode, StepContribution contribution, ChunkContext chunkContext) {
        try {
            String botInstanceId = botNode.getId();
            System.out.println("执行Function Call Bot: " + botInstanceId);
            
            // 检查是否已完成 (支持重启)
            if (isBotCompleted(botNode)) {
                System.out.println("跳过已完成的Function Call Bot: " + botInstanceId);
                return RepeatStatus.FINISHED;
            }
            
            // 标记开始执行
           // taskBotCacheManager.updateBotStatus(botInstanceId, "R");
            
            // 执行execute方法
            var executeResult = botService.execute(botInstanceId);
            
            // 记录执行结果
            chunkContext.getStepContext().getStepExecution().getExecutionContext()
                .put("executeResult", executeResult.getResult());
            
            // 标记完成
           // taskBotCacheManager.updateBotStatus(botInstanceId, "S");
            
            System.out.println("Function Call Bot执行完成，可能产生了新的子任务: " + botInstanceId);
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            // 标记失败
           // taskBotCacheManager.updateBotStatus(botNode.getId(), "F");
            throw new RuntimeException("Function Call Bot执行失败: " + botNode.getId(), e);
        }
    }
    
    /**
     * 执行SubTask Tasklet (在分区中执行)
     */
    private RepeatStatus executeSubTaskTasklet(StepContribution contribution, ChunkContext chunkContext) {
        try {
            // 从分区上下文获取子任务信息
            String subTaskId = chunkContext.getStepContext().getStepExecution()
                .getExecutionContext().getString("subTaskId");
            String parentBotId = chunkContext.getStepContext().getStepExecution()
                .getExecutionContext().getString("parentBotId");
            
            System.out.println("执行SubTask分区: " + subTaskId + " (父Bot: " + parentBotId + ")");
            
            // 获取子任务节点并直接处理Bot
            TaskBotNode subTaskNode = taskBotCacheManager.getTaskNode(subTaskId);
            if (subTaskNode != null && subTaskNode.getBotObject() != null) {
                Bot bot = subTaskNode.getBotObject();
                String botInstanceId = subTaskNode.getId();
                
                // 执行Bot的逻辑 - 根据Bot类型调用正确的BotService方法
                String functionType = bot.getBotType().getFunctionTypeCode();
                if ("A".equals(functionType)) {
                    // ChatBot: 调用chat方法
                    botService.chat(botInstanceId, "继续执行SubTask");
                    System.out.println("SubTask ChatBot执行完成: " + botInstanceId);
                } else if ("F".equals(functionType)) {
                    // Function Call Bot: 调用execute方法
                    botService.execute(botInstanceId);
                    System.out.println("SubTask FunctionBot执行完成: " + botInstanceId);
                }
            }
            
            // 记录子任务执行结果
            chunkContext.getStepContext().getStepExecution().getExecutionContext()
                .put("subTaskId", subTaskId);
            chunkContext.getStepContext().getStepExecution().getExecutionContext()
                .put("subTaskStatus", "completed");
            
            System.out.println("SubTask分区执行完成: " + subTaskId);
            return RepeatStatus.FINISHED;
            
        } catch (Exception e) {
            String subTaskId = chunkContext.getStepContext().getStepExecution()
                .getExecutionContext().getString("subTaskId", "unknown");
            throw new RuntimeException("SubTask执行失败: " + subTaskId, e);
        }
    }
    
    /**
     * 检查Bot是否已完成
     */
    private boolean isBotCompleted(TaskBotNode botNode) {
        String status = botNode.getBotStatus();
        return "S".equals(status); // S表示成功完成
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 为指定任务节点构建完整的Job (递归处理)
     */
    public Job buildJobForTaskNode(String taskId, TaskBotNode taskNode) {
        String jobName = "recursiveTask-" + taskId;
        
        Flow taskFlow = buildTaskFlow(taskNode);
        
        return new JobBuilder(jobName, jobRepository)
            .start(taskFlow)
            .build()
            .build();
    }
    
    /**
     * 获取所有配置的Step名称 (用于监控)
     */
    public Set<String> getAllStepNames(TaskBotNode rootNode) {
        Set<String> stepNames = new HashSet<>();
        collectStepNames(rootNode, stepNames);
        return stepNames;
    }
    
    private void collectStepNames(TaskBotNode node, Set<String> stepNames) {
        if (node.getType() == NodeType.BOT_INSTANCE) {
            Bot bot = node.getBotObject();
            String functionType = bot.getBotType().getFunctionTypeCode();
            
            if ("A".equals(functionType)) {
                stepNames.add("chatBot-" + node.getId());
            } else if ("F".equals(functionType)) {
                stepNames.add("functionBotExecute-" + node.getId());
                stepNames.add("subTasksPartitioning-" + node.getId());
            }
        }
        
        // 递归处理子节点
        List<TaskBotNode> children = taskBotCacheManager.getChildren(node);
        for (TaskBotNode child : children) {
            collectStepNames(child, stepNames);
        }
    }
}
