package customer.ai2code.model.tree;

import cds.gen.mainservice.ContextNodes;
import java.util.ArrayList;
import java.util.List;

/**
 * 上下文树节点，封装 ContextNode 实体并表示树结构关系
 */
public class ContextTreeNode {
    private ContextNodes contextNode;
    private String path;
    private ContextTreeNode parent;
    private List<ContextTreeNode> children;

    public ContextTreeNode(ContextNodes contextNode) {
        this.contextNode = contextNode;
        this.path = contextNode.getPath();
        this.children = new ArrayList<>();
    }

    // Getters and Setters
    public ContextNodes getContextNode() { return contextNode; }
    public void setContextNode(ContextNodes contextNode) { this.contextNode = contextNode; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public ContextTreeNode getParent() { return parent; }
    public void setParent(ContextTreeNode parent) { this.parent = parent; }
    public List<ContextTreeNode> getChildren() { return children; }
    public void setChildren(List<ContextTreeNode> children) { this.children = children; }
    
    // 添加子节点
    public void addChild(ContextTreeNode child) {
        child.setParent(this);
        this.children.add(child);
    }
    
    // 判断是否为叶子节点
    public boolean isLeaf() {
        return children.isEmpty();
    }
}