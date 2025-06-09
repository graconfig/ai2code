import { executeresult } from "#cds-models/";
import { BotInstances } from "#cds-models/MainService";

import { AIModel } from "./AIModel";

export interface Bot {
  execute(): executeresult;

  executeAsync(): boolean;

  stop(): boolean;

  resume(): boolean;

  cancel(): boolean;

  // chatInStreaming(content: string): SseEmitter;

  chat(content: string): string;

  getBotInstance(): BotInstances;

  getAiModel(): AIModel;
}

