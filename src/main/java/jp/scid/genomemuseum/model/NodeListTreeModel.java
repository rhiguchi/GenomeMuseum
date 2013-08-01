package jp.scid.genomemuseum.model;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeListTreeModel implements TreeModel {
    private final static Logger logger = LoggerFactory.getLogger(NodeListTreeModel.class);
    
    private final DefaultTreeModel delegate;
    
    private final Map<DefaultMutableTreeNode, ChildrenChangeHandler> changeHandlers;
    
    protected TreeSource treeSource;
    
    public NodeListTreeModel(DefaultTreeModel delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
        
        changeHandlers = new HashMap<DefaultMutableTreeNode, ChildrenChangeHandler>();
    }

    public NodeListTreeModel() {
        this(new DefaultTreeModel(null, true));
    }
    
    // treeModel
    public Object getRoot() {
        Object root = delegate.getRoot();
        logger.trace("NodeListTreeModel#getRoot: {}", root);
        
        return root;
    }

    public int getIndexOfChild(Object parent, Object child) {
        ensureChildrenRetrieved(parent);
        
        int index = delegate.getIndexOfChild(parent, child);
        logger.trace("NodeListTreeModel#getIndexOfChild: [{}, {}] -> {}", parent, child, index);
        
        return index;
    }

    public Object getChild(Object parent, int index) {
        ensureChildrenRetrieved(parent);

        Object child = delegate.getChild(parent, index);
        logger.trace("NodeListTreeModel#getChild: [{}, {}] -> {}", parent, index, child);
        
        return child;
    }

    public int getChildCount(Object parent) {
        ensureChildrenRetrieved(parent);
        
        int count = delegate.getChildCount(parent);
        logger.trace("NodeListTreeModel#getChildCount: {} -> {}", parent, count);
        
        return count;
    }

    public boolean isLeaf(Object node) {
        boolean leaf = delegate.isLeaf(node);
        logger.trace("NodeListTreeModel#isLeaf: {} -> {}", node, leaf);
        return leaf;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        Object[] objectPath = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObjectPath();
        boolean result = treeSource.updateValueForPath(objectPath, newValue);
        
        if (result) {
            delegate.valueForPathChanged(path, objectPath[objectPath.length - 1]);
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener listener) {
        TreeModelListenerProxy proxy = new TreeModelListenerProxy(listener);
        delegate.addTreeModelListener(proxy);
    }
    
    @Override
    public void removeTreeModelListener(TreeModelListener listener) {
        TreeModelListenerProxy proxy = findProxyListener(listener);
        if (proxy != null) {
            delegate.removeTreeModelListener(proxy);
        }
    }

    private TreeModelListenerProxy findProxyListener(TreeModelListener listener) {
        for (TreeModelListener l: delegate.getTreeModelListeners()) {
            if (((TreeModelListenerProxy) l).original.equals(listener)) {
                return (TreeModelListenerProxy) l;
            }
        }
        return null;
    }
    
    private void ensureChildrenRetrieved(Object parent) {
        if (changeHandlers.containsKey(parent)) {
            return;
        }
        logger.trace("NodeListTreeModel#ensureChildrenRetrieved: {}", parent);
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent;
        reloadChildren(node);
        // install a ChildrenChangeHandler
        ChildrenChangeHandler handler = new ChildrenChangeHandler(node);
        treeSource.addChildrenChangeListener(node.getUserObject(), handler);
        changeHandlers.put(node, handler);
    }

    protected DefaultMutableTreeNode createTreeNode(Object nodeObject, boolean allowsChildren) {
        return new DefaultMutableTreeNode(nodeObject, allowsChildren);
    }

    private MutableTreeNode createTreeNode(Object nodeObject) {
        boolean allowsChildren = getAllowsChildren(nodeObject);
        MutableTreeNode childNode = createTreeNode(nodeObject, allowsChildren);
        return childNode;
    }

    public TreePath getPathForRoot(TreeNode node) {
        TreeNode[] nodes = delegate.getPathToRoot(node);
        TreePath path = new TreePath(nodes);
        return path;
    }

    public TreePath getPathOfIndex(int[] indices) {
        Object root = getRoot();
        if (root == null) {
            throw new IllegalStateException("root must not be null");
        }
        
        TreePath path = new TreePath(root);
        for (int index: indices) {
            Object object = path.getLastPathComponent();
            int childCount = getChildCount(object);
            if (childCount <= index) {
                break;
            }
            
            Object child = getChild(object, index);
            path = path.pathByAddingChild(child);
        }
        return path;
    }
    
    // treeSource
    public TreeSource getTreeSource() {
        return treeSource;
    }
    
    public void setTreeSource(TreeSource treeSource) {
        this.treeSource = treeSource;
        
        // dispose handlers
        if (this.treeSource != null) for (Entry<DefaultMutableTreeNode, ChildrenChangeHandler> entry: changeHandlers.entrySet()) {
            treeSource.removeChildrenChangeListener(entry.getKey().getUserObject(), entry.getValue());
        }
        changeHandlers.clear();
        
        DefaultMutableTreeNode root;
        if (treeSource == null) {
            root = null;
        }
        else {
            root = createTreeNode(treeSource, true);
        }
        
        delegate.setRoot(root);
    }
    
    private boolean getAllowsChildren(Object nodeObject) {
        if (treeSource == null) {
            return false;
        }
        return treeSource.getAllowsChildren(nodeObject);
    }

    private void reloadChildren(DefaultMutableTreeNode node) {
        node.removeAllChildren();
        
        for (Object obj: treeSource.getChildren(node.getUserObject())) {
            node.add(createTreeNode(obj));
        }
    }
    
    private void removeHandlers(DefaultMutableTreeNode node) {
        for (Enumeration<?> it = node.children(); it.hasMoreElements(); ) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) it.nextElement();
            removeHandlers(child);
        }
        ChildrenChangeHandler handler = changeHandlers.get(node);
        if (handler != null) {
            treeSource.removeChildrenChangeListener(node.getUserObject(), handler);
        }
    }
    
    private void childInserted(Object element, MutableTreeNode parent, int index) {
        MutableTreeNode treeNode = createTreeNode(element);
        delegate.insertNodeInto(treeNode, parent, index);
    }
    
    private void childRemoved(MutableTreeNode parent, int index) {
        MutableTreeNode child = (MutableTreeNode) parent.getChildAt(index);
        removeHandlers((DefaultMutableTreeNode) child);
        delegate.removeNodeFromParent(child);
    }
    
    private void childChange(Object element, MutableTreeNode parent, int index) {
        MutableTreeNode childNode = (MutableTreeNode) parent.getChildAt(index);
        childNode.setUserObject(element);
        delegate.nodeChanged(childNode);
    }
    
    public static interface TreeSource {
        boolean getAllowsChildren(Object nodeObject);
        
        List<?> getChildren(Object parent);
        
        boolean updateValueForPath(Object[] path, Object value);
        
        void addChildrenChangeListener(Object nodeObject, ListDataListener l);
        
        void removeChildrenChangeListener(Object nodeObject, ListDataListener l);
    }
    
    private class ChildrenChangeHandler implements ListDataListener {
        private final DefaultMutableTreeNode node;

        public ChildrenChangeHandler(DefaultMutableTreeNode node) {
            this.node = node;
        }
        
        @Override
        public void intervalAdded(ListDataEvent e) {
            List<?> children = treeSource.getChildren(e.getSource());
            
            for (int index = e.getIndex0(); index <= e.getIndex1(); index++) {
                Object element = children.get(index);
                childInserted(element, node, index);
            }
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            for (int index = e.getIndex1(); index >= e.getIndex0(); index--) {
                childRemoved(node, index);
            }
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            List<?> children = treeSource.getChildren(e.getSource());

            for (int index = e.getIndex0(); index <= e.getIndex1(); index++) {
                Object element = children.get(index);
                childChange(element, node, index);
            }
        }
    }

    private class TreeModelListenerProxy implements TreeModelListener {
        private final TreeModelListener original;
        
        public TreeModelListenerProxy(TreeModelListener original) {
            this.original = original;
        }

        private TreeModelEvent wrapEvent(TreeModelEvent e) {
            TreeModelEvent realEvent =
                    new TreeModelEvent(NodeListTreeModel.this, e.getTreePath(), e.getChildIndices(), e.getChildren());
            return realEvent;
        }
        
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            original.treeNodesChanged(wrapEvent(e));
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            original.treeNodesInserted(wrapEvent(e));
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            original.treeNodesRemoved(wrapEvent(e));
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            original.treeStructureChanged(wrapEvent(e));
        }
    }
}
