package customer.ai2code.service.impl;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.configservice.ConfigService;
import cds.gen.configservice.ModelConfigs;
import cds.gen.configservice.ModelConfigs_;
import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.CDSViewFiles;
import cds.gen.mainservice.CDSViewFiles_;
import cds.gen.mainservice.CDSViews;
import cds.gen.mainservice.CDSViews_;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;
import cds.gen.mainservice.Viewfields;
import cds.gen.mainservice.Viewfields_;
import cds.gen.mainservice.RagJoinCond;
import cds.gen.mainservice.RagJoinCond_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.MainService;
import cds.gen.configservice.PromptTexts;
import cds.gen.configservice.PromptTexts_;
import cds.gen.ai.orchestration.rag.BusinessScenarios_;
import cds.gen.ai.orchestration.rag.BusinessScenarios;

import java.util.*;

import customer.ai2code.exception.BusinessException;

@Service
public class GenericCqnService {

    private final MainService mainService;
    private final ConfigService configService;
    private final EntityService entityService;
    private final ObjectMapper objectMapper;
    // private final DataSource dataSource;
    private final PersistenceService persistenceService;

    // private final TaskBotCacheManager cacheManager;

    public GenericCqnService(
            EntityService entityService,
            MainService mainService,
            ConfigService configService,
            // TaskBotCacheManager cacheManager,
            ObjectMapper objectMapper,
            PersistenceService persistenceService) {
        this.entityService = entityService;
        this.mainService = mainService;
        this.configService = configService;
        // this.cacheManager = cacheManager;
        // this.dataSource = dataSource;
        this.persistenceService = persistenceService;
        this.objectMapper = objectMapper;
    }

    // ========== ModelConfig 查询方法 ==========

    public ModelConfigs getModelConfig(String modelConfigId) {
        CqnSelect select = Select.from(ModelConfigs_.class)
                .where(m -> m.ID().eq(modelConfigId));
        return entityService.selectSingle(configService, select, ModelConfigs.class,
                "ModelConfig not found: " + modelConfigId);
    }

    // ========== BotInstance 查询方法 ==========

