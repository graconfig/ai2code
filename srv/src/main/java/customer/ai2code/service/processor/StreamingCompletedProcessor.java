package customer.ai2code.service.processor;
/**
 * 流式聊天完成处理器接口
 * 用于在流式聊天完成后执行自定义逻辑
 */
@FunctionalInterface
public interface StreamingCompletedProcessor {
    
    /**
     * 处理流式聊天完成事件
     * 
     * @param completeReply 完成的回复内容
     */
    void process(String completeReply);
}