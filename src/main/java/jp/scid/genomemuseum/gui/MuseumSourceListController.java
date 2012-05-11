package jp.scid.genomemuseum.gui;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jp.scid.genomemuseum.gui.transfer.MuseumSourceTransferHandler;
import jp.scid.genomemuseum.model.CollectionBox;
import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBoxService;
import jp.scid.genomemuseum.model.EntityService;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumSourceModel;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionBoxNode;
import jp.scid.genomemuseum.model.MuseumSourceModel.ExhibitCollectionNode;
import jp.scid.gui.control.ActionManager;

import org.jdesktop.application.AbstractBean;

public class MuseumSourceListController extends AbstractBean implements TreeSelectionListener {
    final DefaultTreeModel treeModel;
    
    final MuseumSourceModel sourceModel;
    
    final TreeSelectionModel selectionModel;
    
    protected ExhibitDataLoader bioFileLoader = null;
    
    // Actions
    private final ActionManager actionManager;
    
    protected final Action addFreeBoxAction;
    protected final Action addGroupBoxAction;
    protected final Action addSmartBoxAction;
    protected final Action removeBoxAction;
    
    final BindingSupport bindings = new BindingSupport(this);
    
    // Controller
    private final MuseumSourceTransferHandler transferHandler;
    
    private final NodeReloadHandler reloadHandler = new NodeReloadHandler();
    
    public MuseumSourceListController() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root", true);
        treeModel = new DefaultTreeModel(root, true);
        sourceModel = new MuseumSourceModel(treeModel);
        
        selectionModel = createTreeSelectionModel();
        selectionModel.addTreeSelectionListener(this);
        
        actionManager = new ActionManager(this);
        
        addFreeBoxAction = actionManager.getAction("addFreeBox");
        addGroupBoxAction = actionManager.getAction("addGroupBox");
        addSmartBoxAction = actionManager.getAction("addSmartBox");
        removeBoxAction = actionManager.getAction("removeSelectedBox");
        
        transferHandler = new MuseumSourceTransferHandler(this);
    }

    public TreeModel getTreeModel() {
        return treeModel;
    }
    
    public TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }
    
    public TreePath getSelectionPath() {
        return selectionModel.getSelectionPath();
    }
    
    public void setSelectionPath(TreePath path) {
        selectionModel.setSelectionPath(path);
    }
    
    public ExhibitDataLoader getBioFileLoader() {
        return bioFileLoader;
    }
    
    public void setBioFileLoader(ExhibitDataLoader bioFileLoader) {
        this.bioFileLoader = bioFileLoader;
    }
    
    public void addExhibit(URL source) {
        URI uri;
        try {
            uri = source.toURI();
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        
        MuseumExhibit museumExhibit = getBioFileLoader().newMuseumExhibit(uri);
        getBioFileLoader().executeReload(museumExhibit);
    }
    
    public boolean isSelectable(TreePath path) {
        return path.getPathCount() > 2;
    }

    // local lib source
    public void setLocalLibrarySource(Object librarySource) {
        sourceModel.setLocalLibrarySource(librarySource);
        
        selectLocalLibrarySource();
    }
    
    // CollectionBoxSource
    public EntityService<CollectionBox> getCollectionBoxSource() {
        return sourceModel.getCollectionBoxTreeModel();
    }

    public void setCollectionBoxSource(CollectionBoxService newSource) {
        sourceModel.setCollectionBoxTreeModel(newSource);
    }
    
    // Action methods
    public void selectLocalLibrarySource() {
        DefaultMutableTreeNode node = sourceModel.getLocalLibraryNode();
        TreeNode[] pathToRoot = treeModel.getPathToRoot(node);
        TreePath treePath = new TreePath(pathToRoot);
        selectionModel.setSelectionPath(treePath);
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
        
        if (selection != null && selection.getLastPathComponent() instanceof CollectionBoxNode) {
            sourceModel.removeCollectionBox((CollectionBoxNode) selection.getLastPathComponent());
        }
    }
    
    public boolean canMoveNode(ExhibitCollectionNode source, CollectionBoxNode target) {
        // TODO
        return true;
    }
    
    
    public void moveNode(ExhibitCollectionNode source, CollectionBoxNode target) {
        // TODO
    }
    
    public void moveBox(CollectionBoxNode node, CollectionBoxNode targetNode) {
        sourceModel.moveCollectionBox(node, targetNode);
    }
    
    public void moveBox(CollectionBoxNode targetNode) {
        TreePath selection = getSelectionPath();
        CollectionBoxNode node = (CollectionBoxNode) selection.getLastPathComponent();
        moveBox(node, targetNode);
    }
    
    public boolean canImportExhibit(CollectionBoxNode node) {
        // TODO
        return true;
    }
    
    public void importExhibit(CollectionBoxNode node, List<MuseumExhibit> data) {
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
    
    protected void insertCollectionBox(BoxType boxType) {
        TreePath selection = getSelectionPath();
        
        final CollectionBoxNode parent;
        
        if (selection != null && selection.getLastPathComponent() instanceof CollectionBoxNode) {
            parent = (CollectionBoxNode) selection.getLastPathComponent();
        }
        else {
            parent = sourceModel.getCollectionBoxTreeRootNode();
        }
        
        sourceModel.addCollectionBox(boxType, parent);
    }
    
    protected void reloadChildren(CollectionBoxNode boxNode) {
        sourceModel.reloadCollectionBoxNode(boxNode);
    }
    
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        if (getSelectionPath() == null) {
            selectLocalLibrarySource();
        }
    }
    
    // bindings
    public void bindTree(JTree tree) {
        tree.setModel(getTreeModel());
        tree.setRootVisible(false);
        tree.setSelectionModel(getSelectionModel());
        reloadHandler.installTo(tree);
        
        tree.setTransferHandler(transferHandler);
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON);
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
    
    // model factories
    protected TreeSelectionModel createTreeSelectionModel() {
        SelectableSelectionModel selectionModel = new SelectableSelectionModel();
        return selectionModel;
    }
    
    class NodeReloadHandler implements TreeWillExpandListener {
        private final int expandPathCount = 2;
        
        public void installTo(JTree tree) {
            tree.addTreeWillExpandListener(this);
            
            for (int row = 0; row < tree.getRowCount(); row++) {
                if (tree.getPathForRow(row).getPathCount() <= expandPathCount
                        && tree.isCollapsed(row)) {
                    tree.expandRow(row);
                }
            }
        }
        
        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            TreePath path = event.getPath();
            
            if (path.getPathCount() <= expandPathCount)
                throw new ExpandVetoException(event);
        }

        @Override
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            TreePath path = event.getPath();
            
            if (path.getLastPathComponent() instanceof CollectionBoxNode) {
                reloadChildren((CollectionBoxNode) path.getLastPathComponent());
            }
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

        boolean canSelect(TreePath path) {
            return isSelectable(path);
        }
    }
}