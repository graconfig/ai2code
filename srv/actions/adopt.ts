export const adoptHandler = async function (this: any, req: any) {
     const {
      BotInstances, 
      BotMessages,
      BotType,
      ContextNodes
    } 
      = this.entities;

// 1. 获取当前BotMessage条目
  const botMessage= await SELECT.one
    .from(BotMessages)
    .where({ ID: req.params[1] });

//2.获取当前BotInstance
  const BotInstance = await SELECT.one
    .from(BotInstances)
    .where({ ID: botMessage.botInstance_ID }); 

// 3. 根据BotInstances.type获取BotTypes条目。
    const botTypes = await SELECT.one
    .from(BotType)
    .where({ ID: BotInstance.type_ID });

   try {
      // 4. 将这条消息内容存储到ContextNodes条目中。
        //  1. Path:  根据BotTypes设置的outputContextPath
        //  2. label:
        //  3. type: 根据BotTypes设置的contextType
        //  4. Value: BotMessages.message/根据AI function call转成相应的格式
  const contextNodeData = await this.run(
    INSERT({
      path: botTypes.outputContextPath,
      label: null,
      type: botTypes.contextType_code,
      value: botMessage.message,
      task_ID: BotInstance.task_ID
    }).into(ContextNodes)
  );
    // 5. 更新状态
        await UPDATE(BotInstances)
            .set({ status_code: 'SUCCESS' })
            .where({ ID: BotInstance.ID });
            
  // 6.返回ContextNodes条目
      return [contextNodeData];
 }catch (error) {
    // 错误处理：更新状态
        await UPDATE(BotInstances)
            .set({ status_code: 'SUCCESS' })
            .where({ ID: BotInstance.ID });
  }
};