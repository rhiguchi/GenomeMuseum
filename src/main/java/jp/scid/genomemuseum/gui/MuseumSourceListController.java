package jp.scid.genomemuseum.gui;

import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jp.scid.genomemuseum.model.CollectionBox;
import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBoxService;
import jp.scid.genomemuseum.model.MuseumSourceModel;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionBoxNode;
import jp.scid.gui.control.ActionManager;

public class MuseumSourceListController extends AbstractButton {
    final DefaultTreeModel treeModel;
    
    final MuseumSourceModel sourceModel;
    
    final DefaultTreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
    
    protected BioFileLoader bioFileLoader = null;
    
    // Actions
    private final ActionManager actionManager;
    
    protected final Action addFreeBoxAction;
    
    protected final Action addGroupBoxAction;
    
    public MuseumSourceListController() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root", true);
        treeModel = new DefaultTreeModel(root, true);
        sourceModel = new MuseumSourceModel(treeModel);
        
        actionManager = new ActionManager(this);
        
        addFreeBoxAction = actionManager.getAction("addFreeBox");
        addGroupBoxAction = actionManager.getAction("addGroupBox");
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
    
    public BioFileLoader getBioFileLoader() {
        return bioFileLoader;
    }
    
    public void setBioFileLoader(BioFileLoader bioFileLoader) {
        this.bioFileLoader = bioFileLoader;
    }
    
    public void addExhibit(URL source) {
        try {
            getBioFileLoader().executeWithSourceUrl(source);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // CollectionBoxSource
    public CollectionBoxService getCollectionBoxSource() {
        return sourceModel.getCollectionBoxTreeModel();
    }

    public void setCollectionBoxSource(CollectionBoxService newSource) {
        sourceModel.setCollectionBoxTreeModel(newSource);
    }
    
    // Action methods
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
        
        if (selection != null && selection.getLastPathComponent() instanceof CollectionBox) {
            CollectionBoxNode boxNode = (CollectionBoxNode) selection.getLastPathComponent();
            sourceModel.removeCollectionBox(boxNode);
        }
    }
    
    public void moveBox(CollectionBoxNode node, CollectionBoxNode newParent) {
        sourceModel.moveCollectionBox(node, newParent);
    }
    
    public void moveBox(CollectionBoxNode newParent) {
        TreePath selection = getSelectionPath();
        CollectionBoxNode node = (CollectionBoxNode) selection.getLastPathComponent();
        moveBox(node, newParent);
    }
    
    protected void insertCollectionBox(BoxType boxType) {
        TreePath selection = getSelectionPath();
        
        final CollectionBoxNode parent;
        
        if (selection != null && selection.getLastPathComponent() instanceof CollectionBoxNode) {
            // TODO
            parent = (CollectionBoxNode) selection.getLastPathComponent();
        }
        else {
            parent = null;
        }
        
        sourceModel.addCollectionBox(boxType, parent);
    }
    
    public void insertNodeAt(TreePath path, MutableTreeNode newChild) {
        Object selectedNode = path.getLastPathComponent();
        final MutableTreeNode parent;
        final int index;
        
        if (!treeModel.isLeaf(selectedNode) || path.getPathCount() == 1) {
            parent = (MutableTreeNode) selectedNode;
            index = treeModel.getChildCount(selectedNode);
        }
        else {
            parent = (MutableTreeNode) path.getPathComponent(path.getPathCount() - 2);
            index = treeModel.getIndexOfChild(parent, selectedNode);
        }
        
        treeModel.insertNodeInto(newChild, parent, index);
    }
    
    public void bindTree(JTree tree) {
        tree.setModel(sourceModel);
        tree.setRootVisible(false);
        tree.setSelectionModel(selectionModel);
    }
    
    public void bindAddFreeBox(AbstractButton button) {
        button.setAction(addFreeBoxAction);
    }
}