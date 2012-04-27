package jp.scid.genomemuseum.model.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public abstract class SourceTreeNode<E> implements MutableTreeNode, ListDataListener {
    final E nodeElement;
    
    protected Object userObject;
    
    protected MutableTreeNode parent = null;
    
    private boolean allowsChildren;
    
    protected SourceTreeModel<E> source = null;
    
    List<MutableTreeNode> children = null;
    
    public SourceTreeNode(E userObject, boolean allowsChildren) {
        this.userObject = userObject;
        this.nodeElement = userObject;
        this.allowsChildren = allowsChildren;
    }
    
    public SourceTreeNode() {
        this(null, true);
    }
    
    public SourceTreeModel<E> getTreeSource() {
        return source;
    }
    
    public void setTreeSource(SourceTreeModel<E> newsource) {
        if (this.source != null) {
            this.source.removeChildrenListener(nodeElement, this);
        }
        
        this.source = newsource;
        if (isChildrenPrepared() && getChildCount() > 0) {
            removeChildrenFromTree(0, getChildCount() - 1);
        }
        
        if (newsource != null) {
            newsource.addChildrenListener(nodeElement, this);
        }
    }

    // Parent
    @Override
    public MutableTreeNode getParent() {
        return parent;
    }
    
    // TreeNode implementations
    @Override
    public boolean getAllowsChildren() {
        return allowsChildren;
    }
    
    @Override
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }
    
    @Override
    public int getChildCount() {
        return getChildNodeList().size();
    }
    
    @Override
    public TreeNode getChildAt(int index) {
        return getChildNodeList().get(index);
    }
    
    @Override
    public int getIndex(TreeNode node) {
        return getChildNodeList().indexOf(node);
    }

    @Override
    public Enumeration<MutableTreeNode> children() {
        return Collections.enumeration(getChildNodeList());
    }
    
    // MutableTreeNode implementations
    @Override
    public void insert(MutableTreeNode newChild, int index) {
        children.add(index, newChild);
    }
    
    @Override
    public void remove(int index) {
        children.remove(index);
    }
    
    @Override
    public void remove(MutableTreeNode node) {
        children.remove(node);
    }
    
    @Override
    public void removeFromParent() {
        MutableTreeNode parent = getParent();
        if (parent != null) {
            getParent().remove(this);
            setParent(null);
        }
    }
    
    @Override
    public void setParent(MutableTreeNode newParent) {
        this.parent = newParent;
    }
    
    @Override
    public void setUserObject(Object object) {
        this.userObject = object;
    }
    
    public E getNodeElement() {
        return nodeElement;
    }
    
    boolean isChildrenPrepared() {
        return children != null;
    }
    
    void ensureChildrenLoaded() {
        if (!isChildrenPrepared())
            reloadChildren();
    }
    
    protected List<MutableTreeNode> getChildNodeList() {
        ensureChildrenLoaded();
        
        return children;
    }
    
    public void reloadChildren() {
        List<E> elements = source.getChildren(nodeElement);
        
        children = new ArrayList<MutableTreeNode>(elements.size());
        
        for (E userObject: elements) {
            addChild(children.size(), userObject);
        }
    }

    private TreeNode addChild(int index, E userObject) {
        boolean allowsChildren = getElementAllowsChildren(userObject);
        MutableTreeNode newChild = createChildTreeNode(userObject, allowsChildren);
        
        insert(newChild, index);
        return newChild;
    }
    
    @Override
    public void intervalAdded(ListDataEvent e) {
        int start = e.getIndex0();
        int end = e.getIndex1();
        
        for (int index = start; index <= end; index++) {
            E userObject = getChildNodeObject(index);
            TreeNode newChild = addChild(index, userObject);
            
            fireChildInserted(index, newChild);
        }
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        int start = e.getIndex0();
        int end = e.getIndex1();
        
        for (int index = end; index >= start; index--) {
            Object newObject = getChildNodeObject(index);
            MutableTreeNode node = (MutableTreeNode) getChildAt(index);
            
            node.setUserObject(newObject);
            
            fireChildChanged(index, node);
        }
    }
    
    @Override
    public void intervalRemoved(ListDataEvent e) {
        int start = e.getIndex0();
        int end = e.getIndex1();
        
        removeChildrenFromTree(start, end);
    }

    void removeChildrenFromTree(int start, int end) {
        for (int index = end; index >= start; index--) {
            TreeNode removedNode = children.remove(index);
            fireChildRemoved(index, removedNode);
        }
    }
    

    @Override
    public String toString() {
        return String.valueOf(userObject);
    }
    
    protected abstract void fireChildInserted(int index, Object node);
    
    protected abstract void fireChildChanged(int index, Object node);
    
    protected abstract void fireChildRemoved(int index, Object node);
    
    protected abstract MutableTreeNode createChildTreeNode(E userObject, boolean allowsChildren);
    
    E getChildNodeObject(int index) {
        return source.getChildren(nodeElement).get(index);
    }
    
    boolean getElementAllowsChildren(E element) {
        return !source.isLeaf(element);
    }
}
