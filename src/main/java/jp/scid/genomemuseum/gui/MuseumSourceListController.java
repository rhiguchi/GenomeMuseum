package jp.scid.genomemuseum.gui;

import static java.lang.String.*;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
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

import jp.scid.genomemuseum.model.CollectionBox;
import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBoxService;
import jp.scid.genomemuseum.model.EntityService;
import jp.scid.genomemuseum.model.ExhibitListModel;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumSourceModel;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionNode;
import jp.scid.genomemuseum.model.MuseumSourceModel.ExhibitCollectionNode;
import jp.scid.genomemuseum.model.MuseumSourceModel.GroupCollectionNode;
import jp.scid.gui.control.ActionManager;

import org.jdesktop.application.AbstractBean;
import org.jooq.UpdatableRecord;
import org.jooq.UpdatableTable;
import org.jooq.impl.Factory;

public class MuseumSourceListController extends TreeController {
    public static final String PROPETY_SELECTED_ROOM = "selectedRoom";
    
    final MuseumSourceModel sourceModel;
    
    // Actions
    private final ActionManager actionManager;
    
    protected final Action addFreeBoxAction;
    protected final Action addGroupBoxAction;
    protected final Action addSmartBoxAction;
    protected final Action removeBoxAction;
    
    final BindingSupport bindings = new BindingSupport(this);
    
    // Controller
    private final MuseumSourceTransferHandler transferHandler;
    
    private final TreeSelectionListener roomSelectionHandler = new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath path = e.getPath();
            if (path != null) {
                MutableTreeNode node = (MutableTreeNode) path.getLastPathComponent();
                
                if (node instanceof CollectionNode) {
                    // TODO
//                    ExhibitListModel model = ((CollectionNode) node).getCollectionBox(); 
//                    setExhibitListSource(model);
                }
                else if (node instanceof DefaultMutableTreeNode &&
                        ((DefaultMutableTreeNode) node).getUserObject() instanceof ExhibitListModel) {
                    ExhibitListModel model = (ExhibitListModel) ((DefaultMutableTreeNode) node).getUserObject();
//                    setExhibitListSource(model);
                }
            }
        }
    };
    
    public MuseumSourceListController() {
        sourceModel = new MuseumSourceModel((DefaultTreeModel) getTreeModel());
        
        actionManager = new ActionManager(this);
        
        addFreeBoxAction = actionManager.getAction("addFreeBox");
        addGroupBoxAction = actionManager.getAction("addGroupBox");
        addSmartBoxAction = actionManager.getAction("addSmartBox");
        removeBoxAction = actionManager.getAction("removeSelectedBox");
        
        transferHandler = new MuseumSourceTransferHandler(this);
    }
    
    @Override
    public boolean canSelect(TreePath path) {
        return path.getPathCount() > 2;
    }

    // local lib source
    public void setLocalLibrarySource(Object librarySource) {
        sourceModel.setLocalLibrarySource(librarySource);
    }
    
    // CollectionBoxSource
    public EntityService<CollectionBox> getCollectionBoxSource() {
        return sourceModel.getCollectionBoxTreeModel();
    }

    public void setCollectionBoxSource(CollectionBoxService newSource) {
        sourceModel.setCollectionBoxTreeModel(newSource);
    }
    
    // Action methods
    protected void insertCollectionBox(BoxType boxType) {
        TreePath selection = getSelectionPath();
        
        final GroupCollectionNode parent;
        
        if (selection != null && selection.getLastPathComponent() instanceof CollectionNode) {
            CollectionNode selectedNode = (CollectionNode) selection.getLastPathComponent();
            
            if (selectedNode instanceof GroupCollectionNode) {
                parent = (GroupCollectionNode) selectedNode;
            }
            else {
                parent = (GroupCollectionNode) selectedNode.getParent();
            }
        }
        else {
            parent = sourceModel.getCollectionBoxTreeRootNode();
        }
        
        sourceModel.addCollectionBox(boxType, parent);
    }
    
    public void addFreeBox() {
        insertCollectionBox(BoxType.FREE);
    }
    
    public void addGroupBox() {
        insertCollectionBox(BoxType.GROUP);
    }
    
    public void addSmartBox() {
        insertCollectionBox(BoxType.SMART);
    }
    
    public void removeSelectedBox() {
        TreePath selection = getSelectionPath();
        
        if (selection != null && selection.getLastPathComponent() instanceof CollectionNode) {
            sourceModel.removeCollectionBox((CollectionNode) selection.getLastPathComponent());
        }
    }
    
    public boolean canMoveNode(ExhibitCollectionNode node, CollectionNode target) {
        boolean result;
        
        // node moving
        if (target instanceof GroupCollectionNode) {
            if (node instanceof CollectionNode) {
                result = ((GroupCollectionNode) target).canMove((CollectionNode) node);
            }
            else {
                result = false;
            }
        }
        // contents moving
        else {
            return canImportExhibit(target);
        }
        
        return result;
    }
    
    public void moveNode(ExhibitCollectionNode node, CollectionNode target) {
        // node moving
        if (target instanceof GroupCollectionNode) {
            if (node instanceof CollectionNode) {
                sourceModel.moveCollectionBox((CollectionNode) node, (GroupCollectionNode) target);
            }
            else {
                throw new IllegalArgumentException(format("node %s cannot move into %s", node, target));
            }
        }
        // contents moving
        else {
            // TODO
        }
    }
    
    public void moveBox(CollectionNode targetNode) {
        TreePath selection = getSelectionPath();
        CollectionNode node = (CollectionNode) selection.getLastPathComponent();
        moveNode(node, targetNode);
    }
    
    public boolean canImportExhibit(CollectionNode node) {
        return node.isContentsEditable();
    }
    
    public void importExhibit(CollectionNode node, List<MuseumExhibit> data) {
        // TODO
    }
    
    public boolean canImportFile(ExhibitCollectionNode node) {
        // TODO
        return true;
    }
    
    public boolean importFile(ExhibitCollectionNode node, List<File> data) {
        // TODO
        return false;
    }
    
    protected void reloadChildren(GroupCollectionNode boxNode) {
        sourceModel.reloadCollectionBoxNode(boxNode);
    }
    
    // bindings
    @Override
    public void bindTree(JTree tree) {
        super.bindTree(tree);

        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON);
