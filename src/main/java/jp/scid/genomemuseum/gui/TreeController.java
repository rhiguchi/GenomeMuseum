package jp.scid.genomemuseum.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.application.AbstractBean;

abstract class TreeController<E> extends AbstractBean implements TreeWillExpandListener, TreeSelectionListener {
    private final int expandPathCount = 2;
    
    // view models
    private final TransferHandler transferHandler = null;
    
    private final TreeSelectionModel selectionModel;
    
    private final DefaultTreeModel treeModel;
    
    // Model
    private TreeSource treeSource = null;
    
    private boolean canRemove = true;
    private boolean canAdd = true;
    
    // property
    private List<TreePath> selections = Collections.emptyList();
    
    boolean emptySelectionAllowed = false;
    
    // sub controller
    private TreePathEditor treePathEditor = null;
    
    public TreeController() {
        treeModel = createTreeModel();
        
        selectionModel = createSelectionModel();
    }
    
    // bindings
    public void installTo(JTree tree) {
        tree.setRootVisible(false);
        tree.setModel(getTreeModel());
        tree.setSelectionModel(getSelectionModel());
        tree.addTreeSelectionListener(this);
        
        updateExpansion(tree);
        tree.addTreeWillExpandListener(this);

        tree.setTransferHandler(getTransferHandler());
    }
    
    public Object getRootObject() {
        return treeSource;
    }
    
    protected DefaultTreeModel createTreeModel() {
        return new DefaultTreeModel(null, true);
    }
    
    // properties 
    /**
     * Returns the TreeSource
     * @return {@link TreeSource} object
     */
    public TreeSource getTreeSource() {
        return treeSource;
    }
    
    public void setTreeSource(TreeSource treeSource) {
        this.treeSource = treeSource;
        
        reload();
    }
    
    public TreePath getTreePath(int[] indices) {
        TreePath path = new TreePath(treeModel.getRoot());
        
        for (int index: indices) {
            Object child = treeModel.getChild(path.getLastPathComponent(), index);
            path = path.pathByAddingChild(child);
        }
        return path;
    }
    
    // remove
    public boolean canRemove() {
        return canRemove;
    }
    
    public void setCanRemove(boolean newValue) {
        firePropertyChange("canRemove", this.canRemove, this.canRemove = newValue);
    }
    
    public void remove() {
        for (TreePath path: getSelectionPaths()) {
            remove(path);
        }
    }
    
    public void remove(TreePath path) {
        MutableTreeNode lastNode = (MutableTreeNode) path.getLastPathComponent();
        treeModel.removeNodeFromParent(lastNode);
    }
    
    public void remove(MutableTreeNode node) {
        treeModel.removeNodeFromParent(node);
    }
    
    // add
    public boolean canAdd() {
        return canAdd;
    }
    
    public void setCanAdd(boolean newValue) {
        firePropertyChange("canAdd", this.canAdd, this.canAdd = newValue);
    }
    
    public TreePath add() {
        Object nodeObject = createElement();
        return add(nodeObject);
    }

    public TreePath add(Object nodeObject) {
        MutableTreeNode target = getSelectedTreeNode();
        if (target == null) {
            target = (MutableTreeNode) treeModel.getRoot();
        }
        return add(nodeObject, target);
    }

    public TreePath add(Object nodeObject, MutableTreeNode targetNode) {
        if (targetNode == null) throw new IllegalArgumentException("targetNode must not be null");
        
        MutableTreeNode parentNode;
        int insertIndex;
        if (targetNode.getAllowsChildren()) {
            parentNode = targetNode;
            insertIndex = targetNode.getChildCount();
        }
        else {
            parentNode = (DefaultMutableTreeNode) targetNode.getParent();
            insertIndex = parentNode.getIndex(targetNode);
        }
        
        return add(nodeObject, parentNode, insertIndex);
    }
    
