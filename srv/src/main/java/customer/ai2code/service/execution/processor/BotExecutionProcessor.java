package customer.ai2code.service.execution.processor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class BotExecutionProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 跳过第一轮编译，等注解类编译完成
        if (roundEnv.processingOver()) {
            return false;
        }
        
        try {
            // 尝试加载注解类，如果失败说明还没编译完成
            Class.forName("customer.ai2code.service.execution.annotation.BotExecutor");
            Class.forName("customer.ai2code.service.execution.annotation.ExecuteMethod");
            Class.forName("customer.ai2code.service.execution.annotation.ExecuteParameter");
        } catch (ClassNotFoundException e) {
            // 注解类还没编译完成，跳过这轮处理
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE, 
                "Annotation classes not ready, skipping validation..."
            );
            return false;
        }
        
        // 动态导入注解类
        validateBotExecutionClasses(roundEnv);
        return false;
    }

    private void validateBotExecutionClasses(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement classElement = (TypeElement) element;
                
                if (implementsBotExecution(classElement)) {
                    validateBotExecutionClass(classElement);
                }
            }
        }
    }

    private boolean implementsBotExecution(TypeElement classElement) {
        for (javax.lang.model.type.TypeMirror interfaceType : classElement.getInterfaces()) {
            if (interfaceType.toString().contains("BotExecution")) {
                return true;
            }
        }
        return false;
    }

    private void validateBotExecutionClass(TypeElement classElement) {
        // 使用反射检查注解
        try {
            String className = classElement.getQualifiedName().toString();
            Class<?> clazz = Class.forName(className);
            
            // 检查@BotExecutor注解
            if (!clazz.isAnnotationPresent(
                Class.forName("customer.ai2code.service.execution.annotation.BotExecutor").asSubclass(java.lang.annotation.Annotation.class)
            )) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "❌ Class implementing BotExecution must be annotated with @BotExecutor",
                    classElement
                );
            }
            
            // 检查execute方法
            validateExecuteMethod(classElement, clazz);
            
        } catch (ClassNotFoundException e) {
            // 类还没编译完成，跳过
        }
    }

    private void validateExecuteMethod(TypeElement classElement, Class<?> clazz) {
        boolean hasValidExecuteMethod = false;
        
        try {
            Class<?> executeMethodAnnotation = Class.forName("customer.ai2code.service.execution.annotation.ExecuteMethod");
            Class<?> executeParameterAnnotation = Class.forName("customer.ai2code.service.execution.annotation.ExecuteParameter");
            
            for (Element enclosedElement : classElement.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) enclosedElement;
                    
                    if ("execute".equals(method.getSimpleName().toString())) {
                        // 检查方法注解
                        java.lang.reflect.Method javaMethod = findJavaMethod(clazz, method);
                        if (javaMethod != null && javaMethod.isAnnotationPresent(executeMethodAnnotation.asSubclass(java.lang.annotation.Annotation.class))) {
                            
                            // 检查参数注解
                            boolean allParametersValid = validateParameters(method, javaMethod, executeParameterAnnotation, classElement);
                            
                            if (allParametersValid) {
                                hasValidExecuteMethod = true;
                            }
                            
                        } else {
                            processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "❌ execute method must be annotated with @ExecuteMethod",
                                method
                            );
                        }
                    }
                }
            }
            
            if (!hasValidExecuteMethod) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "❌ Class implementing BotExecution must have a valid execute method",
                    classElement
                );
            }
            
        } catch (ClassNotFoundException e) {
            // 注解类还没加载完成
        }
    }

    private boolean validateParameters(ExecutableElement method, java.lang.reflect.Method javaMethod, 
                                     Class<?> executeParameterAnnotation, TypeElement classElement) {
        boolean allParametersValid = true;
        
        // 获取参数信息
        java.lang.reflect.Parameter[] javaParams = javaMethod.getParameters();
        java.util.List<? extends VariableElement> elementParams = method.getParameters();
        
        if (javaParams.length != elementParams.size()) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "❌ Parameter count mismatch in execute method",
                method
            );
            return false;
        }
        
        // 检查每个参数
        for (int i = 0; i < javaParams.length; i++) {
            java.lang.reflect.Parameter javaParam = javaParams[i];
            VariableElement elementParam = elementParams.get(i);
            
            // 检查参数是否有@ExecuteParameter注解
            if (!javaParam.isAnnotationPresent(executeParameterAnnotation.asSubclass(java.lang.annotation.Annotation.class))) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "❌ Parameter '" + elementParam.getSimpleName() + 
                    "' (index " + i + ") must be annotated with @ExecuteParameter",
                    elementParam
                );
                allParametersValid = false;
            } else {
                // 检查注解的属性值
                validateParameterAnnotation(javaParam, elementParam, executeParameterAnnotation);
            }
        }
        
        return allParametersValid;
    }

    private void validateParameterAnnotation(java.lang.reflect.Parameter javaParam, VariableElement elementParam, 
                                           Class<?> executeParameterAnnotation) {
        try {
            // 获取注解实例
            java.lang.annotation.Annotation annotation = javaParam.getAnnotation(executeParameterAnnotation.asSubclass(java.lang.annotation.Annotation.class));
            
            if (annotation != null) {
                // 使用反射检查注解属性
                // java.lang.reflect.Method nameMethod = executeParameterAnnotation.getMethod("name");
                java.lang.reflect.Method requiredMethod = executeParameterAnnotation.getMethod("required");
                java.lang.reflect.Method descriptionMethod = executeParameterAnnotation.getMethod("description");
                
                // String name = (String) nameMethod.invoke(annotation);
                boolean required = (Boolean) requiredMethod.invoke(annotation);
                String description = (String) descriptionMethod.invoke(annotation);
                
                // 验证注解属性
              
                if (description == null || description.trim().isEmpty()) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "⚠️  @ExecuteParameter 'description' is empty for parameter: " + elementParam.getSimpleName(),
                        elementParam
                    );
                }
                
                // 输出参数验证成功信息
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "✅ Parameter '" + elementParam.getSimpleName() + "' validation passed: " +
                    "'required=" + required + ", type=" + javaParam.getType().getSimpleName()
                );
            }
            
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "❌ Error validating @ExecuteParameter annotation for parameter: " + elementParam.getSimpleName() + 
                " - " + e.getMessage(),
                elementParam
            );
        }
    }

    private java.lang.reflect.Method findJavaMethod(Class<?> clazz, ExecutableElement method) {
        try {
            // 获取参数类型
            java.util.List<? extends VariableElement> params = method.getParameters();
            Class<?>[] paramTypes = new Class[params.size()];
            
            for (int i = 0; i < params.size(); i++) {
                VariableElement param = params.get(i);
                String typeName = param.asType().toString();
                
                // 处理基本类型和常见类型
                paramTypes[i] = getClassFromTypeName(typeName);
            }
            
            return clazz.getDeclaredMethod(method.getSimpleName().toString(), paramTypes);
            
        } catch (Exception e) {
            // 如果精确匹配失败，使用名称匹配
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(method.getSimpleName().toString()) && 
                    m.getParameterCount() == method.getParameters().size()) {
                    return m;
                }
            }
            return null;
        }
    }

    private Class<?> getClassFromTypeName(String typeName) {
        try {
            // 处理基本类型
            switch (typeName) {
                case "java.lang.String": return String.class;
                case "java.lang.Integer": return Integer.class;
                case "java.lang.Boolean": return Boolean.class;
                case "java.lang.Double": return Double.class;
                case "java.lang.Long": return Long.class;
                case "int": return int.class;
                case "boolean": return boolean.class;
                case "double": return double.class;
                case "long": return long.class;
                default:
                    return Class.forName(typeName);
            }
        } catch (ClassNotFoundException e) {
            return Object.class; // 默认返回Object
        }
    }
}