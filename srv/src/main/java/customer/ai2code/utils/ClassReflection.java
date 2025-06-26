package customer.ai2code.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.common.reflect.ClassPath;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassReflection {
    public static List<Map<String, Object>> getClassbyInterface(Class interfaceClass)
            throws IOException {
        // List<Map<String, Object>> resultList =
        // ClassPath.from(ClassLoader.getSystemClassLoader())
        return ClassPath.from(ClassLoader.getSystemClassLoader())
                .getTopLevelClassesRecursive("customer")
                .stream()
                .filter(clazz -> {
                    try {
                        Class<?> loadedClass = clazz.load();
                        // 获取接口的完全限定名
                        String interfaceName = interfaceClass.getName();

                        // 检查直接实现的接口
                        for (Class<?> iface : loadedClass.getInterfaces()) {
                            if (iface.getName().equals(interfaceName)) {
                                return true;
                            }
                            // return true;
                        }

                        // 检查父类实现的接口（处理继承情况）
                        Class<?> superClass = loadedClass.getSuperclass();
                        while (superClass != null) {
                            for (Class<?> iface : superClass.getInterfaces()) {
                                if (iface.getName().equals(interfaceName)) {
                                    return true;
                                }
                            }
                            superClass = superClass.getSuperclass();
                        }

                        return false;
                    } catch (Exception e) {
                        System.err.println("Failed to load class: " + clazz.getName() + ", error: " +
                                e.getMessage());
                        return false;
                    }
                })
                .map(clazz -> {
                    try {
                        Class<?> loadedClass = clazz.load();
                        Map<String, Object> data = new HashMap<>();
                        data.put("Name", loadedClass.getName());
                        data.put("Description", loadedClass.getSimpleName());
                        // data.put("Package", loadedClass.getPackage().getName()); // 添加包名信息
                        return data;
                    } catch (Exception e) {
                        System.err.println(
                                "Failed to load class metadata: " + clazz.getName() + ", error: " +
                                        e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }
}
