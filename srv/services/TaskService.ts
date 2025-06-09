import { Task } from "srv/model/Task";
/**
 * 任务服务接口
 */
export interface TaskService {
  createTaskWithBots(context: any): Task;

  createTaskWithBots(
    name: string,
    description: string,
    taskTypeId: string
  ): Task;

  createTaskWithBots(
    botInstanceId: string,
    name: string,
    description: string,
    contextPath: string,
    sequence: number
  ): Task;

  createTaskWithBots(botInstanceId: string, name: string): Task;

  getCurrentTask(taskId: string): Task;

  getCurrentTask(botInstanceId: string, sequence: number): Task;
}