//        reloadHandler.installTo(tree);
    }
    
    public void bindAddFreeBox(AbstractButton button) {
        button.setAction(addFreeBoxAction);
    }
    
    public void bindAddGroupBox(AbstractButton button) {
        button.setAction(addGroupBoxAction);
    }
    
    public void bindAddSmartBox(AbstractButton button) {
        button.setAction(addSmartBoxAction);
    }
    
    public void bindRemoveBox(AbstractButton button) {
        button.setAction(removeBoxAction);
    }
}

abstract class JooqRecordTreeController<E extends UpdatableRecord<E>> extends TreeController<E> {

    // persistence
    protected Factory factory = null;
    
    protected final UpdatableTable<E> table;
    
    public JooqRecordTreeController(UpdatableTable<E> table) {
        this.table = table;
    }
    
    @Override
    protected E createElement() {
        if (factory == null) {
            throw new IllegalStateException("need factory");
        }
        
        E record = factory.newRecord(table);
        return record;
    }
}

abstract class TreeController<E> extends AbstractBean implements TreeWillExpandListener, TreeSelectionListener {
    private final int expandPathCount = 2;
    
    // sub-controller
    private TransferHandler transferHandler = null;
    
    final TreeSelectionModel selectionModel;
    
    final DefaultTreeModel treeModel;
    
    // Model
    TreeSource treeSource;
    
    List<E> allElements = Collections.emptyList();
    
    // property
    boolean emptySelectionAllowed = false;
    
    public TreeController() {
        treeSource = createTreeSource();
        treeModel = createTreeModel();
        
        selectionModel = createSelectionModel();
        
        selectionModel.addTreeSelectionListener(this);
    }

