import { ContextNodes } from "#cds-models/MainService";

export interface ContextService {
    buildContextAsHierarchy(
        contextNodes: ContextNodes[]
    ): Array<Record<string, any>>; // 等价于 List<Map<String, Object>>

    getContextFullPath(
        subPathPrefix: string,
        subPath: string
    ): string;

    upsertContext(
        taskId: string,
        contextPath: string,
        contextValue: string
    ): ContextNodes;

    getContextNode(
        contextNodeId: string
    ): ContextNodes;
}