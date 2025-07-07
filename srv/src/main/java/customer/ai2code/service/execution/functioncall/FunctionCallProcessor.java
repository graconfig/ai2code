package customer.ai2code.service.execution.functioncall;

import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.model.execution.functioncall.FunctionInfo;
import customer.ai2code.model.execution.functioncall.ParameterInfo;
import customer.ai2code.model.execution.functioncall.ParameterTypeInfo;
import customer.ai2code.service.execution.BotExecution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.Struct;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Component
public class FunctionCallProcessor {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    public FunctionCallProcessor(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    /**
     * 扫描并解析所有的 BotExecution 实现类
     * 
     * @return 函数信息列表
     */
    public List<FunctionInfo> extractFunctionInfos() {
        List<FunctionInfo> functionInfos = new ArrayList<>();

        // 获取所有实现了 BotExecution 接口的 Bean
        Map<String, BotExecution> botExecutors = applicationContext.getBeansOfType(BotExecution.class);

        for (Map.Entry<String, BotExecution> entry : botExecutors.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            BotExecutor botExecutor = clazz.getAnnotation(BotExecutor.class);

            if (botExecutor != null && botExecutor.enabled()) {
                functionInfos.addAll(extractFunctionInfosFromClass(clazz, botExecutor));
            }
        }

        return functionInfos;
    }

    /**
     * 从特定的 BotExecution 实例中提取函数信息
     */
    public <T extends BotExecution> List<FunctionInfo> extractFunctionInfosFromInstance(T botExecutionInstance) {
        Class<?> clazz = botExecutionInstance.getClass();
        BotExecutor botExecutor = clazz.getAnnotation(BotExecutor.class);

        if (botExecutor == null || !botExecutor.enabled()) {
            return Collections.emptyList();
        }

        return extractFunctionInfosFromClass(clazz, botExecutor);
    }

    /**
     * 从指定类中提取函数信息
     */
    private List<FunctionInfo> extractFunctionInfosFromClass(Class<?> clazz, BotExecutor botExecutor) {
        List<FunctionInfo> functionInfos = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            ExecuteMethod executeMethod = method.getAnnotation(ExecuteMethod.class);
            if (executeMethod != null) {
                FunctionInfo functionInfo = createFunctionInfo(method, executeMethod, botExecutor);
                functionInfos.add(functionInfo);
            }
        }

        return functionInfos;
    }

    /**
     * 创建函数信息对象
     */
    private FunctionInfo createFunctionInfo(Method method, ExecuteMethod executeMethod, BotExecutor botExecutor) {
        String functionName = executeMethod.operation().isEmpty() ? botExecutor.name() + "_" + method.getName()
                : executeMethod.operation();
        // 标准化函数名称，确保符合 AI 规范
        functionName = sanitizeFunctionName(functionName);

        String description = botExecutor.description().isEmpty() ? "Function from " + botExecutor.name()
                : botExecutor.description();

        List<ParameterInfo> parameters = extractParameterInfos(method);

        return FunctionInfo.builder()
                .name(functionName)
                .description(description)
                .parameters(parameters)
                .method(method)
                .targetClass(method.getDeclaringClass())
                .logExecution(executeMethod.logExecution())
                .build();
    }

    /**
     * 标准化函数名称，确保符合 OpenAI 规范
     * 只保留字母、数字、下划线、点和连字符
     */
    private String sanitizeFunctionName(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "unknown_function";
        }

        // 移除或替换不符合规范的字符
        String sanitized = originalName
                .replaceAll("[^a-zA-Z0-9_.-]", "_") // 将不符合规范的字符替换为下划线
                .replaceAll("_{2,}", "_") // 将连续的下划线替换为单个下划线
                .replaceAll("^_+|_+$", ""); // 移除开头和结尾的下划线

        // 确保函数名不为空
        if (sanitized.isEmpty()) {
            sanitized = "function_" + Math.abs(originalName.hashCode());
        }

