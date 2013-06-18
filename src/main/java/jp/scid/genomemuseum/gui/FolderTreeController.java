package jp.scid.genomemuseum.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jp.scid.bio.store.folder.CollectionType;
import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.folder.FoldersContainer;
import jp.scid.genomemuseum.model.MuseumTreeSource;
import jp.scid.genomemuseum.model.NodeListTreeModel;
import jp.scid.gui.control.ActionManager;
import jp.scid.gui.control.BooleanModelBindings;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeController implements TreeSelectionListener {
    private final static Logger logger = LoggerFactory.getLogger(FolderTreeController.class);
    
    private final NodeListTreeModel treeModel;
    private final SourceSelectionModel selectionModel;
    private final FolderTreeTransferHandler transferHandler;
    
    private final Action basicFolderAddAction;
    private final Action groupFolderAddAction;
    private final Action filterFolderAddAction;
    private final Action folderRemoveAction;
    
    private ValueModel<Object> selectedNodeObject;
    
    private MuseumTreeSource treeSource;
    
    public FolderTreeController() {
        treeModel = new NodeListTreeModel();
        selectionModel = new SourceSelectionModel();
        transferHandler = new FolderTreeTransferHandler(this);
        
        selectedNodeObject = ValueModels.newTreeSelectedNodeObject(selectionModel);
        
        ActionManager actionManager = new ActionManager(this);
        basicFolderAddAction = actionManager.getAction("addBasicFolder");
        groupFolderAddAction = actionManager.getAction("addGroupFolder");
        filterFolderAddAction = actionManager.getAction("addFilterFolder");
        folderRemoveAction = actionManager.getAction("remove");
        
        ValueModel<Boolean> hasSelection =
                ValueModels.newInstanceCheckModel(selectedNodeObject, Folder.class);
        new BooleanModelBindings(hasSelection).bindToActionEnabled(folderRemoveAction);
    }
    
    public void setModel(MuseumTreeSource treeSource) {
        treeModel.setTreeSource(treeSource);
        this.treeSource = treeSource;
        
        selectAnyNode();
    }
    
    public TreeModel getTreeModel() {
        return treeModel;
    }

    public TreePath getSelectionPath() {
        return selectionModel.getSelectionPath();
    }

    public ValueModel<Object> selectedNodeObject() {
        return selectedNodeObject;
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
        selectionModel.setSelectionPath(path);
        // TODO Auto-generated method stub
        
    }

    TreePath addFolder(CollectionType type) {
        Object selectedNodeObject = getSelectedNodeObject();
        
        FoldersContainer parent;
        if (selectedNodeObject instanceof FoldersContainer) {
            parent = (FoldersContainer) selectedNodeObject;
        }
        else if (selectedNodeObject instanceof Folder) {
            parent = ((Folder) selectedNodeObject).getParent();
        }
        else {
            parent = treeSource.getUserCollectionsRoot();
        }
        
        Folder child = parent.createContentFolder(type);
        int[] indexPath = treeSource.getIndexPath(child);
        TreePath path = treeModel.getPathOfIndex(indexPath);
        return path;
    }

    public Object getSelectedNodeObject() {
        return selectedNodeObject.getValue();
    }
    
    public void remove() {
        logger.debug("FolderTreeController#remove");
        Folder folder = (Folder) getSelectedNodeObject();
        if (folder != null) {
            folder.deleteFromParent();
        }
    }
    
    // Selection
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        if (e.getNewLeadSelectionPath() == null) {
            selectAnyNode();
        }
    }
    
    private void selectAnyNode() {
        if (treeModel.getRoot() == null) {
            return;
        }
        
        TreePath path = treeModel.getPathOfIndex(new int[]{0, 0});
        selectionModel.setSelectionPath(path);
    }

    // Bindings
    public void bindTree(JTree tree) {
        tree.setRootVisible(false);
        tree.setModel(treeModel);
        tree.setSelectionModel(selectionModel);
        tree.addTreeSelectionListener(this);

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

    static class SourceSelectionModel extends DefaultTreeSelectionModel {
        public boolean canSelect(TreePath path) {
            return path.getPathCount() > 2;
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
