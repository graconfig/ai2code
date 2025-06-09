import { executeresult } from "#cds-models/";
import { BotMessages, ContextNodes } from "#cds-models/MainService";
import { Bot } from "srv/model/Bot";

export interface BotService {
    getCurrentBot(botInstanceId: string): Bot;
    getCurrentBot(taskId: string, sequence: number): Bot;

    chat(context: any): BotMessages;
    chat(botInstanceId: string, content: string): BotMessages;

    // chatInStreaming(botInstanceId: string, content: string): SseEmitter;

    executeAsync(context: executeresult): boolean;
    executeAsync(botInstanceId: string): boolean;

    execute(context: executeresult): executeresult;
    execute(botInstanceId: string): executeresult;

    adopt(context: any): ContextNodes;
    adopt(botInstanceId: string, messageId: string): ContextNodes;
}
