package customer.ai2code.service;

import java.util.List;

import cds.gen.configservice.PromptTexts;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.service.variable.VariableContext;

public interface PromptService {
    /**
     * 
     * @param prompt 提示词
     *               // * @param mainTaskId 主任务ID
     * @return
     */
    public String parse(PromptTexts prompt, VariableContext context);

    /**
     * 
     * @param botTypeId
     * @param mainTaskId
     * @return
     */
    public List<PromptTexts> getPrompts(Bot bot);

    public PromptTexts getRagAsPrompts(Bot bot, String query);

}
