package customer.ai2code.model.bot;

import java.util.Locale;

import cds.gen.configservice.BotTypes;
import cds.gen.mainservice.BotInstances;
import customer.ai2code.model.ai.config.model.AIModel;
import customer.ai2code.service.impl.GenericCqnService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bot接口的抽象实现类，提供基础实现和共享方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractBot implements Bot {

    protected BotInstances botInstance;
    protected AIModel aiModel;
    protected BotTypes botType;
    protected Locale locale;
    
    // 服务依赖
    protected GenericCqnService genericCqnService;
    
    /**
     * 更新Bot实例状态
     * @param statusCode 状态代码
     * @return 更新后的BotInstances
     */
    @Override
    public BotInstances updateStatus(String statusCode) {
        this.botInstance.setStatusCode(statusCode);
        genericCqnService.updateBotInstanceStatus(this.botInstance, statusCode);
        // 更新本地的botInstance引用
        this.botInstance = genericCqnService.getBotInstanceById(this.botInstance.getId());
        return this.botInstance;
    }
    
    /**
     * 更新Bot实例执行结果
     * @param result 执行结果
     * @return 更新后的BotInstances
     */
    @Override
    public BotInstances updateResult(String result) {
        this.botInstance.setResult(result);
        genericCqnService.updateBotInstanceResult(this.botInstance, result);
        // 更新本地的botInstance引用
        this.botInstance = genericCqnService.getBotInstanceById(this.botInstance.getId());
        return this.botInstance;
    }
    
    /**
     * 更新Bot实例关联的ContextNode ID
     * @param contextNodeId 上下文节点ID
     * @return 更新后的BotInstances
     */
    @Override
    public BotInstances updateContextNodeId(String contextNodeId) {
        this.botInstance.setContextID(contextNodeId);
        genericCqnService.updateBotInstanceContextNodeId(this.botInstance, contextNodeId);
        // 更新本地的botInstance引用
        this.botInstance = genericCqnService.getBotInstanceById(this.botInstance.getId());
        return this.botInstance;
    }
    
    /**
     * 更新Bot实例
     * @return 更新后的BotInstances
     */
    @Override
    public BotInstances update(BotInstances botInstance) {
        genericCqnService.updateBotInstance(botInstance);
        // 更新本地的botInstance引用
        this.botInstance = genericCqnService.getBotInstanceById(this.botInstance.getId());
        return this.botInstance;
    }
}
