package jp.scid.genomemuseum.gui;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jp.scid.bio.store.folder.CollectionType;
import jp.scid.genomemuseum.model.MuseumTreeSource;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderContainer;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderTreeNode;
import jp.scid.genomemuseum.model.NodeListTreeModel;
import jp.scid.gui.control.ActionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeController {
    private final static Logger logger = LoggerFactory.getLogger(FolderTreeController.class);
    
    private final NodeListTreeModel treeModel;
    private final TreeSelectionModel selectionModel;
    private final FolderTreeTransferHandler transferHandler;
    
    private final Action basicFolderAddAction;
    private final Action groupFolderAddAction;
    private final Action filterFolderAddAction;
    private final Action folderRemoveAction;
    
    private MuseumTreeSource treeSource;
    
    public FolderTreeController() {
        treeModel = new NodeListTreeModel();
        selectionModel = new TreeController.SelectableSelectionModel();
        transferHandler = new FolderTreeTransferHandler(this);
        
        ActionManager actionManager = new ActionManager(this);
        basicFolderAddAction = actionManager.getAction("addBasicFolder");
        groupFolderAddAction = actionManager.getAction("addGroupFolder");
        filterFolderAddAction = actionManager.getAction("addFilterFolder");
        folderRemoveAction = actionManager.getAction("remove");
    }
    
    public void setModel(MuseumTreeSource treeSource) {
        treeModel.setTreeSource(treeSource);
        this.treeSource = treeSource;
    }
    
    public TreeModel getTreeModel() {
        return treeModel;
    }

    public TreePath getSelectionPath() {
        return selectionModel.getSelectionPath();
    }
    
    // Transfer
    
    // Actions
    public boolean canAdd() {
        return treeSource != null;
    }
    
    public void addBasicFolder() {
        logger.debug("FolderTreeController#addBasicFolder");
        TreePath path = addFolder(CollectionType.BASIC);
        startEditingAtPath(path);
    }
    
    public void addGroupFolder() {
        logger.debug("FolderTreeController#addGroupFolder");
        TreePath path = addFolder(CollectionType.NODE);
        startEditingAtPath(path);
    }
    
    public void addFilterFolder() {
        logger.debug("FolderTreeController#addFilterFolder");
        TreePath path = addFolder(CollectionType.FILTER);
        startEditingAtPath(path);
    }

    private void startEditingAtPath(TreePath path) {
        // TODO Auto-generated method stub
        
    }

    TreePath addFolder(CollectionType type) {
        Object selectedNodeObject = getSelectedNodeObject();
        
        FolderContainer parent;
        if (selectedNodeObject instanceof FolderContainer) {
            parent = (FolderContainer) selectedNodeObject;
        }
        else if (selectedNodeObject instanceof FolderTreeNode) {
            parent = ((FolderTreeNode) selectedNodeObject).getParentContainer();
        }
        else {
            parent = treeSource.getUserCollectionRoot();
        }
        
        FolderTreeNode newFolder = parent.addChild(type);
        int[] indexPath = treeSource.getIndexPath(newFolder);
        TreePath path = treeModel.getPathOfIndex(indexPath);
        
        return path;
    }

    private Object getSelectedNodeObject() {
        TreePath selectionPath = getSelectionPath();
        Object selectedNodeObject;
        if (selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            selectedNodeObject = ((DefaultMutableTreeNode) selectionPath.getLastPathComponent()).getUserObject();
        }
        else {
            selectedNodeObject = null;
        }
        return selectedNodeObject;
    }

    
    public boolean canRemove() {
        return getSelectedNodeObject() instanceof FolderTreeNode;
    }
    
    public void remove() {
        logger.debug("FolderTreeController#remove");
        Object selectedNodeObject = getSelectedNodeObject();
        ((FolderTreeNode) selectedNodeObject).remove();
    }
    
    // Bindings
    public void bindTree(JTree tree) {
        tree.setRootVisible(false);
        tree.setModel(treeModel);
        tree.setSelectionModel(selectionModel);

//        updateExpansion(tree);
//        tree.addTreeWillExpandListener(this);
        
        tree.setTransferHandler(transferHandler);
    }
    
    public void bindBasicFolderAddButton(AbstractButton button) {
        button.setAction(basicFolderAddAction);
    }
    
    public void bindGroupFolderAddButton(AbstractButton button) {
        button.setAction(groupFolderAddAction);
    }
    
    public void bindFilterFolderAddButton(AbstractButton button) {
        button.setAction(filterFolderAddAction);
    }
    
    public void bindFolderRemoveButton(AbstractButton button) {
        button.setAction(folderRemoveAction);
    }
}
