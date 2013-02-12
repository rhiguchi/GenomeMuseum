package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import jp.scid.genomemuseum.gui.transfer.DefaultTransferExhibitCollectionNode;
import jp.scid.genomemuseum.gui.transfer.TransferExhibitCollectionNode;
import jp.scid.genomemuseum.gui.transfer.TransferMuseumExhibit;
import jp.scid.genomemuseum.gui.transfer.TransferExhibitCollectionNode.Flavor;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionNode;
import jp.scid.genomemuseum.model.MuseumSourceModel.ExhibitCollectionNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuseumSourceTransferHandler extends TransferHandler {
    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(MuseumSourceTransferHandler.class);
    
    final MuseumSourceListController controller;

    public MuseumSourceTransferHandler(MuseumSourceListController controller) {
        this.controller = controller;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        boolean canImport;
        
        if (support.isDataFlavorSupported(TransferExhibitCollectionNode.Flavor.getInstance())) {
            ExhibitCollectionNode sourceNode =
                    TransferExhibitCollectionNode.Flavor.getTransferExhibitCollectionNode(support);
            MutableTreeNode targetNode = getTargetNode(support);
            
            if (targetNode instanceof CollectionNode) {
                canImport = controller.canMoveNode(sourceNode, (CollectionNode) targetNode);
            }
            else {
                canImport = false;
            }
            
            if (support.isDrop()) {
                support.setDropAction(targetNode.getAllowsChildren() ? MOVE : COPY);
                support.setShowDropLocation(canImport);
            }
        }
        else if (support.isDataFlavorSupported(TransferMuseumExhibit.Flavor.getInstance())) {
            MutableTreeNode targetNode = getTargetNode(support);
            
            // TODO compare box id
            
            if (targetNode instanceof CollectionNode) {
                canImport = controller.canImportExhibit((CollectionNode) targetNode);
            }
            else {
                canImport = targetNode == null;
            }

            if (support.isDrop()) {
                support.setDropAction(COPY);
                support.setShowDropLocation(canImport);
            }
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            MutableTreeNode targetNode = getTargetNode(support);
            
            if (targetNode instanceof ExhibitCollectionNode) {
                canImport = controller.canImportFile((ExhibitCollectionNode) targetNode);
            }
            else {
                canImport = targetNode == null;
            }
            
            if (support.isDrop()) {
                support.setDropAction(COPY);
                support.setShowDropLocation(canImport);
            }
        }
        else {
            canImport = false;
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
            
            controller.moveNode(sourceNode, (CollectionNode) targetNode);
            
            result = true;
        }
        else if (support.isDataFlavorSupported(TransferMuseumExhibit.Flavor.getInstance())) {
            List<MuseumExhibit> data = TransferMuseumExhibit.Flavor.getTransferMuseumExhibit(support);
            MutableTreeNode targetNode = getTargetNode(support);
            
            controller.importExhibit((CollectionNode) targetNode, data);
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
            
            result = controller.importFile((ExhibitCollectionNode) targetNode, fileList);
        }
        else {
            result = false;
        }
        
        return result;
    }
    
    MutableTreeNode getTargetNode(TransferSupport support) {
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
