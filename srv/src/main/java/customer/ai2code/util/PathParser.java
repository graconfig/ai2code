package customer.ai2code.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 上下文路径解析工具，处理带数组索引的路径
 */
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
}
