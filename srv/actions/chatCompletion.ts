import { DeploymentApi } from "@sap-ai-sdk/ai-api";
import { __LargeString } from "@sap/cds";
import axios from "axios";

export const chatCompletionHandler = async function (this: any, req: any) {
  const {
    Tasks,
    BotInstances,
    ContextNodes,
    BotMessages,
    BotType,
    PromptText,
    ModelConfigs,
  } = this.entities;
  let insertResult, updateResult, firstChatFlag, functionResponse;

  /**
   * step1:  设置BotInstances的status为`RUNNING`
   */

  //获取当前Tasks
  const currentTasks = await SELECT.one
    .from(Tasks)
    .where({ ID: req.params[0] });

  //获取当前BotInstance
  const currentBotInstance = await SELECT.one
    .from(BotInstances)
    .where({ ID: req.params[1] });

  //更新状态为RUNNING
  updateResult = await batchDynamicUpdate(this, "BotInstances", [
    {
      keys: { ID: currentBotInstance.ID },
      fields: { status_code: "RUNNING" },
    },
  ]);

  /**
   * step2:  根据BotMessages中是否存在记录判定是否为首次调用：
   *         首次对话：  则获取BotTypes.prompts作为system消息，组合user消息发送给AI模型
   *         非首次对话：则获取历史消息，组合本次user消息发送给AI模型
   */

  //获取当前BotMessages历史记录列表
  const currentBotMessages = await SELECT.from(BotMessages).where({
    botInstance_ID: currentBotInstance.ID,
  });

  let currentBotTypes, currentSystemPrompt, currentUserContent;

  const userLanguage = getCurrentLanguage(req);

  currentBotTypes = await SELECT.one
    .from(BotType)
    .where({ ID: currentBotInstance.type_ID });

  //判定是否为首次调用
  if (currentBotMessages.length == 0) {
    firstChatFlag = true;

    currentSystemPrompt = await SELECT.one.from(PromptText).where({
      botType_ID: currentBotTypes.ID,
      lang_code: userLanguage,
    });

    if (currentSystemPrompt) {
      //替换当前系统提示词中的占位符
      currentSystemPrompt.content = await replacePlaceHolder(
        currentSystemPrompt.content,
        ContextNodes,
        currentTasks
      );
    } else {
      throw new Error(`System prompt not find`);
    }
  }

  //当前用户输入消息
  currentUserContent = req.data.content;

  let insertData = [];

  if (firstChatFlag) {
    insertData.push({
      role: "system",
      message: currentSystemPrompt.content,
      ragData: "",
      botInstance_ID: currentBotInstance.ID,
    });
  }

  insertData.push({
    role: "user",
    message: currentUserContent,
    ragData: "",
    botInstance_ID: currentBotInstance.ID,
  });

  insertResult = await batchDynamicInsert(this, "BotMessages", insertData);

  /**
   * step3:  根据BotTypes.model判定使用哪种AI模型，并调用对应AI模型的Completion
   */
  switch (currentBotTypes.functionType_code) {
    //AI_CHAT
    case "A":
      functionResponse = await chatWithAI(currentBotMessages, currentBotTypes);
      break;
    //FUNCTION CALL
    case "F":
      functionResponse = "";
      break;
    //CODE
    case "C":
      functionResponse = "";
      break;
    default:
      throw new Error(
        `Unkonw functionType: ${currentBotTypes.functionType_code}`
      );
  }

  /**
   * step4:  调用成功：将AI返回结果更新之BotMessages表中，并返回assistant消息，更新状态为`SUCCESS`
   *           首次对话：  更新systme、user、assistant
   *           非首次对话：更新user、assistant
   *         调用失败：返回错误消息，更新状态为`FAILED`
   */
  insertData = [];

  insertData.push({
    role: "assistant",
    message: functionResponse,
    ragData: "",
    botInstance_ID: currentBotInstance.ID,
  });

  insertResult = await batchDynamicInsert(this, "BotMessages", insertData);

  //更新状态为SUCCESS
  updateResult = await batchDynamicUpdate(this, "BotInstances", [
    {
      keys: { ID: currentBotInstance.ID },
      fields: { status_code: "SUCCESS" },
    },
  ]);
};
/**
 * 批量动态插入
 * @param   {Object} srv        - CAP服务实例
 * @param   {string} entityName - 实体名称
 * @param   {Array}  entries    - 插入数据数组，每个元素为要插入的记录对象
 * @returns                     - 插入结果数组
 */