    public TreePath add(Object nodeObject, MutableTreeNode parent, int insertIndex) {
        if (parent == null) throw new IllegalArgumentException("targetNode must not be null");
        
        MutableTreeNode newNode = createTreeNode(nodeObject);
        treeModel.insertNodeInto(newNode, parent, insertIndex);
        Object[] pathToRoot = treeModel.getPathToRoot(newNode);
        TreePath path = new TreePath(pathToRoot);
        
        setSelectionPath(path);
        
        return path;
    }
    
    protected DefaultMutableTreeNode createTreeNode(Object child) {
        boolean allowsChildren = getAllowsChildren(child);
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child, allowsChildren);
        return childNode;
    }
    
    // children reloading
    public void reload() {
        Object rootObject = getRootObject();
        DefaultMutableTreeNode rootNode = createTreeNode(rootObject);
        reloadChildren(rootNode);
        treeModel.setRoot(rootNode);
    }
    
    public void reload(TreeNode node) {
        reloadChildren((DefaultMutableTreeNode) node);
        treeModel.nodeStructureChanged(node);
    }

    private void reloadChildren(DefaultMutableTreeNode parent) {
        parent.removeAllChildren();
        
        Collection<?> children = getChildren(parent.getUserObject());

        for (Object child: children) {
            DefaultMutableTreeNode childNode = createTreeNode(child);
            parent.add(childNode);
        }
    }

    // children
    protected boolean getAllowsChildren(Object nodeObject) {
        if (treeSource == null || nodeObject == null) {
            return false;
        }
        else if (nodeObject == treeSource) {
            return true;
        }
        return !treeSource.isLeaf(nodeObject);
    }

    protected Collection<?> getChildren(Object parentObject) {
        List<?> children;
        
        if (treeSource == null) {
            children = Collections.emptyList();
        }
        else if (parentObject == treeSource) {
            children = treeSource.children(null);
        }
        else {
            children = treeSource.children(parentObject);
        }

        return children;
    }

    // element selection
    public TreePath getSelectionPath() {
        if (selections.isEmpty()) {
            return null;
        }
        return selections.get(0);
    }
    
    public List<TreePath> getSelectionPaths() {
        return selections;
    }
    
    public void setSelectionPaths(Collection<TreePath> selections) {
        firePropertyChange("selections", this.selections,
                this.selections = new LinkedList<TreePath>(selections));
    }
    
    public void setSelectionPath(TreePath path) {
        selectionModel.setSelectionPath(path);
    }

    public boolean isSelectable(Object element) {
        return element != null;
    }
    
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        JTree tree = (JTree) e.getSource();
        TreePath[] paths = tree.getSelectionPaths();
        
        if (paths == null || paths.length == 0) {
            if (emptySelectionAllowed) {
                setSelectionPath(null);
            }
            else {
                setSelectionPath(getSelectionPath());
            }
        }
        else {
            setSelectionPaths(Arrays.asList(paths));
        }
    }
    
    protected TreeSelectionModel createSelectionModel() {
        return new SelectableSelectionModel();
    }

    protected E createElement() {
        throw new UnsupportedOperationException("must implement to create element");
    }
    
    public TransferHandler getTransferHandler() {
        return transferHandler;
    }
    
    // selection
    public TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public DefaultMutableTreeNode getSelectedTreeNode() {
        TreePath path = selectionModel.getSelectionPath();
        if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            return (DefaultMutableTreeNode) path.getLastPathComponent();
        }
        
        return null;
    }
    
    public TreeModel getTreeModel() {
        return treeModel;
    }

    // editor
    public void setTreePathEditor(TreePathEditor treePathEditor) {
        this.treePathEditor = treePathEditor;
    }
    
    public void startEditingAtPath(TreePath path) {
        if (treePathEditor == null) {
            return;
        }
        treePathEditor.startEditingAtPath(path);
    }

    // expansion
    public boolean canCollapse(TreePath path) {
        return path.getPathCount() > expandPathCount;
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        TreePath path = event.getPath();

        if (!canCollapse(path))
            throw new ExpandVetoException(event);
    }

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        TreePath path = event.getPath();

        if (path.getLastPathComponent() instanceof TreeNode) {
            TreeNode node = (TreeNode) path.getLastPathComponent();
            reload(node);
        }
    }

    void updateExpansion(JTree tree) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            if (!canCollapse(tree.getPathForRow(row)) && tree.isCollapsed(row)) {
                tree.expandRow(row);
            }
        }
    }
    
    // binding
    public void bindTree(JTree tree) {
        tree.setRootVisible(false);
        tree.setModel(getTreeModel());
        tree.setSelectionModel(getSelectionModel());
        
        updateExpansion(tree);
        tree.addTreeWillExpandListener(this);

        if (transferHandler != null) {
            tree.setTransferHandler(transferHandler);
            tree.setDragEnabled(true);
            tree.setDropMode(DropMode.ON);
        }
    }
    
    static interface TreeSource {
        List<?> children(Object parent);
        boolean isLeaf(Object node);
    }

    static class DefaultTreeSource implements TreeSource {

        @Override
        public List<?> children(Object parent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isLeaf(Object node) {
            return true;
        }
    }

    static interface TreePathEditor {
        void startEditingAtPath(TreePath path);
        
        boolean canEdit(TreePath path);
    }
    
    
    
    @Deprecated
    static class LazyChildLoadingTreeNode implements TreeNode {
        private final Object nodeObject;
        
        private final TreeSource source;
        
        private final TreeNode parent;
        
        private List<TreeNode> childeNodeList = null;
        
        public LazyChildLoadingTreeNode(Object nodeObject, TreeSource source, TreeNode parent) {
            this.nodeObject = nodeObject;
            this.source = source;
            this.parent = parent;
        }

        private List<?> childElements() {
            return source.children(nodeObject);
        }

        protected TreeNode createChildTreeNode(Object nodeObject) {
            TreeNode node = new LazyChildLoadingTreeNode(nodeObject, source, this);
            return node;
        }

        List<TreeNode> childeNodeList() {
            if (childeNodeList == null) {
                List<?> children = childElements();
                List<TreeNode> list = new ArrayList<TreeNode>(children.size());
                
                for (Object child: children) {
                    TreeNode node = createChildTreeNode(child);
                    list.add(node);
                }
                
                childeNodeList = Collections.unmodifiableList(list);
            }
            return childeNodeList;
        }
        
        @Override
        public TreeNode getChildAt(int childIndex) {
            return childeNodeList().get(childIndex);
        }

        @Override
        public int getChildCount() {
            return childeNodeList().size();
        }

        @Override
        public TreeNode getParent() {
            return parent;
        }

        @Override
        public int getIndex(TreeNode node) {
            return childeNodeList().indexOf(node);
        }

        @Override
        public boolean getAllowsChildren() {
            return !isLeaf();
        }

        @Override
        public boolean isLeaf() {
            return source.isLeaf(nodeObject);
        }

        @Override
        public Enumeration<TreeNode> children() {
            return Collections.enumeration(childeNodeList());
        }
        
        public Object getNodeObject() {
            return nodeObject;
        }
    }
    
    static class SelectableSelectionModel extends DefaultTreeSelectionModel {
        
        public boolean canSelect(TreePath path) {
            return true;
        }
        
        @Override
        public void setSelectionPath(TreePath path) {
            if (canSelect(path))
                super.setSelectionPath(path);
        }
        
        @Override
        public void setSelectionPaths(TreePath[] paths) {
            super.setSelectionPaths(filterSelectable(paths));
        }

        @Override
        public void addSelectionPath(TreePath path) {
            if (canSelect(path))
                super.addSelectionPath(path);
        }
        
        @Override
        public void addSelectionPaths(TreePath[] paths) {
            super.addSelectionPaths(filterSelectable(paths));
        }

        TreePath[] filterSelectable(TreePath[] pPaths) {
            if (pPaths == null)
                return null;
            
            List<TreePath> pathList = new ArrayList<TreePath>(pPaths.length);
            for (TreePath path: pPaths) {
                if (canSelect(path))
                    pathList.add(path);
            }
            return pathList.toArray(new TreePath[0]);
        }
    }
}