import { PromptTexts } from "#cds-models/ConfigService";
import { BotMessages } from "#cds-models/MainService";
import { AIModel } from "srv/model/AIModel";
import { BotExecution } from "./execution/BotExecution";

/**
 * AI Service 接口
 */
export interface AIService {
    chatWithAI(
        messages: BotMessages[],
        prompts: PromptTexts[],
        content: string,
        model: AIModel
    ): string;

    // chatWithAIStreaming(
    //     messages: BotMessages[],
    //     prompts: PromptTexts[],
    //     content: string,
    //     model: AIModel,
    //     executor: ExecutorService, // 需自定义
    //     streamingCompletionProcessor: StreamingCompletedProcessor
    // ): SseEmitter;

    functionCalling<T extends BotExecution>(
        messages: BotMessages[],
        prompts: PromptTexts[],
        // functionCall: FunctionCalls, // 如需要可打开
        botExecutClazz: new (...args: any[]) => T,
        model: AIModel
    ): string;
}