    public BotInstances getBotInstanceById(String botInstanceId) {
        var select = Select.from(BotInstances_.class).where(b -> b.ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found: " + botInstanceId);
    }

    public BotInstances getBotInstanceAndSubtasksById(String botInstanceId) {
        var select = Select.from(BotInstances_.class).columns(b -> b._all(), b -> b.tasks().expand())
                .where(b -> b.ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found with subtasks: " + botInstanceId);
    }

    public BotInstances getBotInstanceByTaskAndSequence(String taskId, int sequence) {
        var select = Select.from(BotInstances_.class)
                .where(b -> b.task_ID().eq(taskId).and(b.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found for task: " + taskId + ", sequence: " + sequence);
    }

    /**
     * 根据MessageID查询关联的BotInstance ID
     */
    public String getBotInstanceIdByMessageId(String messageId) {
        CqnSelect select = Select.from(BotMessages_.class)
                .columns(m -> m.botInstance_ID())
                .where(m -> m.ID().eq(messageId));

        BotMessages message = entityService.selectSingle(
                mainService,
                select,
                BotMessages.class,
                "Message not found: " + messageId // 添加错误消息参数
        );

        return message != null ? message.getBotInstanceId() : null;
    }

    // ========== BotInstance 创建和插入方法 ==========

    public BotInstances createAndInsertBotInstance(String taskId, BotTypes botType) {
        BotInstances botInstance = BotInstances.create();
        botInstance.setId(UUID.randomUUID().toString());
        botInstance.setSequence(botType.getSequence());
        botInstance.setTypeId(botType.getId());
        botInstance.setStatusCode("CREATED"); // Created
        botInstance.setTaskId(taskId);

        entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
        return botInstance;
    }

    // ========== BotInstance 更新方法 ==========

    public void updateBotInstance(BotInstances botInstance) {
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }

    public void updateBotInstanceStatus(BotInstances botInstance, String statusCode) {
        // BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        updateBotInstance(botInstance);
    }

    public void updateBotInstanceResult(BotInstances botInstance, String result) {
        // BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setResult(result);
        updateBotInstance(botInstance);
    }

    // 在GenericCqnService中添加这个方法
    public void updateBotInstanceContextNodeId(BotInstances botInstance, String contextNodeId) {
        // Update updateQuery = Update.entity(BotInstances_.class)
        // .where(b -> b.ID().eq(botInstanceId))
        // .data(BotInstances.CONTEXT_NODE_ID, contextNodeId);

        // persistenceService.run(updateQuery);
        botInstance.setContextID(contextNodeId);
        // entityService.update(mainService, null, BotInstances_.class, botInstance,
        // true);
        updateBotInstance(botInstance);
    }

    // ========== BotType 查询方法 ==========

    public BotTypes getBotTypeById(String typeId) {
        var select = Select.from(BotTypes_.class).where(b -> b.ID().eq(typeId));
        return entityService.selectSingle(configService, select, BotTypes.class,
                "BotType not found: " + typeId);
    }

    public List<BotTypes> getBotTypesByTaskType(String taskTypeId) {
        var select = Select.from(BotTypes_.class)
                .where(b -> b.taskType_ID().eq(taskTypeId))
                .orderBy(b -> b.sequence().asc());
        return entityService.selectList(configService, select, BotTypes.class);
    }

    // ========== Task 查询方法 ==========

    public Tasks getTaskById(String taskId) {
        var select = Select.from(Tasks_.class).where(t -> t.ID().eq(taskId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found: " + taskId);
    }

    public Tasks getTaskAndSubBotsById(String taskId) {
        var select = Select.from(Tasks_.class).columns(t -> t._all(), t -> t.botInstances().expand())
                .where(t -> t.ID().eq(taskId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found with bots: " + taskId);
    }

    public Tasks getTaskByBotInstanceAndSequence(String botInstanceId, int sequence) {
        var select = Select.from(Tasks_.class)
                .where(t -> t.botInstance_ID().eq(botInstanceId).and(t.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found for botInstance: " + botInstanceId + ", sequence: " + sequence);
    }

    // 根据BotInstance ID获取任务详情（33160）
    public Tasks getTaskByBotInstance(String botInstanceId) {
        var select = Select.from(Tasks_.class)
                .where(t -> t.botInstance_ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found for botInstance: " + botInstanceId);
    }

    public String getTaskIdByBotInstanceId(String botInstanceId) {
        // 构建查询语句
        CqnSelect select = Select.from(BotInstances_.class)
                .columns(b -> b.task_ID())
                .where(b -> b.ID().eq(botInstanceId));

        // 查询结果列表
        BotInstances instance = entityService.selectSingle(mainService, select, BotInstances.class,
                String.format("Parent task not found for bot instance %s", botInstanceId));

        // 检查并返回结果
        // if (instance.isEmpty() || instances.get(0).getTaskId() == null) {
        // throw new IllegalStateException("No taskId found for botInstanceId: " +
        // botInstanceId);
        // }

        return instance.getTaskId();
    }

    /**
     * 根据BotInstance ID获取父任务详情
     * 这个方法用于获取创建当前BotInstance的父任务（即包含该BotInstance的任务）
     * 暂不使用
     */
    public Tasks getParentTaskByBotInstance(String botInstanceId) {
        try {
            // 1. 先获取当前BotInstance
            // BotInstances botInstance = getBotInstanceById(botInstanceId);

            // 2. 获取BotInstance所属的任务
            String taskId = getTaskIdByBotInstanceId(botInstanceId);

            // 3. 获取该任务
            Tasks currentTask = getTaskById(taskId);

            // 4. 如果当前任务有父任务ID，则获取父任务
            // if (currentTask.getParentTaskId() != null &&
            // !currentTask.getParentTaskId().isEmpty()) {
            // return getTaskById(currentTask.getParentTaskId());
            // } else {
            // // 5. 如果没有父任务，返回当前任务本身
            // return currentTask;
            // }
            return currentTask;

        } catch (Exception e) {
            throw new BusinessException("Failed to get parent task for botInstance: " + botInstanceId, e);
        }
    }

    /**
     * 根据botInstanceId获取主任务ID
     * 已启用缓存读取
     */
    public String getMainTaskId(String botInstanceId) {
        // 使用缓存管理器获取主任务ID
        // String mainTaskId = cacheManager.getMainTaskId(botInstanceId);
        // if (mainTaskId != null) {
        //     return mainTaskId;
        // }

        // 如果缓存中没有，执行原有逻辑
        // 1. 通过botInstanceId获取BotInstance
        BotInstances botInstance = getBotInstanceById(botInstanceId);

        // 2. 获取当前任务ID
        String currentTaskId = botInstance.getTaskId();

        // 3. 循环查找，直到找到主任务
        while (currentTaskId != null) {
            Tasks currentTask = getTaskById(currentTaskId);

            // 4. 如果是主任务，返回该任务ID
            if (currentTask.getIsMain() != null && currentTask.getIsMain()) {
                return currentTaskId;
            }

            // 5. 如果不是主任务，通过botInstanceId找到父任务
            String parentBotInstanceId = currentTask.getBotInstanceId();

            if (parentBotInstanceId == null || parentBotInstanceId.isEmpty()) {
                // 如果没有父BotInstance，说明这可能就是顶层任务
                // 但不是主任务，这种情况可能是数据错误
                throw new BusinessException(
                        "Found top-level task but it's not marked as main task: " + currentTaskId);
            }

            // 6. 获取父BotInstance的任务ID
            BotInstances parentBotInstance = getBotInstanceById(parentBotInstanceId);
            currentTaskId = parentBotInstance.getTaskId();
        }

        // 如果遍历完还没找到主任务，抛出异常
        throw new BusinessException("Main task not found for botInstanceId: " + botInstanceId);
    }

    // ========== Task 创建和插入方法 ==========

    public Tasks createAndInsertMainTask(String name, String description, String taskTypeId) {
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(true);
        newTask.setContextPath("");
        newTask.setSequence(0);
        newTask.setTypeId(taskTypeId);

        entityService.insert(mainService, null, Tasks_.class, newTask, true);
        return newTask;
    }

    public Tasks createAndInsertSubTask(String name, String description, String contextPath,
            int sequence, String botInstanceId, String taskTypeId) {
        Tasks newTask = Tasks.create();
        newTask.setId(UUID.randomUUID().toString());
        newTask.setName(name);
        newTask.setDescription(description);
        newTask.setIsMain(false); // 子任务
        newTask.setContextPath(contextPath);
        newTask.setSequence(sequence);
        newTask.setBotInstanceId(botInstanceId);
        newTask.setTypeId(taskTypeId);

        entityService.insert(mainService, null, Tasks_.class, newTask, true);
        return newTask;
    }

    // ========== ContextNode 查询方法 ==========

    public ContextNodes getContextNodeById(String contextNodeId) {
        var select = Select.from(ContextNodes_.class).where(c -> c.ID().eq(contextNodeId));
        return entityService.selectSingle(mainService, select, ContextNodes.class,
                "ContextNode not found: " + contextNodeId);
    }

    // 修改为根据taskId和contextPath查询
    public ContextNodes getContextNodeByTaskAndPath(String taskId, String contextPath) {
        var select = Select.from(ContextNodes_.class)
                .where(c -> c.task_ID().eq(taskId).and(c.path().eq(contextPath)));
        return entityService.selectSingle(mainService, select, ContextNodes.class,
                "ContextNode not found for task: " + taskId + ", path: " + contextPath);
    }

    /**
     * 根据任务ID和路径模式查询ContextNodes
     * 支持通配符模式，如: subtask[*].a.property 匹配 subtask[0].a.property,
     * subtask[123].a.property 等
     */
    public List<ContextNodes> getContextNodesByTaskAndPathPattern(String taskId, String contextPath) {
        // 将通配符模式转换为正则表达式
        // 例如: subtask[*].a.property -> subtask\[\d+\]\.a\.property
        String regexPattern = convertWildcardToRegex(contextPath);

        var select = Select.from(ContextNodes_.class)
                .where(c -> c.task_ID().eq(taskId).and(c.path().matchesPattern(regexPattern)));
        return entityService.selectList(mainService, select, ContextNodes.class);
    }

    /**
     * 根据主任务ID查询所有上下文节点
     * 
     * @param mainTaskId
     * @return
     */
    public List<ContextNodes> getContextNodesByMainTaskId(String mainTaskId) {
        var select = Select.from(ContextNodes_.class)
                .where(c -> c.task_ID().eq(mainTaskId));
        return entityService.selectList(mainService, select, ContextNodes.class);
    }

    // ========== ContextNode 创建和插入方法 ==========

    public ContextNodes createAndInsertContextNode(String taskId, String path, String label,
            String type, String value) {
        ContextNodes contextNode = ContextNodes.create();
        contextNode.setId(UUID.randomUUID().toString());
        contextNode.setTaskId(taskId);
        contextNode.setPath(path);
        contextNode.setLabel(label);
        contextNode.setType(type);
        contextNode.setValue(value);

        entityService.insert(mainService, null, ContextNodes_.class, contextNode, true);
        return contextNode;
    }

    // ========== ContextNode 更新方法 ==========

    // 更新ContextNode的业务方法
    public ContextNodes updateContextNodeValue(ContextNodes existingNode, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setModifiedAt(java.time.Instant.now());

        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
    }

    public ContextNodes updateContextNodeAdditionalInfo(String mainTaskId, String contextPath, String additionalInfo) {
        ContextNodes contextNodes = getContextNodeByTaskAndPath(mainTaskId, contextPath);
        contextNodes.setAdditionalInfo(additionalInfo);
        entityService.update(mainService, null, ContextNodes_.class, contextNodes, true);
        return contextNodes;
    }

    // ========== PromptText 查询方法 ==========

    /**
     * 根据BotType ID查询所有PromptTexts
     */
    public List<PromptTexts> getPromptTextsByBotType(String botTypeId) {
        var select = Select.from(PromptTexts_.class)
                .where(p -> p.botType_ID().eq(botTypeId));
        // .orderBy(p -> p.sequence().asc());
        return entityService.selectList(configService, select, PromptTexts.class);
    }

    // ========== BotMessage 查询方法 ==========

    /**
     * 判断是否是第一次调用
     */
    public boolean isFirstCall(String botInstanceId) {
        try {
            // 查询该Bot实例是否有历史消息
            List<BotMessages> messages = getBotMessagesByBotInstanceId(botInstanceId);
            return messages == null || messages.isEmpty();
        } catch (Exception e) {
            // 如果查询失败，默认认为是第一次调用
            return true;
        }
    }

    /**
     * 根据BotInstance ID查询所有BotMessages
     */
    public List<BotMessages> getBotMessagesByBotInstanceId(String botInstanceId) {
        var select = Select.from(BotMessages_.class)
                .where(b -> b.botInstance_ID().eq(botInstanceId))
                .orderBy(b -> b.createdAt().asc());
        return entityService.selectList(mainService, select, BotMessages.class);
    }

    /**
     * 根据BotInstance ID获取最新的用户消息
     */
    public BotMessages getLatestUserMessage(String botInstanceId) {
        CqnSelect select = Select.from(BotMessages_.class)
                .where(b -> b.botInstance_ID().eq(botInstanceId)
                        .and(b.role().eq("user")))
                .orderBy(b -> b.createdAt().desc())
                .limit(1);

        List<BotMessages> messages = entityService.selectList(mainService, select, BotMessages.class);

        if (messages.isEmpty()) {
            return null; // 如果没有找到用户消息，返回 null
        }

        return messages.get(0);
    }

    /**
     * 根据BotInstance ID和MessageID获取Message
     */
    public BotMessages getMessageById(String botInstanceId, String messageId) {
        CqnSelect select = Select.from(BotMessages_.class)
                .where(m -> m.ID().eq(messageId)
                        .and(m.botInstance_ID().eq(botInstanceId)));
        return entityService.selectSingle(
                mainService,
                select,
                BotMessages.class,
                String.format("Message %s not found in bot instance %s", messageId, botInstanceId));
    }

    // ========== BotMessage 创建和插入方法 ==========

    /**
     * 创建并插入单条BotMessage（指定角色）
     */
    public BotMessages createAndInsertBotMessage(String botInstanceId, String message, String role) {
        BotMessages botMessage = BotMessages.create();
        botMessage.setId(UUID.randomUUID().toString());
        botMessage.setBotInstanceId(botInstanceId);
        botMessage.setMessage(message);
        botMessage.setRole(role);

        entityService.insert(mainService, null, BotMessages_.class, botMessage, true);
        return botMessage;
    }

    /**
     * 创建并插入单条BotMessage（指定角色）
     */
    public BotMessages createAndInsertBotMessage(String botInstanceId, String message, String ragContent, String role) {
        BotMessages botMessage = BotMessages.create();
        botMessage.setId(UUID.randomUUID().toString());
        botMessage.setBotInstanceId(botInstanceId);
        botMessage.setMessage(message);
        botMessage.setRagData(ragContent);
        botMessage.setRole(role);

        entityService.insert(mainService, null, BotMessages_.class, botMessage, true);
        return botMessage;
    }

    // ========== 工具方法 ==========

    /**
     * 将通配符模式转换为正则表达式
     * 例如: subtask[*].a.property -> subtask\[\d+\]\.a\.property
     */
    private String convertWildcardToRegex(String wildcardPattern) {
        if (wildcardPattern == null) {
            return null;
        }

        // 首先处理特殊的通配符模式 [*]，避免被后续的转义影响
        String regex = wildcardPattern.replace("[*]", "PLACEHOLDER_FOR_DIGITS");

        // 转义正则表达式特殊字符
        regex = regex
                .replace("\\", "\\\\") // 转义反斜杠
                .replace(".", "\\.") // 转义点号
                .replace("(", "\\(") // 转义左括号
                .replace(")", "\\)") // 转义右括号
                .replace("+", "\\+") // 转义加号
                .replace("^", "\\^") // 转义尖角号
                .replace("$", "\\$") // 转义美元符号
                .replace("|", "\\|") // 转义管道符
                .replace("?", "\\?") // 转义问号
                .replace("*", "\\*") // 转义星号（但不是在[]内的）
                .replace("{", "\\{") // 转义左大括号
                .replace("}", "\\}") // 转义右大括号
                .replace("[", "\\[") // 转义左中括号
                .replace("]", "\\]"); // 转义右中括号

        // 最后将占位符替换为正确的数字匹配模式
        regex = regex.replace("PLACEHOLDER_FOR_DIGITS", "\\[\\d+\\]");

        return regex;
    }

    // 根据path生成友好的label
    public String generateLabelFromPath(String contextPath) {
        if (contextPath == null || contextPath.isEmpty()) {
            return "Context";
        }

        // 将路径转换为友好的标签
        String[] parts = contextPath.split("\\.");
        String lastPart = parts[parts.length - 1];

        // 将驼峰命名转换为可读格式
        return lastPart.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("_", " ")
                .replaceAll("-", " ")
                .toLowerCase()
                .replaceAll("\\b\\w", "");
    }

    /**
     * 根据BotInstance ID获取输出上下文路径
     * 暂不使用
     */
    public String getOutputContextPathByBotInstanceId(String botInstanceId) {
        // 1. 获取BotInstance
        BotInstances botInstance = getBotInstanceById(botInstanceId);

        // 2. 获取关联的BotType
        BotTypes botType = getBotTypeById(botInstance.getTypeId());

        // 3. 返回配置的输出路径
        return botType.getOutputContextPath();
    }

    // ========== RAG 相关查询方法 ==========

    public String findMatchingViewsByScenario(String ragSource, int ragTopK, String query,
            Locale language, double threshold) throws JsonProcessingException {
        // 1.构建向量
        // CqnVector vector = CQL.vector(query);
        // var similarity = CQL.cosineSimilarity(CQL.get("embeddings"), vector);
        // 2.查询 BusinessScenarios 表，获取符合条件的场景
        // CqnSelect selectScenario = Select.from(BusinessScenarios_.class)
        // .columns(b -> b.get("scenario"), b -> b.get("description"), b ->
        // b.get("viewCategory"),
        // b -> similarity.as("similarity"))
        // .where(b -> similarity.gt(threshold))
        // .orderBy(b -> similarity.desc())
        // .limit(ragTopK);
        // CQL nativeEmbedding =

        // List<Row> scenarioRows = mainService.run(selectScenario).listOf(Row.class);
        CqnSelect selectScenario = Select.from(BusinessScenarios_.class)
                .columns(s -> s.scenario(), s -> s.description(), s -> s.viewCategory())
                .orderBy(b -> CQL.cosineSimilarity(b.embeddings(),
                        CQL.func("VECTOR_EMBEDDING", CQL.constant(query), CQL.constant("QUERY"),
                                CQL.constant("SAP_NEB.20240715")))
                        .desc())
                .limit(ragTopK);
        List<BusinessScenarios> scenarioRows = persistenceService.run(selectScenario).listOf(BusinessScenarios.class);

        // if (scenarioRows.isEmpty()) {
        // return "[]"; // 如果没有匹配的场景，返回空数组
        // }
        // 3.提取 viewCategory 并展开
        Set<String> categories = new LinkedHashSet<>();
        for (BusinessScenarios row : scenarioRows) {
            String viewCategory = (String) row.getViewCategory();
            if (viewCategory != null) {
                String[] parts = viewCategory.split("/");
                for (String part : parts) {
                    categories.add(part.trim());
                }
            }
        }

        if (categories.isEmpty())
            return "[]";

        // Step 4: 查询 CDSViews 表中符合条件的 view
        // CqnSelect selectViews = Select.from(CDSViews_.class)
        // .columns("viewName", "viewDesc", "viewCategory")
        // .where(c -> CQL.get("viewCategory").in(categories)
        // .and(CQL.get("isActive").eq(true)));
        CqnSelect selectViews = Select.from(CDSViews_.class)
                .columns(v -> v.viewName(), v -> v.viewDesc(), v -> v.viewCategory())
                .where(v -> v.viewCategory().in(categories)
                        .and(v.isActive().eq(true)));

        List<CDSViews> viewRows = mainService.run(selectViews).listOf(CDSViews.class);

        // Step 5: 返回 JSON 格式
        return objectMapper.writeValueAsString(viewRows);

    }

    public String findViewFieldsByViewNames(List<String> viewList, int ragTopK, Locale language)
            throws JsonProcessingException {

        CqnSelect select = Select.from(Viewfields_.class)
                .columns(f -> f.get("tableName"), f -> f.get("tableDesc"), f -> f.get("content"))
                .where(f -> f.get("tableName").in(viewList).and(f.get("langu").eq(language.getLanguage())))
                .limit(ragTopK);

        List<Viewfields> rows = mainService.run(select).listOf(Viewfields.class);
        return objectMapper.writeValueAsString(rows);

    }

    public String findJoinConditionsByViewNames(List<String> viewList) throws JsonProcessingException {
        CqnSelect select = Select.from(RagJoinCond_.class)
                .columns(c -> c.tableFirst(), c -> c.tableSecond(), c -> c.tableJoin())
                .where(c -> c.tableFirst().in(viewList).or(c.tableSecond().in(viewList)));

        List<RagJoinCond> rows = mainService.run(select).listOf(RagJoinCond.class);
        return objectMapper.writeValueAsString(rows);
    }

    // ========== CDSViews 操作方法 ==========

    /**
     * 插入CDSViews实体（基于viewName主键）
     * 
     * @param view CDSViews实体（需包含viewName）
     */
    public void insertCDSViews(CDSViews view) {
        // 校验必填字段（viewName）
        if (view.getViewName() == null || view.getViewName().isEmpty()) {
            throw new IllegalArgumentException("CDSViews.viewName不能为空");
        }

        entityService.insert(
                mainService, // 使用MainService作为服务上下文
                null, // 无事务上下文
                CDSViews_.class,
                view,
                true // 自动生成审计字段（managed aspect）
        );
    }

    /**
     * 批量插入CDSViews实体
     * 
     * @param views CDSViews实体列表
     */
    public void batchInsertCDSViews(List<CDSViews> views) {
        if (views == null || views.isEmpty())
            return;

        // 检查每个视图是否有viewName
        for (CDSViews view : views) {
            if (view.getViewName() == null || view.getViewName().isEmpty()) {
                throw new IllegalArgumentException("CDSViews的viewName不能为空");
            }
        }

        // 使用EntityService的批量插入方法
        entityService.batchInsert(
                mainService,
                null,
                CDSViews_.class,
                views,
                null, // 没有reportId
                true // 非草稿模式
        );
    }

    /**
     * 批量查询CDSViews（根据viewName列表）
     * 
     * @param viewNames 视图名称列表
     * @return CDSViews实体列表
     */
    public List<CDSViews> batchSelectCDSViews(List<String> viewNames) {
        if (viewNames == null || viewNames.isEmpty()) {
            return Collections.emptyList();
        }

        CqnSelect select = Select.from(CDSViews_.class)
                .where(v -> v.viewName().in(viewNames));

        return entityService.selectList(mainService, select, CDSViews.class);
    }

    /**
     * 批量更新CDSViews
     * 
     * @param views 需要更新的视图实体列表
     */
    public void batchUpdateCDSViews(List<CDSViews> views) {
        if (views == null || views.isEmpty())
            return;

        views.forEach(view -> {
            // 校验必要字段
            if (view.getViewName() == null) {
                throw new IllegalArgumentException("CDSViews必须包含viewName");
            }

            // 执行单个更新
            entityService.update(
                    mainService,
                    null,
                    CDSViews_.class,
                    view,
                    true);
        });
    }

    public void deleteCDSViewsByNames(List<String> viewNames) {
        if (viewNames == null || viewNames.isEmpty())
            return;
        // 使用entityService的delete方法
        for (String viewName : viewNames) {
            CDSViews view = CDSViews.create();
            view.setViewName(viewName);
            entityService.delete(mainService, null, CDSViews_.class, view, true);
        }
    }

    // ========== Viewfields 操作方法 ==========

    /**
     * 插入Viewfields实体（带ID主键）
     * 
     * @param field Viewfields实体（需包含ID或自动生成）
     */
    public void insertViewfields(Viewfields field) {
        // 自动生成ID（如果未设置）
        if (field.getId() == null) {
            field.setId(UUID.randomUUID().toString());
        }

        // 调用entityService插入
        entityService.insert(
                mainService,
                null,
                Viewfields_.class,
                field,
                true);
    }

    /**
     * 批量插入Viewfields实体
     * 
     * @param fields Viewfields实体列表
     */
    public void batchInsertViewfields(List<Viewfields> fields) {
        if (fields == null || fields.isEmpty())
            return;

        // 为每个字段生成ID（如果未设置）
        for (Viewfields field : fields) {
            if (field.getId() == null) {
                field.setId(UUID.randomUUID().toString());
            }
        }

        // 使用EntityService的批量插入方法
        entityService.batchInsert(
                mainService,
                null,
                Viewfields_.class,
                fields,
                null, // 没有reportId
                true // 非草稿模式
        );
    }

    /**
     * 批量查询Viewfields（根据ID列表）
     * 
     * @param fieldIds 字段ID列表
     * @return Viewfields实体列表
     */
    public List<Viewfields> batchSelectViewfields(List<String> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return Collections.emptyList();
        }

        CqnSelect select = Select.from(Viewfields_.class)
                .where(f -> f.ID().in(fieldIds));

        return entityService.selectList(mainService, select, Viewfields.class);
    }

    /**
     * 根据表名和语言批量查询Viewfields
     * 
     * @param tableNames 表名列表
     * @param language   语言代码
     * @return Viewfields实体列表
     */
    public List<Viewfields> batchSelectViewfieldsByTable(List<String> tableNames, String language) {
        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyList();
        }

        CqnSelect select = Select.from(Viewfields_.class)
                .where(f -> f.tableName().in(tableNames)
                        .and(f.langu().eq(language)));

        return entityService.selectList(mainService, select, Viewfields.class);
    }

    /**
     * 批量更新Viewfields
     * 
     * @param fields 需要更新的字段实体列表
     */
    public void batchUpdateViewfields(List<Viewfields> fields) {
        if (fields == null || fields.isEmpty())
            return;

        fields.forEach(field -> {
            // 校验必要字段
            if (field.getId() == null) {
                field.setId(UUID.randomUUID().toString());
            }

            // 执行单个更新
            entityService.update(
                    mainService,
                    null,
                    Viewfields_.class,
                    field,
                    true);
        });
    }

    /**
     * 根据表名和语言批量更新Viewfields
     * 
     * @param tableName  表名
     * @param language   语言代码
     * @param updateData 更新数据（不包含ID）
     */
    public void batchUpdateViewfieldsByTable(String tableName, String language, Viewfields updateData) {
        // 1. 查询符合条件的字段
        List<Viewfields> fields = batchSelectViewfieldsByTable(
                Collections.singletonList(tableName),
                language);

        // 2. 应用更新数据
        fields.forEach(field -> {
            // 复制更新数据到实体
            if (updateData.getTableDesc() != null) {
                field.setTableDesc(updateData.getTableDesc());
            }
            if (updateData.getContent() != null) {
                field.setContent(updateData.getContent());
            }
        });

        // 3. 批量更新
        batchUpdateViewfields(fields);
    }

    public void deleteViewFieldsByTableAndLangu(String tableName, String langu) {
        // 使用entityService的delete方法
        Viewfields field = Viewfields.create();
        field.setTableName(tableName);
        field.setLangu(langu);
        entityService.delete(mainService, null, Viewfields_.class, field, true);
    }

    // ========== CDSViewFiles 操作方法 ==========

    /**
     * 插入CDSViewFiles实体
     * 
     * @param file CDSViewFiles实体（需包含fileName）
     */
    public void insertCDSViewFiles(CDSViewFiles file) {
        // 校验必填字段
        if (file.getFileName() == null || file.getFileName().isEmpty()) {
            throw new IllegalArgumentException("CDSViewFiles.fileName不能为空");
        }

        // 自动生成ID（如果未设置）
        if (file.getId() == null) {
            file.setId(UUID.randomUUID().toString());
        }

        try {
            // 调用entityService插入
            entityService.insert(
                    mainService,
                    null,
                    CDSViewFiles_.class,
                    file,
                    true);
        } catch (Exception e) {
            // TODO: handle exception
            throw new BusinessException("CDS视图上传失败543: " + e.getMessage(), e);
        }

    }

}