    protected DefaultTreeModel createTreeModel() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        return new DefaultTreeModel(rootNode, true);
    }
    
    protected TreeSource createTreeSource() {
        return new DefaultTreeSource();
    }
    
    public void setTreeSource(TreeSource treeSource) {
        this.treeSource = treeSource;
        
        reload();
    }
    
    public void reload() {
        LazyChildLoadingTreeNode node = new LazyChildLoadingTreeNode(null, treeSource, null);
        defaultTreeModel().setRoot(node);
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

    List<E> getChildren(E parent) {
        
        return allElements;
    }
    
    // nodes
//    protected boolean getAllowesChildren(E element) {
//        return true;
//    }
//    
//    protected MutableTreeNode createTreeNode(Object nodeObject, boolean allowsChildren) {
//        return new DefaultMutableTreeNode(nodeObject, allowsChildren);
//    }
//    
//    protected MutableTreeNode createRootNode() {
//        return createTreeNode(this, true);
//    }
//    
    
    
    // contents
    public void setContent(List<E> newElements) {
    }
    
    
    
    private E elementForNode(TreeNode node) {
        return (E) ((DefaultMutableTreeNode) node).getUserObject();
    }
    
//    protected void reload(E element, MutableTreeNode node) {
//        List<E> children = getChildren(element);
//        
//        while (node.getChildCount() > 0) {
//            node.remove(node.getChildCount() - 1);
//        }
//        
//        for (E child: children) {
//            boolean allowsChildren = getAllowesChildren(child);
//            MutableTreeNode childNode = createTreeNode(child, allowsChildren);
//            // TODO
//            
//            node.insert(childNode, node.getChildCount());
//        }
//        
//        if (node.getParent() == null) {
//            defaultTreeModel().setRoot(node);
//        }
//        else {
//            defaultTreeModel().reload(node);
//        }
//    }
//    
//    protected void reload() {
//        MutableTreeNode root = createRootNode();
//        reload(null, root);
//    }
//    
    public void add() {
        E e = createElement();
        add(e);
    }
    
    public void add(E element) {
        TreePath path = getSelectionPath();
        if (path == null) {
            path = new TreePath(treeModel.getRoot());
        }
        
        MutableTreeNode parent = (MutableTreeNode) path.getLastPathComponent();
        int index = parent.getChildCount();
        
        insertNodeInto(element, path, index);
    }
    
    public void insertNodeInto(E element, TreePath path, int index) {
//        boolean allowsChildren = getAllowesChildren(element);
//        MutableTreeNode newChild = createTreeNode(element, allowsChildren);
//        MutableTreeNode parent = (MutableTreeNode) path.getLastPathComponent();
//        
//        defaultTreeModel().insertNodeInto(newChild, parent, index);
    }
    
    // selection
    public TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public TreePath getSelectionPath() {
        return selectionModel.getSelectionPath();
    }
    
    public void setSelectionPath(TreePath path) {
        if (canSelect(path)) {
            selectionModel.setSelectionPath(path);
        }
    }
    
    public boolean canSelect(TreePath path) {
        return true;
    }
    
    private DefaultTreeModel defaultTreeModel() {
        return (DefaultTreeModel) treeModel;
    }
    
    protected TreeModel getTreeModel() {
        return treeModel;
    }
    
    public void selectFirstRow() {
        // TODO
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        TreeSelectionModel model = (TreeSelectionModel) e.getSource();
        
        if (model.isSelectionEmpty() && !emptySelectionAllowed) {
            selectFirstRow();
        }
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

    @SuppressWarnings("unchecked")
    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        TreePath path = event.getPath();

        if (path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
//            E element = (E) node.getUserObject();
//            reload(element, node);
        }
    }

    private void updateExpansion(JTree tree) {
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
    
    public static interface ListHolder {
        Iterable<?> fetch();
    }
    
    public static class DefaultListHolder implements ListHolder {
        List<?> elementList;

        public DefaultListHolder(List<?> elementList) {
            super();
            this.elementList = elementList;
        }
        
        @Override
        public Iterable<?> fetch() {
            return elementList;
        }
    }
    
    class SelectableSelectionModel extends DefaultTreeSelectionModel {
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