package customer.ai2code.service.impl;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.CdsVector;
import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnVector;

import cds.gen.configservice.ConfigService;
import cds.gen.configservice.ModelConfigs;
import cds.gen.configservice.ModelConfigs_;
import cds.gen.ai.orchestration.rag.*;
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
import cds.gen.mainservice.BusinessScenarios_;
import cds.gen.mainservice.Tasks;
import cds.gen.mainservice.Tasks_;
import cds.gen.mainservice.Viewfields;
import cds.gen.mainservice.Viewfields_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.ContextNodes_;
import cds.gen.mainservice.MainService;
import cds.gen.configservice.PromptTexts;
import cds.gen.configservice.PromptTexts_;

import javax.print.DocFlavor.STRING;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import customer.ai2code.exception.BusinessException;

@Service
public class GenericCqnService {

    private final MainService mainService;
    private final ConfigService configService;
    private final EntityService entityService;
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;

    private final TaskBotCacheManager cacheManager;

    public GenericCqnService(
            EntityService entityService,
            MainService mainService,
            ConfigService configService,
            TaskBotCacheManager cacheManager,
            ObjectMapper objectMapper,
            DataSource dataSource) {
        this.entityService = entityService;
        this.mainService = mainService;
        this.configService = configService;
        this.cacheManager = cacheManager;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    // 原有查询方法...
    public ModelConfigs getModelConfig(String modelConfigId) {
        CqnSelect select = Select.from(ModelConfigs_.class)
                .where(m -> m.ID().eq(modelConfigId));
        return entityService.selectSingle(configService, select, ModelConfigs.class,
                "ModelConfig not found: " + modelConfigId);
    }

    public BotInstances getBotInstanceById(String botInstanceId) {
        var select = Select.from(BotInstances_.class).where(b -> b.ID().eq(botInstanceId));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found: " + botInstanceId);
    }

    public BotInstances getBotInstanceByTaskAndSequence(String taskId, int sequence) {
        var select = Select.from(BotInstances_.class)
                .where(b -> b.task_ID().eq(taskId).and(b.sequence().eq(sequence)));
        return entityService.selectSingle(mainService, select, BotInstances.class,
                "BotInstance not found for task: " + taskId + ", sequence: " + sequence);
    }

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

    public Tasks getTaskById(String taskId) {
        var select = Select.from(Tasks_.class).where(t -> t.ID().eq(taskId));
        return entityService.selectSingle(mainService, select, Tasks.class,
                "Task not found: " + taskId);
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

    // 创建和插入方法 - 包含业务逻辑
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

    public BotInstances createAndInsertBotInstance(String taskId, BotTypes botType) {
        BotInstances botInstance = BotInstances.create();
        botInstance.setId(UUID.randomUUID().toString());
        botInstance.setSequence(botType.getSequence());
        botInstance.setTypeId(botType.getId());
        botInstance.setStatusCode("C"); // Created
        botInstance.setTaskId(taskId);

        entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
        return botInstance;
    }

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

    // 更新方法
    public void updateBotInstance(BotInstances botInstance) {
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }

    public void updateBotInstanceStatus(String botInstanceId, String statusCode) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        updateBotInstance(botInstance);
    }

    public void updateBotInstanceResult(String botInstanceId, String result) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setResult(result);
        updateBotInstance(botInstance);
    }

    public void updateBotInstanceStatusAndResult(String botInstanceId, String statusCode, String result) {
        BotInstances botInstance = getBotInstanceById(botInstanceId);
        botInstance.setStatusCode(statusCode);
        botInstance.setResult(result);
        updateBotInstance(botInstance);
    }

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

    // 更新ContextNode的业务方法
    public ContextNodes updateContextNodeValue(ContextNodes existingNode, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setModifiedAt(java.time.Instant.now());

        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
    }

