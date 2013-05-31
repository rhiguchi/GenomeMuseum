package jp.scid.genomemuseum.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ListModel;
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

public class NodeListTreeModel implements TreeModel {

    private final DefaultTreeModel delegate;
    
    private final Map<TreeNode, ChildrenSync> childrenSyncs;
    
    private final Map<TreeModelListener, TreeModelListenerProxy> listenerProxyMap;
    
    protected TreeSource treeSource;
    
    public NodeListTreeModel(DefaultTreeModel delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
        
        childrenSyncs = new HashMap<TreeNode, ChildrenSync>();
        listenerProxyMap = new HashMap<TreeModelListener, TreeModelListenerProxy>();
    }

    public NodeListTreeModel() {
        this(new DefaultTreeModel(null, true));
    }
    
    // treeModel
    public Object getRoot() {
        return delegate.getRoot();
    }

    public int getIndexOfChild(Object parent, Object child) {
        ensureChildrenRetrieved(parent);
        return delegate.getIndexOfChild(parent, child);
    }

    public Object getChild(Object parent, int index) {
        ensureChildrenRetrieved(parent);
        return delegate.getChild(parent, index);
    }

    public int getChildCount(Object parent) {
        ensureChildrenRetrieved(parent);
        return delegate.getChildCount(parent);
    }

    public boolean isLeaf(Object node) {
        return delegate.isLeaf(node);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        delegate.valueForPathChanged(path, newValue);
    }

    @Override
    public void addTreeModelListener(TreeModelListener listener) {
        TreeModelListenerProxy proxy = new TreeModelListenerProxy(listener);
        listenerProxyMap.put(listener, proxy);
        delegate.addTreeModelListener(proxy);
    }
    
    @Override
    public void removeTreeModelListener(TreeModelListener listener) {
        TreeModelListenerProxy proxy = listenerProxyMap.remove(listener);
        if (proxy != null) {
            delegate.removeTreeModelListener(proxy);
        }
    }
    
    private void ensureChildrenRetrieved(Object parent) {
        if (childrenSyncs.containsKey(parent)) {
            return;
        }
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent;
        ChildrenSync sync = new ChildrenSync(node);
        ListModel children = getChildren(node.getUserObject());
        sync.setListModel(children);
        
        childrenSyncs.put(node, sync);
    }

    protected DefaultMutableTreeNode createTreeNode(Object nodeObject, boolean allowsChildren) {
        return new DefaultMutableTreeNode(nodeObject, allowsChildren);
    }
    
    public TreePath getPathForRoot(TreeNode node) {
        TreeNode[] nodes = delegate.getPathToRoot(node);
        TreePath path = new TreePath(nodes);
        return path;
    }

    public TreePath getPathOfIndex(int[] indices) {
        TreePath path = new TreePath(getRoot());
        for (int index: indices) {
            Object child = getChild(path.getLastPathComponent(), index);
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
        for (Entry<?, ChildrenSync> entry: childrenSyncs.entrySet()) {
            entry.getValue().dispose();
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
    
    private ListModel getChildren(Object parent) {
        return treeSource.getChildren(parent);
    }
    
    public static interface TreeSource {
        boolean getAllowsChildren(Object nodeObject);
        
        ListModel getChildren(Object parent);
    }
    
    private class ChildrenSync implements ListDataListener {
        private final DefaultMutableTreeNode node;
        private ListModel model;
        
        public ChildrenSync(DefaultMutableTreeNode node) {
            if (node == null) throw new IllegalArgumentException("node must not be null");
            this.node = node;
        }

        public void dispose() {
            if (this.model != null) {
                this.model.removeListDataListener(this);
            }
        }

        public void setListModel(ListModel newModel) {
            if (this.model != null) {
                this.model.removeListDataListener(this);
            }
            this.model = newModel;
            reload();
            
            if (newModel != null) {
                newModel.addListDataListener(this);
            }
        }

        private void reload() {
            node.removeAllChildren();
            
            if (model != null) {
                insertChildren(model, 0, model.getSize());
            }
        }

        private void insertChildren(ListModel source, int start, int end) {
            for (int i = start; i < end; i++) {
                Object nodeObject = source.getElementAt(i);
                boolean allowsChildren = getAllowsChildren(nodeObject);
                MutableTreeNode childNode = createTreeNode(nodeObject, allowsChildren);
                
                node.insert(childNode, i);
            }
        }
        
        @Override
        public void intervalAdded(ListDataEvent e) {
            int start = e.getIndex0();
            int end = e.getIndex1();
            
            insertChildren((ListModel) e.getSource(), start, end + 1);
            int[] childIndices = range(start, end);
            delegate.nodesWereInserted(node, childIndices);
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            int start = e.getIndex0();
            int end = e.getIndex1();
            
            TreeNode[] removedChildren = new TreeNode[end - start + 1];
            
            for (int i = end; i >= start; i--) {
                removedChildren[i - start]  = node.getChildAt(i);
                node.remove(i);
            }

            int[] childIndices = range(start, end);
            delegate.nodesWereRemoved(node, childIndices, removedChildren);
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            ListModel source = (ListModel) e.getSource();
            int start = e.getIndex0();
            int end = e.getIndex1();
            
            for (int i = start; i <= end; i++) {
                Object newNodeObject = source.getElementAt(i);
                MutableTreeNode childNode = (MutableTreeNode) node.getChildAt(i);
                childNode.setUserObject(newNodeObject);
            }
            
            int[] childIndices = range(start, end);
            delegate.nodesChanged(node, childIndices);
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
    
    private static int[] range(int start, int end) {
        int[] childIndices = new int[end - start + 1];
        for (int i = 0; i < childIndices.length; i++) {
            childIndices[i] = i + start;
        }
        return childIndices;
    }
}
