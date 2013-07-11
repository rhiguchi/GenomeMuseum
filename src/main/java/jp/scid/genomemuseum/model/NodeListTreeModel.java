package jp.scid.genomemuseum.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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

import ca.odell.glazedlists.AbstractEventList;
import ca.odell.glazedlists.GlazedLists;

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
        logger.debug("NodeListTreeModel#getRoot: {}", root);
        
        return root;
    }

    public int getIndexOfChild(Object parent, Object child) {
        ensureChildrenRetrieved(parent);
        
        int index = delegate.getIndexOfChild(parent, child);
        logger.debug("NodeListTreeModel#getIndexOfChild: [{}, {}] -> {}", parent, child, index);
        
        return index;
    }

    public Object getChild(Object parent, int index) {
        ensureChildrenRetrieved(parent);

        Object child = delegate.getChild(parent, index);
        logger.debug("NodeListTreeModel#getChild: [{}, {}] -> {}", parent, index, child);
        
        return child;
    }

    public int getChildCount(Object parent) {
        ensureChildrenRetrieved(parent);
        
        int count = delegate.getChildCount(parent);
        logger.debug("NodeListTreeModel#getChildCount: {} -> {}", parent, count);
        
        return count;
    }

    public boolean isLeaf(Object node) {
        boolean leaf = delegate.isLeaf(node);
        logger.debug("NodeListTreeModel#isLeaf: {} -> {}", node, leaf);
        return leaf;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        delegate.valueForPathChanged(path, newValue);
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
        logger.debug("NodeListTreeModel#ensureChildrenRetrieved: {}", parent);
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent;
        ChildrenChangeHandler handler = new ChildrenChangeHandler(node);
        changeHandlers.put(node, handler);
        
        handler.setTreeSource(treeSource);
    }

    protected DefaultMutableTreeNode createTreeNode(Object nodeObject, boolean allowsChildren) {
        return new DefaultMutableTreeNode(nodeObject, allowsChildren);
    }

    private MutableTreeNode createTreeNode(Object nodeObject) {
        boolean allowsChildren = getAllowsChildren(nodeObject);
        MutableTreeNode childNode = createTreeNode(nodeObject, allowsChildren);
        return childNode;
    }

    public void reload(DefaultMutableTreeNode node) {
        List<?> children = treeSource.getChildren(node.getUserObject());
        List<Object> source = new ArrayList<Object>(children);
        
        TreeNodeChildList target = new TreeNodeChildList(node);
        
        GlazedLists.replaceAll(target, source, true);
    }
    
    class TreeNodeChildList extends AbstractEventList<Object> {
        private final DefaultMutableTreeNode parent;
        public TreeNodeChildList(DefaultMutableTreeNode parent) {
            this.parent = parent;
        }

        public void dispose() {
            // do nothing
        }

        @Override
        public int size() {
            return parent.getChildCount();
        }

        @Override
        public Object get(int index) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parent.getChildAt(index);
            return childNode.getUserObject();
        }
        
        @Override
        public Object set(int index, Object value) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parent.getChildAt(index);
            Object oldObject = get(index);
            childNode.setUserObject(value);
            delegate.nodeChanged(childNode);
            return oldObject;
        }
        
        @Override
        public void add(int index, Object value) {
            MutableTreeNode node = createTreeNode(value);
            delegate.insertNodeInto(node, parent, index);
        }
        
        @Override
        public Object remove(int index) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parent.getChildAt(index);
            Object oldObject = childNode.getUserObject();
            
            removeFromParent(childNode);
            
            return oldObject;
        }
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
        for (Iterator<? extends Entry<?, ChildrenChangeHandler>> it = changeHandlers.entrySet().iterator();
                it.hasNext();) {
            it.next().getValue().setTreeSource(null);
            it.remove();
        }
        
        MutableTreeNode root;
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
    
    private void removeFromParent(DefaultMutableTreeNode childNode) {
        delegate.removeNodeFromParent(childNode);
        
        // dispose handler
        ChildrenChangeHandler handler = changeHandlers.remove(childNode);
        if (handler != null) {
            handler.setTreeSource(null);
        }
    }

    public static interface TreeSource {
        boolean getAllowsChildren(Object nodeObject);
        
        List<?> getChildren(Object parent);
        
        void addChildrenChangeListener(Object parent, ChangeListener l);
        
        void removeChildrenChangeListener(Object parent, ChangeListener l);
    }
    
    private class ChildrenChangeHandler implements ChangeListener {
        private final DefaultMutableTreeNode node;
        private TreeSource treeSource;
        
        public ChildrenChangeHandler(DefaultMutableTreeNode node) {
            if (node == null) throw new IllegalArgumentException("node must not be null");
            this.node = node;
        }
        
        public void setTreeSource(TreeSource treeSource) {
            if (this.treeSource != null) {
                this.treeSource.removeChildrenChangeListener(node.getUserObject(), this);
            }
            
            this.treeSource = treeSource;
            
            if (treeSource != null) {
                treeSource.addChildrenChangeListener(node.getUserObject(), this);
                
                node.removeAllChildren();
                for (Object obj: treeSource.getChildren(node.getUserObject())) {
                    MutableTreeNode treeNode = createTreeNode(obj);
                    node.add(treeNode);
                }
            }
        }
        
        public void stateChanged(ChangeEvent e) {
            reload(node);
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
