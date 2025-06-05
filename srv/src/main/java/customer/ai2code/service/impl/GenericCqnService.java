package customer.ai2code.service.impl;

import org.springframework.stereotype.Service;

import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;

import cds.gen.configservice.ConfigService;
import cds.gen.configservice.ModelConfigs;
import cds.gen.configservice.ModelConfigs_;
import cds.gen.configservice.BotTypes;
import cds.gen.configservice.BotTypes_;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.MainService;

@Service
public class GenericCqnService {
    
    private final MainService mainService;
    private final ConfigService configService;
    private final EntityService entityService;

    public GenericCqnService(MainService mainService, ConfigService configService, EntityService entityService) {
        this.mainService = mainService;
        this.configService = configService;
        this.entityService = entityService;
    }

    public ModelConfigs getModelConfig(String modelConfigId) {
        // 从配置服务中获取模型配置
        // return configService.getModelConfigs().byId(modelConfigId).single();
        CqnSelect select = Select.from(ModelConfigs_.class)
                .where(m -> m.ID().eq(modelConfigId));
        return entityService.selectSingle(null, select, null, modelConfigId);
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

    public void updateBotInstance(BotInstances botInstance) {
        entityService.update(mainService, null, BotInstances_.class, botInstance, true);
    }
}
