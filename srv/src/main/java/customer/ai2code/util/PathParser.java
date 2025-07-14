package customer.ai2code.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 上下文路径解析工具，处理带数组索引的路径
 */
@Component // 添加Spring组件注解
public class PathParser {
    private static final Pattern ARRAY_PATTERN = Pattern.compile("(.*?)\\[(\\d+)\\]$");
    private static final Pattern PATH_SEGMENT_PATTERN = Pattern.compile("([^\\[\\]]+)(\\[\\d+\\])?");

    /**
     * 解析上下文路径为路径段数组
     * 例如："a.b[0].c" -> ["a", "b[0]", "c"]
     */
    public String[] parsePath(String path) {
        if (path == null || path.isEmpty()) {
            return new String[0];
        }
        
        // 先按点分割
        String[] segments = path.split("\\.");
        List<String> parsedSegments = new ArrayList<>();
        
        for (String segment : segments) {
            Matcher matcher = PATH_SEGMENT_PATTERN.matcher(segment);
            if (matcher.matches()) {
                String baseName = matcher.group(1);
                String arrayIndex = matcher.group(2);
                
                if (arrayIndex != null) {
                    parsedSegments.add(baseName + arrayIndex);
                } else {
                    parsedSegments.add(baseName);
                }
            } else {
                parsedSegments.add(segment);
            }
        }
        
        return parsedSegments.toArray(new String[0]);
    }

    /**
     * 获取路径的父路径
     * 例如："a.b[0].c" -> "a.b[0]"
     */
    public String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // 处理数组索引情况
        Matcher arrayMatcher = ARRAY_PATTERN.matcher(path);
        if (arrayMatcher.matches()) {
            String parentPath = arrayMatcher.group(1);
            if (!parentPath.isEmpty()) {
                return parentPath;
            }
        }
        
        // 处理普通路径
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return path.substring(0, lastDotIndex);
        }
        
        return null;
    }

    /**
     * 获取路径的所有层级
     * 例如："subtask[0].cView" -> ["subtask", "subtask[0]"]
     * 例如："subtask[0]" -> ["subtask"]
     */
    public List<String> getPathHierarchy(String path) {
        List<String> hierarchy = new ArrayList<>();
        String[] segments = path.split("\\.");
        
        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < segments.length - 1; i++) {
            if (currentPath.length() > 0) {
                currentPath.append(".");
            }
            currentPath.append(segments[i]);
            
            String pathStr = currentPath.toString();
            
            // 如果当前段包含数组索引，先创建不带索引的父节点
            if (pathStr.contains("[") && pathStr.contains("]")) {
                String basePathWithoutIndex = pathStr.replaceAll("\\[\\d+\\]", "");
                if (!basePathWithoutIndex.equals(pathStr) && !hierarchy.contains(basePathWithoutIndex)) {
                    hierarchy.add(basePathWithoutIndex); // 先添加基础路径
                }
            }
            
            // 然后添加当前路径
            hierarchy.add(pathStr);
        }
        
        // 特殊处理：如果原路径本身包含数组索引且没有子路径，也要创建父节点
        if (segments.length == 1 && path.contains("[") && path.contains("]")) {
            String basePathWithoutIndex = path.replaceAll("\\[\\d+\\]", "");
            if (!basePathWithoutIndex.equals(path)) {
                hierarchy.add(basePathWithoutIndex);
            }
        }
        
        return hierarchy;
    }
    
    /**
     * 获取直接父路径
     * 例如："subtask[0].cView" -> "subtask[0]"
     * 例如："subtask[0]" -> "subtask"
     */
    public String getImmediateParentPath(String path) {
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return path.substring(0, lastDotIndex);
        }
        
        // 特殊处理：如果是数组索引路径且没有点分隔符，返回不带索引的基础路径
        if (path.contains("[") && path.contains("]")) {
            String basePathWithoutIndex = path.replaceAll("\\[\\d+\\]", "");
            if (!basePathWithoutIndex.equals(path) && !basePathWithoutIndex.isEmpty()) {
                return basePathWithoutIndex;
            }
        }
        
        return null;
    }

    /**
     * 从路径生成标签
     */
    public String generateLabelFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "Root";
        }

        // 提取路径的最后一部分作为标签
        String[] parts = path.split("\\.");
        String lastPart = parts[parts.length - 1];

        // 移除数组索引
        // lastPart = lastPart.replaceAll("\\[\\d+\\]", "");

        // 移除双大括号
        lastPart = lastPart.replaceAll("\\{", "").replaceAll("\\}", "");

        // 首字母大写
        if (!lastPart.isEmpty()) {
            return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
        }

        return "Node";
    }
}