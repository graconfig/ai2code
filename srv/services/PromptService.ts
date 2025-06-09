import { PromptTexts } from "#cds-models/ConfigService";

export interface PromptService {
    /**
     * 解析提示词
     * @param prompt 提示词
     * @param mainTaskId 主任务ID
     */
    parse(prompt: PromptTexts, mainTaskId: string): string;

    /**
     * 获取提示词列表
     * @param botTypeId bot类型ID
     * @param mainTaskId 主任务ID
     */
    getPrompts(botTypeId: string, mainTaskId: string): PromptTexts[];
}