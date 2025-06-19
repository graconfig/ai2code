import { BotInstance, BotMessage } from "#cds-models/MainService";
import cds from "@sap/cds";
import { adoptHandler } from "./actions/adopt";
import { chatCompletionHandler } from "./actions/chatCompletion";
import { createTaskWithBotsHandler } from "./actions/createTaskWithBots";
import { executeHandler } from "./actions/execute";

export default class MainService extends cds.ApplicationService {
  init() {
    const { chatCompletion, execute } = BotInstance.actions;
    const { adopt } = BotMessage.actions;

    this.on(execute, (req) => executeHandler.call(this, req));

    this.on(chatCompletion, (req) => chatCompletionHandler.call(this, req));

    this.on(adopt, (req) => adoptHandler.call(this, req));

    return super.init();
  }
  async createTaskWithBots(name: any, description: any,typeId: any) {
    createTaskWithBotsHandler.call(this,name, description, typeId);
  }
}
