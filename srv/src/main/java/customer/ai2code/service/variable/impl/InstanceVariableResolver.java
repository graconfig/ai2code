package customer.ai2code.service.variable.impl;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sap.cds.ql.CdsName;

import cds.gen.mainservice.BotInstances;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.service.variable.VariableResolver;

/**
 * Instance 实例变量解析器 - 利用CdsName注解智能解析BotInstances属性
 */
@Component
public class InstanceVariableResolver implements VariableResolver {

    // 缓存方法映射，提高性能
    private static final Map<String, Method> methodCache = new HashMap<>();

    @Override
    public boolean supports(String variableExpression) {
        return variableExpression.startsWith("Instance:");
    }

    @Override
    public String resolve(String variableExpression, VariableContext context) {
        try {
            String propertyName = variableExpression.replace("Instance:", "");
            Object instance = context.getCurrentInstance();
            
            if (instance instanceof BotInstances) {
                return resolveBotInstanceProperty((BotInstances) instance, propertyName);
            }
            
            return "";
        } catch (Exception e) {
            System.err.println("Failed to resolve instance variable: " + variableExpression + ", error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public int getPriority() {
        return 2;
    }

    /**
     * 解析BotInstances属性 - 利用CdsName注解
     */
    private String resolveBotInstanceProperty(BotInstances botInstance, String propertyName) {
        if (botInstance == null || propertyName == null) {
            return "";
        }

        try {
            // 1. 首先尝试通过CDS常量直接匹配
            String cdsConstantValue = getCdsConstantValue(propertyName);
            if (cdsConstantValue != null) {
                Method method = findGetterMethodByCdsName(cdsConstantValue);
                if (method != null) {
                    Object value = method.invoke(botInstance);
                    return formatValue(value);
                }
            }

            // 2. 尝试直接通过属性名匹配方法
            Method directMethod = findDirectMethod(propertyName);
            if (directMethod != null) {
                Object value = directMethod.invoke(botInstance);
                return formatValue(value);
            }

            // 3. 尝试通过反射找到对应的getter方法
            Method reflectionMethod = findMethodByReflection(propertyName);
            if (reflectionMethod != null) {
                Object value = reflectionMethod.invoke(botInstance);
                return formatValue(value);
            }

            // 4. 特殊处理一些计算属性
            return resolveSpecialProperty(botInstance, propertyName);

        } catch (Exception e) {
            System.err.println("Failed to resolve BotInstances property: " + propertyName + ", error: " + e.getMessage());
            return "";
        }
    }

    /**
     * 通过CDS常量获取对应的CdsName值
     */
    private String getCdsConstantValue(String propertyName) {
        try {
            // 处理常见的属性名映射
            switch (propertyName.toLowerCase()) {
                case "id":
                    return BotInstances.ID;
                case "sequence":
                    return BotInstances.SEQUENCE;
                case "result":
                    return BotInstances.RESULT;
                case "type":
                    return BotInstances.TYPE;
                case "typeid":
                case "type_id":
                    return BotInstances.TYPE_ID;
                case "status":
                    return BotInstances.STATUS;
                case "statuscode":
                case "status_code":
                    return BotInstances.STATUS_CODE;
                case "task":
                    return BotInstances.TASK;
                case "taskid":
                case "task_id":
                    return BotInstances.TASK_ID;
                case "tasks":
                    return BotInstances.TASKS;
                case "messages":
                    return BotInstances.MESSAGES;
                case "createdat":
                case "created_at":
                    return BotInstances.CREATED_AT;
                case "createdby":
                case "created_by":
                    return BotInstances.CREATED_BY;
                case "modifiedat":
                case "modified_at":
                    return BotInstances.MODIFIED_AT;
                case "modifiedby":
                case "modified_by":
                    return BotInstances.MODIFIED_BY;
                default:
                    // 尝试通过反射获取常量值
                    try {
                        String constantName = propertyName.toUpperCase();
                        return (String) BotInstances.class.getField(constantName).get(null);
                    } catch (Exception e) {
                        return null;
                    }
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过CdsName注解查找getter方法（区分get和set方法）
     */
    private Method findGetterMethodByCdsName(String cdsName) {
        String cacheKey = "getter-cds:" + cdsName;
        if (methodCache.containsKey(cacheKey)) {
            return methodCache.get(cacheKey);
        }

        try {
            Method[] methods = BotInstances.class.getMethods();
            for (Method method : methods) {
                CdsName annotation = method.getAnnotation(CdsName.class);
                if (annotation != null && cdsName.equals(annotation.value())) {
                    // 检查是否是getter方法
                    if (isGetterMethod(method)) {
                        methodCache.put(cacheKey, method);
                        return method;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }

        methodCache.put(cacheKey, null);
        return null;
    }

    /**
     * 判断是否是getter方法
     */
    private boolean isGetterMethod(Method method) {
        String methodName = method.getName();
        
        // 检查方法名是否以get或is开头
        boolean hasGetterPrefix = methodName.startsWith("get") || methodName.startsWith("is");
        
        // 检查是否无参数
        boolean hasNoParameters = method.getParameterCount() == 0;
        
        // 检查是否有返回值
        boolean hasReturnValue = !method.getReturnType().equals(void.class);
        
        return hasGetterPrefix && hasNoParameters && hasReturnValue;
    }

    /**
     * 直接通过属性名查找方法
     */
    private Method findDirectMethod(String propertyName) {
        String cacheKey = "direct:" + propertyName;
        if (methodCache.containsKey(cacheKey)) {
            return methodCache.get(cacheKey);
        }

        try {
            // 尝试标准的getter方法名
            String getterName = "get" + capitalizeFirstLetter(propertyName);
            Method method = BotInstances.class.getMethod(getterName);
            if (isGetterMethod(method)) {
                methodCache.put(cacheKey, method);
                return method;
            }
        } catch (Exception e) {
            // 尝试boolean getter方法名
            try {
                String booleanGetterName = "is" + capitalizeFirstLetter(propertyName);
                Method method = BotInstances.class.getMethod(booleanGetterName);
                if (isGetterMethod(method)) {
                    methodCache.put(cacheKey, method);
                    return method;
                }
            } catch (Exception e2) {
                // 继续下面的逻辑
            }
        }

        methodCache.put(cacheKey, null);
        return null;
    }

    /**
     * 通过反射查找方法（模糊匹配）
     */
    private Method findMethodByReflection(String propertyName) {
        String cacheKey = "reflection:" + propertyName;
        if (methodCache.containsKey(cacheKey)) {
            return methodCache.get(cacheKey);
        }

        try {
            Method[] methods = BotInstances.class.getMethods();
            String lowerPropertyName = propertyName.toLowerCase();
            
            for (Method method : methods) {
                // 只考虑getter方法
                if (!isGetterMethod(method)) {
                    continue;
                }
                
                String methodName = method.getName().toLowerCase();
                
                // 匹配 getXxx 方法
                if (methodName.startsWith("get") && 
                    methodName.substring(3).equals(lowerPropertyName)) {
                    methodCache.put(cacheKey, method);
                    return method;
                }
                
                // 匹配 isXxx 方法
                if (methodName.startsWith("is") && 
                    methodName.substring(2).equals(lowerPropertyName)) {
                    methodCache.put(cacheKey, method);
                    return method;
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }

        methodCache.put(cacheKey, null);
        return null;
    }

    /**
     * 处理特殊属性（计算属性）
     */
    private String resolveSpecialProperty(BotInstances botInstance, String propertyName) {
        switch (propertyName.toLowerCase()) {
            case "taskscount":
                return String.valueOf(botInstance.getTasks() != null ? botInstance.getTasks().size() : 0);
            case "messagescount":
                return String.valueOf(botInstance.getMessages() != null ? botInstance.getMessages().size() : 0);
            case "typename":
                return botInstance.getType() != null ? 
                       safeToString(botInstance.getType().getName()) : 
                       safeToString(botInstance.getTypeId());
            case "statusname":
                return botInstance.getStatus() != null ? 
                       safeToString(botInstance.getStatus().getName()) : 
                       safeToString(botInstance.getStatusCode());
            case "taskname":
                return botInstance.getTask() != null ? 
                       safeToString(botInstance.getTask().getName()) : 
                       safeToString(botInstance.getTaskId());
            default:
                return "";
        }
    }

    /**
     * 格式化返回值
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        
        if (value instanceof Instant) {
            return formatInstant((Instant) value);
        }
        
        return value.toString();
    }

    /**
     * 格式化时间
     */
    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        // 可以根据需要自定义时间格式
        return instant.toString();
    }

    /**
     * 安全转换为字符串
     */
    private String safeToString(Object value) {
        return value != null ? value.toString() : "";
    }

    /**
     * 首字母大写
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}