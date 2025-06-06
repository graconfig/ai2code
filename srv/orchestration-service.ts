import { BotInstance, BotMessage } from "#cds-models/MainService";
import cds from "@sap/cds";
import { adoptHandler } from "./actions/adopt";
import { chatCompletionHandler } from "./actions/chatCompletion";
import { createTaskWithBotsHandler } from "./actions/createTaskWithBots";

export default class MainService extends cds.ApplicationService {
  init() {
    const { chatCompletion } = BotInstance.actions;
    const { adopt } = BotMessage.actions;

    this.on(chatCompletion, (req) => chatCompletionHandler.call(this, req));

    this.on(adopt, (req) => adoptHandler.call(this, req));

    return super.init();
  }
  async createTaskWithBots(req: any) {
    createTaskWithBotsHandler(req)
  }
}