        // 确保函数名以字母开头
        if (!sanitized.matches("^[a-zA-Z].*")) {
            sanitized = "func_" + sanitized;
        }

        return sanitized;
    }

    /**
     * 提取方法参数信息 - 增强版，支持复杂类型处理
     */
    private List<ParameterInfo> extractParameterInfos(Method method) {
        List<ParameterInfo> parameterInfos = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            ExecuteParameter executeParam = parameter.getAnnotation(ExecuteParameter.class);

            if (executeParam != null) {
                // 获取参数名
                String parameterName = getParameterNameSpringAIStyle(parameter, i);

                // 获取参数类型信息
                ParameterTypeInfo typeInfo = extractParameterTypeInfo(parameter);

                ParameterInfo paramInfo = ParameterInfo.builder()
                        .name(parameterName)
                        .required(executeParam.required())
                        .description(executeParam.description())
                        // 添加类型信息字段
                        .jsonSchemaType(typeInfo.getJsonSchemaType())

                        .type(parameter.getType())
                        .properties(typeInfo.getProperties())

                        .itemType(typeInfo.getItemType())
                        .itemProperties(typeInfo.getItemProperties())

                        .genericType(parameter.getParameterizedType())
                        .build();
                parameterInfos.add(paramInfo);
            }
        }

        return parameterInfos;
    }

    /**
     * 提取参数的详细类型信息
     */
    private ParameterTypeInfo extractParameterTypeInfo(Parameter parameter) {
        Class<?> parameterType = parameter.getType();
        Type genericType = parameter.getParameterizedType();

        var builder = ParameterTypeInfo.builder();

        System.out.println("Extracting parameter type info for: " + parameterType.getName() +
                ", genericType: " + genericType);

        // 处理基本类型
        if (parameterType == String.class) {
            builder.jsonSchemaType("string");
            System.out.println("Detected as string type");
        } else if (parameterType == Integer.class || parameterType == int.class) {
            builder.jsonSchemaType("integer");
            System.out.println("Detected as integer type");
        } else if (parameterType == Boolean.class || parameterType == boolean.class) {
            builder.jsonSchemaType("boolean");
            System.out.println("Detected as boolean type");
        } else if (parameterType == Double.class || parameterType == double.class ||
                parameterType == Float.class || parameterType == float.class) {
            builder.jsonSchemaType("number");
            System.out.println("Detected as number type");
        }
        // 处理 Collection 类型（包括 List, Set 等）
        else if (Collection.class.isAssignableFrom(parameterType)) {
            builder.jsonSchemaType("array");
            System.out.println("Detected as Collection/array type");

            // 提取泛型参数类型
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                System.out.println("Found " + actualTypeArguments.length + " generic type arguments");

                if (actualTypeArguments.length > 0) {
                    Type itemType = actualTypeArguments[0];
                    System.out.println("First generic type argument: " + itemType);

                    if (itemType instanceof Class) {
                        Class<?> itemClass = (Class<?>) itemType;
                        builder.itemType(itemClass);
                        System.out.println("Item class: " + itemClass.getName() +
                                ", isComplexType: " + isComplexType(itemClass));

                        // 如果是复杂对象（包括 CDS 接口），递归提取其属性
                        if (isComplexType(itemClass)) {
                            System.out.println("Extracting properties for complex item type: " + itemClass.getName());
                            Map<String, Object> itemProperties = extractObjectPropertiesWithDepth(itemClass, 0);
                            builder.itemProperties(itemProperties);
                            System.out.println("Extracted " + itemProperties.size() + " properties for item type");
                        } else {
                            System.out.println("Item type is simple, no properties to extract");
                        }
                    } else {
                        System.out.println("Item type is not a simple Class, type: " + itemType.getClass().getName());
                    }
                }
            } else {
                System.out.println("Generic type is not ParameterizedType, cannot extract item type");
            }
        }
        // 处理 Map 类型
        else if (Map.class.isAssignableFrom(parameterType)) {
            builder.jsonSchemaType("object");
            System.out.println("Detected as Map/object type");
        }
        // 处理复杂对象类型（包括 CDS 接口）
        else if (isComplexType(parameterType)) {
            builder.jsonSchemaType("object");
            System.out.println("Detected as complex object type: " + parameterType.getName());
            Map<String, Object> properties = extractObjectPropertiesWithDepth(parameterType, 0);
            builder.properties(properties);
            System.out.println("Extracted " + properties.size() + " properties for complex type");
        }
        // 默认处理为字符串
        else {
            builder.jsonSchemaType("string");
            System.out.println("Detected as default string type");
        }

        var result = builder.build();
        System.out.println("Final parameter type info - jsonSchemaType: " + result.getJsonSchemaType() +
                ", hasProperties: " + (result.getProperties() != null && !result.getProperties().isEmpty()) +
                ", hasItemProperties: "
                + (result.getItemProperties() != null && !result.getItemProperties().isEmpty()));
        return result;
    }

    /**
     * 判断是否为复杂类型（非基本类型、非Java内置类型）
     */
    private boolean isComplexType(Class<?> clazz) {
        boolean isPrimitive = clazz.isPrimitive();
        boolean isJavaLang = clazz.getName().startsWith("java.lang");
        boolean isJavaUtil = clazz.getName().startsWith("java.util");
        boolean isJavaTime = clazz.getName().startsWith("java.time");
        boolean isObject = clazz == Object.class;

        // 特别检查 CDS 生成的接口
        boolean isCdsInterface = clazz.isInterface() && clazz.getName().startsWith("cds.gen");

        boolean isComplex = !isPrimitive && !isJavaLang && !isJavaUtil && !isJavaTime && !isObject;

        System.out.println("Checking if complex type - class: " + clazz.getName() +
                ", isPrimitive: " + isPrimitive +
                ", isJavaLang: " + isJavaLang +
                ", isJavaUtil: " + isJavaUtil +
                ", isJavaTime: " + isJavaTime +
                ", isObject: " + isObject +
                ", isCdsInterface: " + isCdsInterface +
                ", isComplex: " + isComplex);

        return isComplex;
    }

    /**
     * 提取复杂对象的属性信息 - 带深度控制的版本，支持嵌套 CDS 接口解析
     */
    private Map<String, Object> extractObjectPropertiesWithDepth(Class<?> clazz, int depth) {
        // 防止无限递归，设置最大深度
        final int MAX_DEPTH = 5;
        if (depth > MAX_DEPTH) {
            System.out.println("Max depth reached for class: " + clazz.getName() + ", returning simple object type");
            return Map.of("type", "object", "description", "Complex nested object (depth limit reached)");
        }

        Map<String, Object> properties = new HashMap<>();

        try {
            // 检查是否为 CDS 生成的接口
            boolean isCdsInterface = clazz.isInterface() && clazz.getName().startsWith("cds.gen");

            System.out.println("Extracting properties for class: " + clazz.getName() +
                    ", isInterface: " + clazz.isInterface() +
                    ", isCdsInterface: " + isCdsInterface +
                    ", depth: " + depth);

            if (isCdsInterface) {
                // 对于 CDS 接口，主要通过 getter 方法提取属性
                System.out.println("Processing CDS interface, extracting from methods...");

                // 同时也提取字段信息（CDS 接口的字段通常是常量定义）
                extractFieldsFromCdsInterface(clazz, properties, depth + 1);

                extractPropertiesFromMethodsWithDepth(clazz, properties, depth + 1);

            } else {
                // 对于普通类，先处理字段
                extractPropertiesFromFieldsWithDepth(clazz, properties, depth + 1);

                // 然后检查 getter 方法（for Lombok generated getters）
                extractPropertiesFromMethodsWithDepth(clazz, properties, depth + 1);
            }

            System.out.println("Extracted " + properties.size() + " properties: " + properties.keySet());

        } catch (Exception e) {
            System.err.println(
                    "Failed to extract properties for class: " + clazz.getName() + ", error: " + e.getMessage());
        }

        return properties;
    }

    /**
     * 提取复杂对象的属性信息 - 保持原有接口兼容性
     */
    private Map<String, Object> extractObjectProperties(Class<?> clazz) {
        return extractObjectPropertiesWithDepth(clazz, 0);
    }

    /**
     * 从字段中提取属性信息（用于普通类）- 带深度控制
     */
    private void extractPropertiesFromFieldsWithDepth(Class<?> clazz, Map<String, Object> properties, int depth) {
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            // 跳过静态字段和 serialVersionUID
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    "serialVersionUID".equals(field.getName())) {
                continue;
            }

            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            Map<String, Object> fieldSchema = createFieldSchemaWithDepth(fieldType, field.getGenericType(), field,
                    depth);
            properties.put(fieldName, fieldSchema);
        }
    }

    /**
     * 从字段中提取属性信息（用于普通类）- 保持兼容性
     */
    private void extractPropertiesFromFields(Class<?> clazz, Map<String, Object> properties) {
        extractPropertiesFromFieldsWithDepth(clazz, properties, 0);
    }

    /**
     * 从方法中提取属性信息 - 带深度控制
     */
    private void extractPropertiesFromMethodsWithDepth(Class<?> clazz, Map<String, Object> properties, int depth) {
        Method[] methods = clazz.getDeclaredMethods();
        System.out.println(
                "Checking " + methods.length + " methods in class: " + clazz.getName() + " at depth: " + depth);

        for (Method method : methods) {
            if (isGetterMethod(method)) {
                String propertyName = getPropertyNameFromGetter(method);
                System.out.println("Found getter method: " + method.getName() + " -> property: " + propertyName);

                if (!properties.containsKey(propertyName)) {
                    Class<?> returnType = method.getReturnType();
                    Map<String, Object> propertySchema = createFieldSchemaWithDepth(returnType,
                            method.getGenericReturnType(), null, depth);
                    properties.put(propertyName, propertySchema);
                } else {
                    System.out.println("Property " + propertyName + " already exists, skipping...");
                }
            }
        }
    }

    /**
     * 从方法中提取属性信息 - 保持兼容性
     */
    private void extractPropertiesFromMethods(Class<?> clazz, Map<String, Object> properties) {
        extractPropertiesFromMethodsWithDepth(clazz, properties, 0);
    }

    /**
     * 从 CDS 接口的字段中提取常量定义
     */
    private void extractFieldsFromCdsInterface(Class<?> clazz, Map<String, Object> properties, int depth) {
        Method[] methods = clazz.getDeclaredMethods();
        System.out.println(
                "Checking " + methods.length + " methods in class: " + clazz.getName() + " at depth: " + depth);

        for (Method method : methods) {
            if (isGetterMethod(method)) {
                String propertyName = getPropertyNameFromAnnotation(method);
                System.out.println("Found getter method: " + method.getName() + " -> property: " + propertyName);

                if (!properties.containsKey(propertyName)) {
                    Class<?> returnType = method.getReturnType();
                    Map<String, Object> propertySchema = createFieldSchemaWithDepth(returnType,
                            method.getGenericReturnType(), null, depth);
                    properties.put(propertyName, propertySchema);
                } else {
                    System.out.println("Property " + propertyName + " already exists, skipping...");
                }
            }
        }
    }

    /**
     * 创建字段的 schema 信息 - 带深度控制，支持嵌套 CDS 接口解析
     */
    private Map<String, Object> createFieldSchemaWithDepth(Class<?> fieldType, Type genericType,
            java.lang.reflect.Field field, int depth) {
        Map<String, Object> fieldSchema = new HashMap<>();

        // 设置字段类型
        if (fieldType == String.class) {
            fieldSchema.put("type", "string");
        } else if (fieldType == Integer.class || fieldType == int.class) {
            fieldSchema.put("type", "integer");
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            fieldSchema.put("type", "boolean");
        } else if (fieldType == Double.class || fieldType == double.class ||
                fieldType == Float.class || fieldType == float.class) {
            fieldSchema.put("type", "number");
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            fieldSchema.put("type", "array");

            // 处理 Collection 的泛型类型
            if (genericType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericType;
                Type[] actualTypes = paramType.getActualTypeArguments();
                if (actualTypes.length > 0 && actualTypes[0] instanceof Class) {
                    Class<?> itemClass = (Class<?>) actualTypes[0];
                    if (isComplexType(itemClass)) {
                        // 递归解析嵌套的复杂类型（包括 CDS 接口）
                        Map<String, Object> itemProperties = extractObjectPropertiesWithDepth(itemClass, depth + 1);
                        fieldSchema.put("items", Map.of("type", "object", "properties", itemProperties));
                    } else {
                        fieldSchema.put("items", Map.of("type", getJsonSchemaType(itemClass)));
                    }
                }
            }
        } else if (isComplexType(fieldType)) {
            fieldSchema.put("type", "object");

            // 对于复杂类型（包括嵌套的 CDS 接口），递归解析其属性
            Map<String, Object> nestedProperties = extractObjectPropertiesWithDepth(fieldType, depth + 1);
            if (!nestedProperties.isEmpty()) {
                fieldSchema.put("properties", nestedProperties);
            }
        } else {
            fieldSchema.put("type", "string");
        }

        // 添加字段描述（如果有注解的话）
        if (field != null) {
            addFieldDescription(field, fieldSchema);
        }

        return fieldSchema;
    }

    /**
     * 创建字段的 schema 信息 - 保持兼容性
     */
    private Map<String, Object> createFieldSchema(Class<?> fieldType, Type genericType, java.lang.reflect.Field field) {
        return createFieldSchemaWithDepth(fieldType, genericType, field, 0);
    }

    /**
     * 为字段添加描述信息
     */
    private void addFieldDescription(java.lang.reflect.Field field, Map<String, Object> fieldSchema) {
        // 检查是否有 Jackson 注解
        com.fasterxml.jackson.annotation.JsonProperty jsonProperty = field
                .getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
            fieldSchema.put("description", jsonProperty.value());
        }

        // 可以添加其他注解的处理，比如 @Schema, @ApiModelProperty 等
    }

    /**
     * 判断是否为 getter 方法
     */
    private boolean isGetterMethod(Method method) {
        return method.getName().startsWith("get") &&
                method.getParameterCount() == 0 &&
                method.getReturnType() != void.class &&
                !java.lang.reflect.Modifier.isStatic(method.getModifiers());
    }

    /**
     * 从 getter 方法名提取属性名
     */
    private String getPropertyNameFromGetter(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            String propertyName = methodName.substring(3);
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return methodName;
    }

    /**
     * 从 getter 方法名提取属性名
     */
    private String getPropertyNameFromAnnotation(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            String propertyName = methodName.substring(3);
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return methodName;
    }

    /**
     * 获取 JSON Schema 类型
     */
    private String getJsonSchemaType(Class<?> clazz) {
        if (clazz == String.class) {
            return "string";
        } else if (clazz == Integer.class || clazz == int.class) {
            return "integer";
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return "boolean";
        } else if (clazz == Double.class || clazz == double.class ||
                clazz == Float.class || clazz == float.class) {
            return "number";
        } else if (List.class.isAssignableFrom(clazz)) {
            return "array";
        } else if (Map.class.isAssignableFrom(clazz) || isComplexType(clazz)) {
            return "object";
        } else {
            return "string";
        }
    }

    /**
     * 获取参数名 - 优先使用注解的 name 属性
     */
    private String getParameterNameSpringAIStyle(Parameter parameter, int parameterIndex) {
        ExecuteParameter executeParam = parameter.getAnnotation(ExecuteParameter.class);

        // 1. 优先使用注解的 name 属性
        if (executeParam != null) {
            String annotationName = executeParam.name();
            if (annotationName != null && !annotationName.trim().isEmpty()) {
                return annotationName;
            }
        }

        // 2. 如果注解没有 name 属性或为空，使用反射获取参数名
        if (parameter.isNamePresent()) {
            return parameter.getName();
        }

        // 3. 最后使用位置索引
        return "arg" + parameterIndex;
    }

    /**
     * 执行指定的函数调用
     */
    public Object executeFunctionCall(String functionName, Map<String, Object> arguments) throws Exception {
        Optional<FunctionInfo> functionInfoOpt = findFunctionInfo(functionName);
        if (!functionInfoOpt.isPresent()) {
            throw new IllegalArgumentException("Function not found: " + functionName);
        }

        FunctionInfo functionInfo = functionInfoOpt.get();
        Object targetInstance = applicationContext.getBean(functionInfo.getTargetClass());

        // 4. 获取目标方法
        Method method = functionInfo.getMethod();
        // 准备方法参数
        Object[] methodArgs = prepareMethodArguments(method, arguments);

        // 执行方法
        if (functionInfo.isLogExecution()) {
            // TODO: 添加日志记录
        }

        return functionInfo.getMethod().invoke(targetInstance, methodArgs);
    }

    /**
     * 准备方法参数 - 修复版本，正确处理复杂类型转换
     */
    private Object[] prepareMethodArguments(Method method, Map<String, Object> argumentsMap) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            ExecuteParameter executeParam = parameter.getAnnotation(ExecuteParameter.class);

            if (executeParam != null) {
                String paramName = executeParam.name();
                Object argumentValue = argumentsMap.get(paramName);

                // 转换参数类型
                args[i] = convertArgumentToParameterType(argumentValue, parameter);
            }
        }

        return args;
    }

    /**
     * 将参数值转换为正确的参数类型
     */
    private Object convertArgumentToParameterType(Object argumentValue, Parameter parameter) {
        if (argumentValue == null) {
            return null;
        }

        Class<?> parameterType = parameter.getType();
        Type genericType = parameter.getParameterizedType();

        try {
            // 处理 List 类型
            if (List.class.isAssignableFrom(parameterType)) {
                return convertToList(argumentValue, genericType);
            }

            // 处理基本类型
            if (parameterType == String.class) {
                return argumentValue.toString();
            } else if (parameterType == Integer.class || parameterType == int.class) {
                if (argumentValue instanceof Number) {
                    return ((Number) argumentValue).intValue();
                } else {
                    return Integer.parseInt(argumentValue.toString());
                }
            } else if (parameterType == Boolean.class || parameterType == boolean.class) {
                if (argumentValue instanceof Boolean) {
                    return argumentValue;
                } else {
                    return Boolean.parseBoolean(argumentValue.toString());
                }
            } else if (parameterType == Double.class || parameterType == double.class) {
                if (argumentValue instanceof Number) {
                    return ((Number) argumentValue).doubleValue();
                } else {
                    return Double.parseDouble(argumentValue.toString());
                }
            } else if (parameterType instanceof Class) {
                return convertToSpecificType(argumentValue, parameterType);
            }

            // 如果参数类型就是期望的类型，直接返回
            if (parameterType.isAssignableFrom(argumentValue.getClass())) {
                return argumentValue;
            }

            // 处理复杂对象类型
            return objectMapper.convertValue(argumentValue, parameterType);

        } catch (Exception e) {
            throw new BusinessException("Failed to convert argument to parameter type: " + parameterType.getName(), e);
        }
    }

    /**
     * 转换为 List 类型，正确处理泛型
     */
    private List<?> convertToList(Object argumentValue, Type genericType) {
        if (!(argumentValue instanceof List)) {
            throw new BusinessException("Expected List type but got: " + argumentValue.getClass().getName());
        }

        @SuppressWarnings("unchecked")
        List<Object> rawList = (List<Object>) argumentValue;

        // 获取泛型参数类型
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (actualTypeArguments.length > 0) {
                Type itemType = actualTypeArguments[0];

                if (itemType instanceof Class) {
                    Class<?> itemClass = (Class<?>) itemType;

                    // 转换列表中的每个元素
                    List<Object> convertedList = new ArrayList<>();
                    for (Object item : rawList) {
                        Object convertedItem = convertToSpecificType(item, itemClass);
                        convertedList.add(convertedItem);
                    }

                    return convertedList;
                }
            }
        }

        // 如果无法确定泛型类型，返回原始列表
        return rawList;
    }

    /**
     * 转换为特定类型
     */
    private Object convertToSpecificType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // 如果已经是目标类型，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // 处理基本类型
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                return Double.parseDouble(value.toString());
            }
        } else if (targetType.getName().contains("cds.gen")) {
            // 处理 CDS 生成的接口类型
            // 先使用 Struct.create 创建一个实例
            Object structInstance = Struct.create(targetType);

            // 使用 ObjectMapper 将 value 的数据转换并填充到 structInstance 中
            try {
                // 先将 value 转换为 JSON 字符串
                String jsonString = objectMapper.writeValueAsString(value);

                // 再将 JSON 字符串反序列化到 structInstance 中
                // 注意：这里需要更新 structInstance 的内容，而不是创建新对象
                objectMapper.readerForUpdating(structInstance).readValue(jsonString);

                return structInstance;
            } catch (Exception e) {
                throw new BusinessException("Failed to convert value to CDS Struct type: " + targetType.getName() +
                        ", value: " + value, e);
            }
        }

        // 处理复杂对象类型 - 使用 Jackson 进行转换
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            throw new BusinessException("Failed to convert value to type: " + targetType.getName() +
                    ", value: " + value + ", valueType: " + value.getClass().getName(), e);
        }
    }

    /**
     * 在特定实例上执行函数调用
     * 
     * @param functionName         函数名
     * @param argumentsJson        参数数据
     * @param botExecutionInstance Bot 执行实例
     * @return 执行结果
     */
    public <T extends BotExecution> Object executeFunctionCallOnInstance(String functionName,
            String argumentsJson,
            T botExecutionInstance) throws Exception {
        // 从实例中查找对应的函数信息
        List<FunctionInfo> functionInfos = extractFunctionInfosFromInstance(botExecutionInstance);
        Optional<FunctionInfo> functionInfoOpt = functionInfos.stream()
                .filter(info -> info.getName().equals(functionName))
                .findFirst();

        if (!functionInfoOpt.isPresent()) {
            throw new IllegalArgumentException("Function not found in instance: " + functionName);
        }

        FunctionInfo functionInfo = functionInfoOpt.get();

        Map<String, Object> arguments;
        try {
            arguments = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BusinessException("Failed to parse function arguments: " + argumentsJson, e);
        }

        // 准备方法参数
        // 4. 获取目标方法
        Method method = functionInfo.getMethod();

        Object[] methodArgs = prepareMethodArguments(method, arguments);

        // 在指定实例上执行方法
        if (functionInfo.isLogExecution()) {
            // TODO: 添加日志记录
            System.out.println("Executing function: " + functionName + " on instance: " +
                    botExecutionInstance.getClass().getSimpleName());
        }

        return functionInfo.getMethod().invoke(botExecutionInstance, methodArgs);
    }

    /**
     * 根据函数名查找对应的函数信息
     */
    public Optional<FunctionInfo> findFunctionInfo(String functionName) {
        return extractFunctionInfos().stream()
                .filter(info -> info.getName().equals(functionName))
                .findFirst();
    }
}