package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import jp.scid.genomemuseum.gui.transfer.DefaultTransferExhibitCollectionNode;
import jp.scid.genomemuseum.gui.transfer.TransferExhibitCollectionNode;
import jp.scid.genomemuseum.gui.transfer.TransferMuseumExhibit;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionNode;
import jp.scid.genomemuseum.model.MuseumSourceModel.ExhibitCollectionNode;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderContainer;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderTreeNode;
import jp.scid.genomemuseum.model.MuseumTreeSource.SequenceImportable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeTransferHandler extends TransferHandler {
    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(FolderTreeTransferHandler.class);
    
    final FolderTreeController controller;

    public FolderTreeTransferHandler(FolderTreeController controller) {
        this.controller = controller;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        boolean canImport;
        int action = COPY;
        
        if (support.isDataFlavorSupported(TransferExhibitCollectionNode.Flavor.getInstance())) {
            ExhibitCollectionNode sourceNode =
                    TransferExhibitCollectionNode.Flavor.getTransferExhibitCollectionNode(support);
            Object targetObject = getTargetNodeObject(support);
            
            if (targetObject instanceof FolderContainer) {
                canImport = false; // TODO ((FolderContainer) targetObject).canMove(sourceNode);
                if (canImport) {
                    action = MOVE;
                }
            }
            else {
                canImport = false;
            }
        }
        else if (support.isDataFlavorSupported(TransferMuseumExhibit.Flavor.getInstance())) {
            MutableTreeNode targetNode = getTargetNode(support);
            
            canImport = targetNode instanceof SequenceImportable && targetNode instanceof FolderTreeNode;
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            MutableTreeNode targetNode = getTargetNode(support);
            
            canImport = targetNode instanceof SequenceImportable;
        }
        else {
            canImport = false;
        }
        
        if (support.isDrop()) {
            support.setDropAction(action);
            support.setShowDropLocation(canImport);
        }
        
        return canImport;
    }

    @Override
    public boolean importData(TransferSupport support) {
        final boolean result;
        
        if (support.isDataFlavorSupported(TransferExhibitCollectionNode.Flavor.getInstance())) {
            ExhibitCollectionNode sourceNode =
                    TransferExhibitCollectionNode.Flavor.getTransferExhibitCollectionNode(support);
            MutableTreeNode targetNode = getTargetNode(support);
            
//            controller.moveNode(sourceNode, (CollectionNode) targetNode);
            
            result = true;
        }
        else if (support.isDataFlavorSupported(TransferMuseumExhibit.Flavor.getInstance())) {
            List<MuseumExhibit> data = TransferMuseumExhibit.Flavor.getTransferMuseumExhibit(support);
            MutableTreeNode targetNode = getTargetNode(support);
            
//            controller.importExhibit((CollectionNode) targetNode, data);
            result = true;
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            List<File> fileList;
            MutableTreeNode targetNode;
            try {
                fileList = ListTransferHandler.getTransferFile(support);
                targetNode = getTargetNode(support);
            }
            catch (IOException e) {
                return false;
            }
            
            result = false; //controller.importFile((ExhibitCollectionNode) targetNode, fileList);
        }
        else {
            result = false;
        }
        
        return result;
    }
    
    private Object getTargetNodeObject(TransferSupport support) {
        MutableTreeNode node = getTargetNode(support);
        if (node instanceof DefaultMutableTreeNode) {
            return ((DefaultMutableTreeNode) node).getUserObject();
        }
        return null;
    }
    private MutableTreeNode getTargetNode(TransferSupport support) {
        TreePath targetPath;
        
        if (support.getComponent() instanceof JTree) {
            if (support.isDrop()) {
                targetPath = ((JTree.DropLocation) support.getDropLocation()).getPath();
            }
            else {
                targetPath = ((JTree) support.getComponent()).getSelectionPath();
            }
        }
        else {
            targetPath = null;
        }
        
        return targetPath == null ? null : (MutableTreeNode) targetPath.getLastPathComponent();
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        if (c instanceof JTree && ((JTree) c).getModel() == controller.getTreeModel()) {
            return COPY_OR_MOVE;
        }
        
        return super.getSourceActions(c);
    }
    
    @Override
    protected Transferable createTransferable(JComponent c) {
        final Transferable data;
        
        if (c instanceof JTree && ((JTree) c).getModel() == controller.getTreeModel()) {
            TreePath path = controller.getSelectionPath();
            
            if (path != null && path.getLastPathComponent() instanceof ExhibitCollectionNode) {
                ExhibitCollectionNode node = ((ExhibitCollectionNode) path.getLastPathComponent());
                data = new DefaultTransferExhibitCollectionNode(node);
            }
            else {
                data = null;
            }
        }
        else {
            data = null;
        }
        
        return data;
    }
}
