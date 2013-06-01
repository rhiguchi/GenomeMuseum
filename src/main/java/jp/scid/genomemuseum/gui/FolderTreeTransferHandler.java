package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.sequence.FolderContentGeneticSequence;
import jp.scid.bio.store.sequence.SequenceCollection;
import jp.scid.genomemuseum.model.FolderContainer;
import jp.scid.genomemuseum.model.FolderTreeNode;
import jp.scid.genomemuseum.model.SequenceImportable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeTransferHandler extends TransferHandler {
    public final static DataFlavor FOLDER_FLAVOR = new DataFlavor(Folder.class, "Folder");
    
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
        
        if (support.isDataFlavorSupported(FOLDER_FLAVOR)) {
            Folder folder = getTransferFolder(support);
            if (folder == null) {
                return false;
            }
            
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
        else if (support.isDataFlavorSupported(GeneticSequenceList.FLAVOR)) {
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

    static Folder getTransferFolder(TransferSupport support) {
        try {
            return (Folder) support.getTransferable().getTransferData(FOLDER_FLAVOR);
        }
        catch (UnsupportedFlavorException e) {
            logger.error("cannot get transfer Folder", e);
            return null;
        }
        catch (IOException e) {
            logger.error("cannot get transfer Folder", e);
            return null;
        }
    }

    @Override
    public boolean importData(TransferSupport support) {
        final boolean result;
        
        if (support.isDataFlavorSupported(FOLDER_FLAVOR)) {
            Folder folder = getTransferFolder(support);
            if (folder == null) {
                return false;   
            }
//            ExhibitCollectionNode sourceNode =
//                    TransferExhibitCollectionNode.Flavor.getTransferExhibitCollectionNode(support);
            MutableTreeNode targetNode = getTargetNode(support);
            
//            controller.moveNode(sourceNode, (CollectionNode) targetNode);
            
            result = true;
        }
        else if (support.isDataFlavorSupported(GeneticSequenceList.FLAVOR)) {
//            List<MuseumExhibit> data = GeneticSequenceList.Flavor.getTransferMuseumExhibit(support);
//            MutableTreeNode targetNode = getTargetNode(support);
            
//            controller.importExhibit((CollectionNode) targetNode, data);
            result = true;
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            List<File> fileList = GeneticSequenceListTransferHandler.getTransferFile(support);
            MutableTreeNode targetNode = getTargetNode(support);
            
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
        if (c instanceof JTree && ((JTree) c).getModel() == controller.getTreeModel()) {
            return createTransferData();
        }
        
        return super.createTransferable(c);
    }

    private Transferable createTransferData() {
        TreePath path = controller.getSelectionPath();
        if (path == null) {
            return null;
        }
        
        if (!(path.getLastPathComponent() instanceof Folder)) {
            Folder folder = (Folder) path.getLastPathComponent();
            
            List<FolderContentGeneticSequence> contentList = new LinkedList<FolderContentGeneticSequence>();
            SequenceCollection<FolderContentGeneticSequence> contents = folder.getContentSequences();
            for (int index = 0; index < contents.getSize(); index++) {
                contentList.add(contents.getElementAt(index));
            }
            
            return new FolderTransferObject(folder, contentList);
        }
        
        return null;
    }
    

    class FolderTransferObject implements Transferable {
        private final Folder element;
        private final ProxyGeneticSequenceTransferObject contents;
        private List<DataFlavor> flavors = null;
        
        public FolderTransferObject(Folder element, List<FolderContentGeneticSequence> contentList) {
            if (element == null) throw new IllegalArgumentException("element must not be null");
            this.element = element;
            
            if (contentList == null || contentList.isEmpty()) {
                contents = null;
            }
            else {
                contents = new ProxyGeneticSequenceTransferObject(contentList);
            }
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return getFlavors().toArray(new DataFlavor[0]);
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return getFlavors().contains(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (getFlavors().contains(flavor)) {
                if (flavor.equals(FOLDER_FLAVOR)) {
                    return element;
                }
                else if (flavor.equals(DataFlavor.stringFlavor)) {
                    return getText();
                }
                else if (flavor.equals(GeneticSequenceList.FLAVOR)) {
                    return contents;
                }
                else if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                    return ProxyGeneticSequenceTransferObject.getFiles(contents);
                }
            }
            
            throw new UnsupportedFlavorException(flavor);
        }
        
        public String getText() {
            return element.toString();
        }
        
        public List<DataFlavor> getFlavors() {
            if (flavors == null) {
                flavors = new ArrayList<DataFlavor>(3);
                flavors.add(FOLDER_FLAVOR);
                flavors.add(DataFlavor.stringFlavor);
                
                if (contents != null) {
                    flavors.add(GeneticSequenceList.FLAVOR);
                    if (contents.hasFile()) {
                        flavors.add(DataFlavor.javaFileListFlavor);
                    }
                }
            }
            return flavors;
        }
    }
}
