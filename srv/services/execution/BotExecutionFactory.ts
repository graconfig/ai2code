import { BotExecution } from "./BotExecution";

/**
 * BotExecutionFactory 函数式接口
 */
export type BotExecutionFactory = (
    botName: string,
    description: string,
    version: string,
    enabled: boolean
) => BotExecution;