    // 更新ContextNode的完整信息
    public ContextNodes updateContextNode(ContextNodes existingNode, String label, String type, String contextValue) {
        existingNode.setValue(contextValue);
        existingNode.setLabel(label);
        existingNode.setType(type);
        existingNode.setModifiedAt(java.time.Instant.now());

        entityService.update(mainService, null, ContextNodes_.class, existingNode, true);
        return existingNode;
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

    // public void insertContextNode(ContextNodes contextNode) {
    // entityService.insert(mainService, null, ContextNodes_.class, contextNode,
    // true);
    // }

    // 原有的简单插入方法保留，但标记为内部使用
    private void insertTask(Tasks task) {
        entityService.insert(mainService, null, Tasks_.class, task, true);
    }

    private void insertBotInstance(BotInstances botInstance) {
        entityService.insert(mainService, null, BotInstances_.class, botInstance, true);
    }

    private void insertContextNode(ContextNodes contextNode) {
        entityService.insert(mainService, null, ContextNodes_.class, contextNode, true);
    }

    /**
     * 根据BotType ID查询所有PromptTexts
     */
    public List<PromptTexts> getPromptTextsByBotType(String botTypeId) {
        var select = Select.from(PromptTexts_.class)
                .where(p -> p.botType_ID().eq(botTypeId));
        // .orderBy(p -> p.sequence().asc());
        return entityService.selectList(configService, select, PromptTexts.class);
    }

    /**
     * 根据path查询ContextNode（不指定taskId）
     */
    // public ContextNodes getContextNodeByPath(String contextPath) {
    // var select = Select.from(ContextNodes_.class)
    // .where(c -> c.path().eq(contextPath));
    // return entityService.selectSingle(mainService, select, ContextNodes.class,
    // "ContextNode not found for path: " + contextPath);
    // }

    /**
     * 根据botInstanceId获取主任务ID
     */
    public String getMainTaskId(String botInstanceId) {
        // 使用缓存管理器获取主任务ID
        String mainTaskId = cacheManager.getMainTaskId(botInstanceId);
        if (mainTaskId != null) {
            return mainTaskId;
        }

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
    public BotMessages createAndInsertBotMessage(String botInstanceId, String message, String ragContent,String role) {
        BotMessages botMessage = BotMessages.create();
        botMessage.setId(UUID.randomUUID().toString());
        botMessage.setBotInstanceId(botInstanceId);
        botMessage.setMessage(message);
        botMessage.setRagData(ragContent);
        botMessage.setRole(role);

        entityService.insert(mainService, null, BotMessages_.class, botMessage, true);
        return botMessage;
    }


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

    /**
     * 根据BotInstance ID获取输出上下文路径
     */
    public String getOutputContextPathByBotInstanceId(String botInstanceId) {
        // 1. 获取BotInstance
        BotInstances botInstance = getBotInstanceById(botInstanceId);

        // 2. 获取关联的BotType
        BotTypes botType = getBotTypeById(botInstance.getTypeId());

        // 3. 返回配置的输出路径
        return botType.getOutputContextPath();
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

    // /**
    // * 执行原生SQL查询，返回结果列表（Map形式）
    // */
    // public List<Map<String, Object>> execNativeSql(String sql) throws
    // SQLException {
    // try (Connection conn = dataSource.getConnection();
    // PreparedStatement stmt = conn.prepareStatement(sql);
    // ResultSet rs = stmt.executeQuery()) {

    // List<Map<String, Object>> results = new ArrayList<>();
    // ResultSetMetaData meta = rs.getMetaData();
    // int colCount = meta.getColumnCount();

    // while (rs.next()) {
    // Map<String, Object> row = new LinkedHashMap<>();
    // for (int i = 1; i <= colCount; i++) {
    // Object val = rs.getObject(i);
    // row.put(meta.getColumnLabel(i), val);
    // }
    // results.add(row);
    // }
    // return results;
    // }
    // }

    public String findMatchingViewsByScenario(String ragSource, int ragTopK, String query,
            Locale language, double threshold) {
        
        // 1.构建向量
        CqnVector vector = CQL.vector(query);
        var similarity = CQL.cosineSimilarity(CQL.get("embeddings"), vector);
        // 2.查询 BusinessScenarios 表，获取符合条件的场景
        CqnSelect selectScenario = Select.from(BusinessScenarios_.class)
                .columns(b -> b.get("scenario"), b -> b.get("description"), b -> b.get("viewCategory"),
                        b -> similarity.as("similarity"))
                .where(b -> similarity.gt(threshold))
                .orderBy(b -> similarity.desc())
                .limit(ragTopK);

        List<Row> scenarioRows = mainService.run(selectScenario).listOf(Row.class);
        if (scenarioRows.isEmpty()) {
            return "[]"; // 如果没有匹配的场景，返回空数组
        }
        // 3.提取 viewCategory 并展开
        Set<String> categories = new LinkedHashSet<>();
        for (Row row : scenarioRows) {
            String viewCategory = (String) row.get("viewCategory");
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
        CqnSelect selectViews = Select.from(CDSViews_.class)
                .columns("viewName", "viewDesc", "viewCategory")
                .where(c -> CQL.get("viewCategory").in(categories)
                        .and(CQL.get("isActive").eq(true)));

        List<Row> viewRows = mainService.run(selectViews).listOf(Row.class);

        // Step 5: 返回 JSON 格式
        return rowsToJson(viewRows);

    }

    public String findViewFieldsByViewNames(List<String> viewList, int ragTopK, Locale language) {

        CqnSelect select = Select.from(Viewfields_.class)
                .columns(f -> f.get("tableName"), f -> f.get("tableDesc"), f -> f.get("content"))
                .where(f -> f.get("category").in(viewList).and(f.get("langu").eq(language.getLanguage())))
                .limit(ragTopK);

        List<Row> rows = mainService.run(select).listOf(Row.class);
        return rowsToJson(rows);

    }

    public String findJoinConditionsByViewNames(List<String> viewList) {
        CqnSelect select = Select.from(RagJoinCond_.class)
                .columns(c -> c.get("tableFirst"), c -> c.get("tableSecond"), c -> c.get("tableJoin"))
                .where(c -> c.get("tableFirst").in(viewList).or(c.get("tableSecond").in(viewList)));

        List<Row> rows = mainService.run(select).listOf(Row.class);
        return rowsToJson(rows);
    }

    private String rowsToJson(List<Row> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Row row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String column : row.keySet()) {
                map.put(column, row.get(column));
            }
            result.add(map);
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "[{\"error\":\"Failed to convert result to JSON\"}]";
        }
    }

     // ========== 新增CDSViews插入方法 ==========
    /**
     * 插入CDSViews实体（基于viewName主键）
     * @param view CDSViews实体（需包含viewName）
     */
    public void insertCDSViews(CDSViews view) {
        // 校验必填字段（viewName）
        if (view.getViewName() == null || view.getViewName().isEmpty()) {
            throw new IllegalArgumentException("CDSViews.viewName不能为空");
        }
        
        entityService.insert(
            mainService,  // 使用MainService作为服务上下文
            null,         // 无事务上下文
            CDSViews_.class, 
            view, 
            true  // 自动生成审计字段（managed aspect）
        );
    }

    // ========== 新增Viewfields插入方法 ==========
    /**
     * 插入Viewfields实体（带ID主键）
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
            true
        );
    }

    // ========== 新增CDSViewFiles插入方法 ==========
    /**
     * 插入CDSViewFiles实体
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
        
        // 调用entityService插入
        entityService.insert(
            mainService,
            null,
            CDSViewFiles_.class,
            file,
            true
        );
    }

    // 在GenericCqnService中添加以下方法
    public void deleteCDSViewsByNames(List<String> viewNames) {
        if (viewNames == null || viewNames.isEmpty()) return;
        // 使用entityService的delete方法
        for (String viewName : viewNames) {
            CDSViews view = CDSViews.create();
            view.setViewName(viewName);
            entityService.delete(mainService, null, CDSViews_.class, view, true);
        }
    }

    public void deleteViewFieldsByTableAndLangu(String tableName, String langu) {
        // 使用entityService的delete方法
        Viewfields field = Viewfields.create();
        field.setTableName(tableName);
        field.setLangu(langu);
        entityService.delete(mainService, null, Viewfields_.class, field, true);
    }

}
