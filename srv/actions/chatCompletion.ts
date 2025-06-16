import { AiDeploymentList, DeploymentApi } from "@sap-ai-sdk/ai-api";
import { __LargeString } from "@sap/cds";
import { AzureOpenAiChatClient } from "@sap-ai-sdk/foundation-models";
import axios from "axios";
import { HttpDestination } from "@sap-cloud-sdk/connectivity";
let tokenCache: { token: any, expiry: any } = {
  token: undefined,
  expiry: undefined
}
let aiModelIDCache: { modelName: any, ID: any }[] = [];

export const chatCompletionHandler = async function (this: any, req: any) {
  const {
    Tasks,
    BotInstances,
    ContextNodes,
    BotMessages,
    BotType,
    PromptText,
  } = this.entities;

  let functionResponse;

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
  await batchDynamicUpdate(this, "BotInstances", [
    {
      keys: {
        ID: currentBotInstance.ID
      },
      fields: {
        status_code: "RUNNING",
        modifiedAt: new Date().toISOString(),
      },
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
    role: { '!=': 'system' }
  });

  let currentBotTypes, currentSystemPrompt, currentUserContent;

  const userLanguage = getCurrentLanguage(req);

  //获取当前BotType信息
  currentBotTypes = await SELECT.one
    .from(BotType)
    .where({ ID: currentBotInstance.type_ID });

  //根据当前BotType获取对应的系统提示词
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

  //当前用户输入消息
  currentUserContent = req.data.content;

  let insertData = [];

  //首次调用时追加更新System消息
  if (currentBotMessages.length == 0) {
    insertData.push({
      role: "system",
      message: currentSystemPrompt.content,
      ragData: "",
      botInstance_ID: currentBotInstance.ID,
      createdAt: new Date().toISOString(),
      modifiedAt: new Date().toISOString(),
    });
  }

  insertData.push({
    role: "user",
    message: currentUserContent,
    ragData: "",
    botInstance_ID: currentBotInstance.ID,
    createdAt: new Date().toISOString(),
    modifiedAt: new Date().toISOString(),
  });

  await batchDynamicInsert(this, "BotMessages", insertData);

  /**
   * step3:  根据BotTypes.model判定使用哪种AI模型，并调用对应AI模型的Completion
   */
  switch (currentBotTypes.functionType_code) {
    //AI_CHAT
    case "A":
      functionResponse = await chatWithAI(currentSystemPrompt, currentBotMessages, currentUserContent, currentBotTypes);
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
    createdAt: new Date().toISOString(),
    modifiedAt: new Date().toISOString(),
  });

  //更新AI返回消息
  await batchDynamicInsert(this, "BotMessages", insertData);

  //更新状态为SUCCESS
  await batchDynamicUpdate(this, "BotInstances", [
    {
      keys: {
        ID: currentBotInstance.ID
      },
      fields: {
        status_code: "SUCCESS",
        modifiedAt: new Date().toISOString(),
      },
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
 * @param currentSystemPrompt - system提示词
 * @param currentBotMessages  - 对话上下文（历史记录）
 * @param currentUserContent  - 当前用户输入内容
 * @param currentBotType      - 对话Bot信息
 * @returns                   - AI返回的内容
 */
async function chatWithAI(
  currentSystemPrompt: any,
  currentBotMessages: any,
  currentUserContent: any,
  currentBotType: any
): Promise<any> {

  let
    // systemMessages_GPT: AzureOpenAiChatCompletionRequestSystemMessage[],
    // userMessages_GPT: AzureOpenAiChatCompletionRequestMessage[],
    // tools_GPT: AzureOpenAiChatCompletionTool[],
    response;

  //获取对话模型配置
  const currentModelConfig = await SELECT.one
    .from("ModelConfig")
    .where({ ID: currentBotType.model_ID });

  if (!currentModelConfig) {
    throw new Error(`AI Config not exits.`);
  }

  //JSON化配置信息
  const parameters_JSON = JSON.parse(currentModelConfig.parameters);

  //获取Destination
  const aiDestination = await getAICoreDestination(parameters_JSON);

  //获取可执行模型列表
  const resourceList = await getAiDeploymentList(aiDestination);

  //获取当前配置模型对应的DeploymentID
  const DeploymentID = resourceList.find(res => res.modelName === currentModelConfig.modelName)?.ID
  if (!DeploymentID) {
    throw new Error(`Model ${currentModelConfig.modelName} is unavailable in BTP`);
  }

  //设置系统消息
  const Messages_GPT = [{
    role: "system",
    content: [{
      type: "text",
      text: currentSystemPrompt.content
    }]
  }]

  //设置历史消息上下文
  for (const botMessage of currentBotMessages) {
    Messages_GPT.push({
      role: botMessage.role,
      content: [{
        type: "text",
        text: botMessage.message.trim().replace(/\n/g, " ")
      }]
    })
  }

  //追加本次用户消息
  Messages_GPT.push({
    role: 'user',
    content: [{
      type: "text",
      text: currentUserContent.trim().replace(/\n/g, " ")
    }]
  })

  //暂时没有tools的逻辑，后续应该会有
  let tools_GPT

  switch (currentModelConfig.modelName) {
    case "gpt-4o":
      response = await invokeGPTModel(aiDestination, DeploymentID, Messages_GPT, tools_GPT)
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
/**
 * 
 * @param parameters_JSON AI服务连接信息
 * @returns 
 */
async function getAICoreDestination(parameters_JSON: any) {
  let accessToken

  if (tokenCache.token && tokenCache.expiry > Date.now()) {
    accessToken = tokenCache.token
  } else {
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

    accessToken = tokenResponse.data.access_token;

    //暂存tokenCache，避免每次获取token
    tokenCache = {
      token: tokenResponse.data.access_token,
      expiry: Date.now() + (tokenResponse.data.expires_in - 3600) * 1000
    };
  }

  const destination: any = {
    url: parameters_JSON.serviceurls.AI_API_URL,
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  };

  return destination
}
/**
 * 调用GPT大模型
 * @param aiDestination   - AI Core连接目标
 * @param DeploymentID    - AI DeplotmentID
 * @param Messages_GPT    - 完整消息内容
 * @param tools           - tools
 */
async function invokeGPTModel(
  aiDestination: HttpDestination,
  DeploymentID: string,
  Messages_GPT: any,
  tools: any) {

  const requestPara = {
    messages: Messages_GPT,
    ...(tools != null && { tools })
  };

  const response = await new AzureOpenAiChatClient(
    {
      deploymentId: DeploymentID
    },
    aiDestination,
  ).run(requestPara);

  if (!response) {
    throw new Error("AI Model invoke failed");
  }

  return response.data.choices[0].message.content;
}
/**
 * 
 * @param aiDestination AI连接参数
 */
async function getAiDeploymentList(aiDestination: any) {
  if (aiModelIDCache.length > 0) {
    return aiModelIDCache
  } else {
    try {
      const resources: AiDeploymentList = await DeploymentApi.deploymentQuery(
        {
          status: "RUNNING",
          executableIds: ["azure-openai", "aws-bedrock"],
          scenarioId: "foundation-models",
        },
        { "AI-Resource-Group": "default" }
      ).execute(aiDestination);

      for (const resource of resources.resources) {
        aiModelIDCache.push({
          modelName: resource.configurationName,
          ID: resource.id
        })
      }

      return aiModelIDCache
    } catch (error) {
      throw error
    }
  }
}