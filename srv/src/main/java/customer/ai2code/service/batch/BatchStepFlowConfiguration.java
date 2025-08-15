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
        // 获取任务名称，如果没有则使用ID
        String taskName = mainTaskNode.getTaskObject().getTask().getName();
        String jobName = "MainTask[" + taskName + "]-Job-" + mainTaskId;

        JobBuilder jobBuilder = new JobBuilder(jobName, jobRepository);

        // 构建主任务的Flow
        Flow mainTaskFlow = buildTaskFlow(mainTaskNode);

        return jobBuilder
                .start(mainTaskFlow)
                .end()
                .build();
    }

    /**
     * 为任务节点构建Flow
     */
    public Flow buildTaskFlow(TaskBotNode taskNode) {
        // 获取任务名称信息
        String taskName = taskNode.getTaskObject().getTask().getName();
        String flowName = "TaskFlow[" + taskName + "]-" + taskNode.getId();
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
            String emptyTaskName = taskNode.getTaskObject().getTask().getName();
            Step emptyStep = buildEmptyStep("EmptyTaskStep[" + emptyTaskName + "]-" + taskNode.getId());
            currentFlow = flowBuilder.start(emptyStep).build();
        }

        return currentFlow;
    }

    /**
     * 构建Chat Bot步骤
     */
    public Step buildChatBotStep(TaskBotNode botNode) {
        // 获取Bot的详细信息来构建更具描述性的名称
        String botTypeName = botNode.getBotObject().getBotType().getName();
        String stepName = "ChatBotStep[" + botTypeName + "]-" + botNode.getId();

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
        // 获取Bot的详细信息来构建更具描述性的名称
        String botTypeName = botNode.getBotObject().getBotType().getName();
        String flowName = "FunctionBotFlow[" + botTypeName + "]-" + botNode.getId();

        // Step 1: 执行Function Call Bot的execute方法
        Step executeStep = buildFunctionBotExecuteStep(botNode);

        // Step 2: 执行SubTasks (使用Partitioning)
        Step subTasksStep = buildSubTasksPartitioningStep(botNode);

        JobExecutionDecider decider = createSubTaskDecision(botNode);
        
        return new FlowBuilder<Flow>(flowName)
                .start(executeStep)
                .next(decider)
                .on("HAS_SUBTASKS").to(subTasksStep)
                .from(decider).on("NO_SUBTASKS").end()
                .build();
    }

    /**
     * 构建Function Call Bot的execute步骤
     */
    public Step buildFunctionBotExecuteStep(TaskBotNode botNode) {
        // 获取Bot的详细信息来构建更具描述性的名称
        String botTypeName = botNode.getBotObject().getBotType().getName();
        String stepName = "FunctionBotExecuteStep[" + botTypeName + "]-" + botNode.getId();

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
        // 获取Bot的详细信息来构建更具描述性的名称
        String botTypeName = botNode.getBotObject().getBotType().getName();
        String stepName = "SubTasksPartitioningStep[" + botTypeName + "]-" + botNode.getId();

        return new StepBuilder(stepName, jobRepository)
                .partitioner("subTaskPartition-" + botNode.getId(), createSubTaskPartitioner(botNode))
                .partitionHandler(createSubTaskPartitionHandler(botNode))
                .build();
    }

    /**
     * 创建SubTask分区器 - 支持动态Flow构建
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

                        // 🎯 动态构建Flow定义 - 获取SubTask下的Bot列表
                        List<TaskBotNode> botsInSubTask = taskBotCacheManager.getChildren(subTaskNode);
                        List<Bot> botList = new ArrayList<>();
                        for (TaskBotNode botNode : botsInSubTask) {
                            if (botNode.getBotObject() != null) {
                                botList.add(botNode.getBotObject());
                            }
                        }

                        // 将Bot数量和序列信息存储到ExecutionContext
                        context.putInt("botCount", botList.size());
                        context.putString("botSequence", buildBotSequenceString(botsInSubTask));

                        // 为每个Bot存储详细信息
                        for (int i = 0; i < botsInSubTask.size(); i++) {
                            TaskBotNode botNode = botsInSubTask.get(i);
                            context.putString("bot_" + i + "_id", botNode.getId());
                            context.putString("bot_" + i + "_type",
                                    botNode.getBotObject().getBotType().getFunctionTypeCode());
                        }

                        String partitionKey = "subTask-" + partitionIndex++;
                        partitions.put(partitionKey, context);

                        System.out.println("创建SubTask分区: " + subTaskNode.getId() +
                                ", Bot数量: " + botList.size() +
                                ", 分区键: " + partitionKey);
                    }
                }

                return partitions;
            }
        };
    }

    /**
     * 构建Bot序列字符串 (用于调试和日志)
     */
    private String buildBotSequenceString(List<TaskBotNode> botNodes) {
        StringBuilder sequence = new StringBuilder();
        for (int i = 0; i < botNodes.size(); i++) {
            if (i > 0)
                sequence.append("->");
            TaskBotNode botNode = botNodes.get(i);
            if (botNode.getBotObject() != null) {
                sequence.append(botNode.getBotObject().getBotType().getFunctionTypeCode());
            }
        }
        return sequence.toString();
    }

    /**
     * 创建SubTask分区处理器 - 使用动态Step构建
     */
    public PartitionHandler createSubTaskPartitionHandler(TaskBotNode botNode) {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setTaskExecutor(taskExecutor);
        partitionHandler.setStep(buildDynamicSubTaskExecutionStep(botNode));
        partitionHandler.setGridSize(10); // 最大并行度

        return partitionHandler;
    }

    /**
     * 构建动态SubTask执行步骤 - 根据ExecutionContext中的Bot信息动态执行
     */
    public Step buildDynamicSubTaskExecutionStep(TaskBotNode parentBotNode) {
        // 获取父Bot的详细信息来构建更具描述性的名称
        String parentBotTypeName = parentBotNode.getBotObject().getBotType().getName();
        String stepName = "DynamicSubTaskExecutionStep[" + parentBotTypeName + "]-" + parentBotNode.getId();

        Tasklet dynamicSubTaskTasklet = new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                return executeDynamicSubTaskFlow(contribution, chunkContext);
            }
        };

        return new StepBuilder(stepName, jobRepository)
                .tasklet(dynamicSubTaskTasklet, transactionManager)

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
    private RepeatStatus executeChatBotTasklet(TaskBotNode botNode, StepContribution contribution,
            ChunkContext chunkContext) {
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
            String chatContent = "继续";
            BotMessages chatResult = botService.chat(botInstanceId, chatContent);

            // 2. 调用adopt方法
            ContextNodes adoptResult = botService.adopt(botInstanceId, chatResult.getId());

            // 3. 记录执行结果
            chunkContext.getStepContext().getStepExecution().getExecutionContext()
                    .put("chatResult", chatResult.getMessage());
            chunkContext.getStepContext().getStepExecution().getExecutionContext()
                    .put("adoptResult", adoptResult.getId());

            // 4. 标记完成
            // taskBotCacheManager.updateBotStatus(botInstanceId, "S");

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
    private RepeatStatus executeFunctionBotTasklet(TaskBotNode botNode, StepContribution contribution,
            ChunkContext chunkContext) {
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
     * 执行动态SubTask Flow - 根据ExecutionContext中的Bot信息执行完整的Bot序列
     */
    private RepeatStatus executeDynamicSubTaskFlow(StepContribution contribution, ChunkContext chunkContext) {
        try {
            // 从分区上下文获取子任务信息
            ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
            String subTaskId = executionContext.getString("subTaskId");
            String parentBotId = executionContext.getString("parentBotId");
            int botCount = executionContext.getInt("botCount", 0);
            String botSequence = executionContext.getString("botSequence", "");

            System.out.println("🚀 执行动态SubTask Flow: " + subTaskId +
                    " (父Bot: " + parentBotId +
                    ", Bot数量: " + botCount +
                    ", 序列: " + botSequence + ")");

            TaskBotNode taskNode = taskBotCacheManager.getTaskNode(subTaskId);

            List<TaskBotNode> botNodes = taskBotCacheManager.getChildren(taskNode);

            // 按序列执行每个Bot
            // for (int i = 0; i < botCount; i++) {
            for (TaskBotNode taskBotNode : botNodes) {
                String botId = taskBotNode.getId();
                String botType = taskBotNode.getBot().getBotType().getFunctionTypeCode();
                Integer sequence = taskBotNode.getBot().getBotInstance().getSequence();

                if (botId != null) {
                    System.out.println("执行Bot [" + sequence + "/" + botCount + "]: " + botId + " (类型: " + botType + ")");

                    if (isBotCompleted(taskBotNode)) {
                        System.out.println("跳过已完成的SubTask Bot: " + botId);
                        continue;
                    }

                    // 根据Bot类型执行相应的方法
                    if ("A".equals(botType)) {
                        // ChatBot: 执行chat + adopt
                        BotMessages chatResult = botService.chat(botId, "继续执行SubTask流程");
                        botService.adopt(botId, chatResult.getId());
                        System.out.println("✅ ChatBot执行完成: " + botId);

                    } else if ("F".equals(botType)) {
                        // Function Call Bot: 执行execute
                        botService.execute(botId);
                        System.out.println("✅ FunctionBot执行完成: " + botId);
                    }

                    // 记录当前Bot的执行状态
                    executionContext.put("bot_" + sequence + "_status", "completed");
                }
            }

            // 记录整个SubTask Flow的执行结果
            executionContext.put("subTaskFlowStatus", "completed");
            executionContext.put("completedBotCount", botCount);

            System.out.println("🎉 动态SubTask Flow执行完成: " + subTaskId + " (执行了 " + botCount + " 个Bot)");
            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            String subTaskId = chunkContext.getStepContext().getStepExecution()
                    .getExecutionContext().getString("subTaskId", "unknown");
            System.err.println("❌ 动态SubTask Flow执行失败: " + subTaskId + ", 错误: " + e.getMessage());
            throw new RuntimeException("动态SubTask Flow执行失败: " + subTaskId, e);
        }
    }

    /**
     * 检查Bot是否已完成
     */
    private boolean isBotCompleted(TaskBotNode botNode) {
        String status = botNode.getBotStatus();
        return "SUCCESS".equals(status); // S表示成功完成
    }

    // ==================== 工具方法 ====================

    /**
     * 为指定任务节点构建完整的Job (递归处理)
     */
    public Job buildJobForTaskNode(String taskId, TaskBotNode taskNode) {
        // 获取任务名称信息用于更好的Job命名
        String taskName = taskNode.getTaskObject().getTask().getName();
        String jobName = "RecursiveTaskJob[" + taskName + "]-" + taskId;

        Flow taskFlow = buildTaskFlow(taskNode);

        return new JobBuilder(jobName, jobRepository)
                .start(taskFlow)
                .end()
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
