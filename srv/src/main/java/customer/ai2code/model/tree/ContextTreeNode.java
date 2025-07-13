package customer.ai2code.model.tree;

import cds.gen.mainservice.ContextNodes;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 上下文树节点，封装 ContextNode 实体并表示树结构关系
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextTreeNode {
    @JsonIgnore
    private ContextNodes contextNode;
    private String path;
    
    @JsonIgnore  // 忽略parent属性，避免循环引用
    private ContextTreeNode parent;
    
    private List<ContextTreeNode> items;

    public ContextTreeNode(ContextNodes contextNode) {
        this.contextNode = contextNode;
        this.path = contextNode.getPath();
        this.items = new ArrayList<>();
    }

    // Getters and Setters
    public ContextNodes getContextNode() { return contextNode; }
    public void setContextNode(ContextNodes contextNode) { this.contextNode = contextNode; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public ContextTreeNode getParent() { return parent; }
    public void setParent(ContextTreeNode parent) { this.parent = parent; }
    public List<ContextTreeNode> getItems() { return items; }
    public void setItems(List<ContextTreeNode> items) { this.items = items; }
    
    // 添加子节点
    public void addChild(ContextTreeNode child) {
        child.setParent(this);
        this.items.add(child);
    }
    
    // 判断是否为叶子节点
    public boolean isLeaf() {
        return items.isEmpty();
    }
    
    // JSON序列化时需要的属性
    public String getId() {
        return contextNode != null ? contextNode.getId() : null;
    }
    
    public String getLabel() {
        return contextNode != null ? contextNode.getLabel() : generateLabelFromPath(path);
    }
    
    public String getType() {
        return contextNode != null ? contextNode.getType() : "virtual";
    }
    
    public String getValue() {
        return contextNode != null ? contextNode.getValue() : null;
    }
    
    public String getTaskId() {
        return contextNode != null ? contextNode.getTaskId() : null;
    }
    
    /**
     * 从路径生成标签
     */
    private String generateLabelFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "Root";
        }

        // 提取路径的最后一部分作为标签
        String[] parts = path.split("\\.");
        String lastPart = parts[parts.length - 1];

        // 移除数组索引
        // lastPart = lastPart.replaceAll("\\[\\d+\\]", "");

        // 首字母大写
        if (!lastPart.isEmpty()) {
            return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
        }

        return "Node";
    }
}