async function batchDynamicInsert(
  srv: any,
  entityName: string,
  entries: any[]
) {
  try {
    const entity = srv.entities[entityName];
    if (!entity) {
      throw new Error(`Entity ${entityName} not found in service`);
    }

    // 验证插入数据不为空
    if (!entries || entries.length === 0) {
      throw new Error("No entries provided for insertion");
    }

    const results = [];

    // 循环单条插入
    for (let i = 0; i < entries.length; i++) {
      try {
        const entry = entries[i];

        // 单条插入
        const result = await srv.run(INSERT.into(entity).entries(entry));

        results.push(result);
      } catch (error) {
        throw error;
      }
    }

    return {
      success: true,
      result: results,
      count: entries.length,
    };
  } catch (error) {
    console.error(` ${entityName} insert error:`, error);
    throw error;
  }
}
/**
 * 批量动态更新
 * @param   {Object} srv        - CAP服务实例
 * @param   {string} entityName - 实体名称
 * @param   {Array}  updates    - 更新数组，每个元素包含 {keys: {}, fields: {}}
 * @returns                     - 更新结果数组
 */
async function batchDynamicUpdate(srv: any, entityName: any, updates: any) {
  try {
    const entity = srv.entities[entityName];
    if (!entity) {
      throw new Error(`Entity ${entityName} not found in service`);
    }

    const results = [];

    try {
      for (const update of updates) {
        const result = await srv.run(
          UPDATE(entity).set(update.fields).where(update.keys)
        );
        results.push(result);
      }
      return { success: true, results };
    } catch (error) {
      throw error;
    }
  } catch (error) {
    console.error("Batch dynamic update error:", error);
    throw error;
  }
}
/**
 * 获取用户当前语言
 * @param   {Object} req - CAP服务实例
 * @returns              - 当前语言
 */
function getCurrentLanguage(req: any) {
  const userLanguage = req.user?.locale || "EN";
  return userLanguage;
}
/**
 * 判定提示词中是否存在{{}}占位符：
 * 若存在：则根据{{}}中的路径值，从ContextNodes中根据TaskID + path获取对应的value值，替换提示词中的占位符
 * @param   {__LargeString} Prompt - 提示词
 * @param   {object} ContextNodes  - ContextNodes实体
 * @param   {object} currentTasks  - currentTasks实体
 * @returns                        - 提示词
 */
async function replacePlaceHolder(
  PromptContent: any,
  ContextNodes: any,
  currentTasks: any
) {
  const markers = extractTemplateMarkers(PromptContent);

  let contextNodeValue;
  for (const marker of markers) {
    contextNodeValue = await SELECT.one
      .from(ContextNodes)
      .where({ task_ID: currentTasks.ID })
      .columns("value");

    if (contextNodeValue) {
      PromptContent = PromptContent.replace(marker, contextNodeValue.value);
    }
  }

  return PromptContent;
}

/**
 * 提取字符串中所有 {{...}} 格式的模板标记
 * @param content - 要处理的字符串
 * @returns       - 包含所有匹配模板的数组
 */
function extractTemplateMarkers(content: any): string[] {
  const templateRegex = /\{\{([^{}]+)\}\}/g;
  const matches: string[] = [];

  let match;
  while ((match = templateRegex.exec(content)) !== null) {
    matches.push(match[0].trim());
  }

  return matches;
}
/**
 * 根据BotType的Model类型，调用对应的LLM
 * @param currentBotMessages - 对话上下文（历史记录）
 * @param currentBotType     - 对话Bot信息
 * @returns                  - AI返回的内容
 */
async function chatWithAI(
  currentBotMessages: any,
  currentBotType: any
): Promise<any> {
  let response;
  const currentModelConfig = await SELECT.one
    .from("ModelConfig")
    .where({ ID: currentBotType.model_ID });

  if (!currentModelConfig) {
    throw new Error(`AI Config not exits.`);
  }

  // const aiAPI = await import("@sap-ai-sdk/ai-api");

  const parameters_JSON = JSON.parse(currentModelConfig.parameters);

  const tokenResponse = await axios.post(
    parameters_JSON.url + "/oauth/token",
    new URLSearchParams({
      grant_type: "client_credentials",
      client_id: parameters_JSON.clientid,
      client_secret: parameters_JSON.clientsecret,
    }),
    {
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
    }
  );

  const accessToken = tokenResponse.data.access_token;

  const destination: any = {
    url: parameters_JSON.serviceurls.AI_API_URL,
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  };

  const { resources } = await DeploymentApi.deploymentQuery(
    {
      status: "RUNNING",
      executableIds: ["azure-openai", "aws-bedrock"],
      scenarioId: "foundation-models",
    },
    { "AI-Resource-Group": "default" }
  ).execute(destination);

  console.log("resources", resources);

  switch (currentModelConfig.modelName) {
    case "gpt-4o":
      break;
    case "gpt-4.1":
      break;
    case "claude-3.7-sonnet":
      break;
    case "claude-4-sonnet":
      break;
    case "gemini-2.5-pro":
      break;
    case "text-embedding-3-large":
      break;
    case "text-embedding-3-small":
      break;
    default:
      throw new Error(`This LLM model not support yet.`);
  }

  return response;
}
