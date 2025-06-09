import { AIAuthConfig } from "./AIAuthConfig";

/**
 * 通用AI服务配置接口
 */
export interface AIServiceConfig {
    /**
     * 获取AI服务类型
     */
    getServiceType(): string;
    
    /**
     * 获取API端点URL
     */
    getApiUrl(): string;
    
    /**
     * 获取认证信息
     */
    getAuthConfig(): AIAuthConfig;
    
    /**
     * 检查配置是否有效
     */
    isValid(): boolean;
}
