export const createTaskWithBotsHandler = async function (
  this: any,
  req: any
) {
  const { BotType, Tasks } = this.entities;
  // 1. 查询TaskType的botTypes
  const BotTypes = await SELECT.from(BotType).where({ taskType_ID: req.data.typeId });
  // 2. 为每个BotType创建BotInstance
  const BotInstances = BotTypes.map((r: any) => ({
    sequence: r.sequence,
    status_code: "CREATED",
    type_ID: r.ID,
  }));
  // 3. 创建基础ContextNodes
  const BasicContextNode = [
    {
      path: "description",
      label: req.data.description,
    },
  ];
  // 4. 创建主Task
  const Maintask = await this.run(
    INSERT({
      name: req.data.name,
      description: req.data.description,
      isMain: true,
      contextPath: "",
      sequence: 0,
      type_ID: req.data.typeId,
      botInstances: BotInstances,
      contextNodes: BasicContextNode,
    }).into(Tasks)
  );

  // 5. 返回新建的Task对象
  return Maintask;
};
