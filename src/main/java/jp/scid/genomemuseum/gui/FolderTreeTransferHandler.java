package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.folder.FolderRecordBasicFolder;
import jp.scid.bio.store.folder.FoldersContainer;
import jp.scid.bio.store.sequence.FolderContentGeneticSequence;
import jp.scid.bio.store.sequence.ImportableSequenceSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreeTransferHandler extends TransferHandler {
    private final static DataFlavor FOLDER_FLAVOR = new DataFlavor(Folder.class, "Folder");
    
    final static Logger logger = LoggerFactory.getLogger(FolderTreeTransferHandler.class);
    
    final private FolderTreeController treeController;
    
    public FolderTreeTransferHandler(FolderTreeController treeController) {
        this.treeController = treeController;
    }

    // Folder transfer
    int getFolderImportAction(TransferSupport support) {
        Folder folder = getTransferFolder(support);
        
        Object targetObject = getTargetNodeObject(support);
        if (targetObject instanceof FoldersContainer) {
            boolean canImport = ((FoldersContainer) targetObject).canAddChild(folder);
            return canImport ? MOVE : NONE;
        }
        else if (targetObject instanceof FolderRecordBasicFolder) {
            return COPY;
        }
        
        return MOVE;
    }
    
    boolean importFolder(TransferSupport support) {
        Folder folder = getTransferFolder(support);
        Object targetObject = getTargetNodeObject(support);
        
        if (targetObject instanceof FoldersContainer) {
            return treeController.moveTo(folder, (FoldersContainer) targetObject);
        }
        else if (targetObject instanceof FolderRecordBasicFolder) {
            FolderRecordBasicFolder dest = ((FolderRecordBasicFolder) targetObject);
            dest.addAllSequences(folder.getGeneticSequences());
            return true;
        }
        
        return treeController.moveTo(folder, null);
    }
    
    // Sequences transfer
    int getSequenceListImportAction(TransferSupport support) {
        Object targetObject = getTargetNodeObject(support);
        return targetObject instanceof FolderRecordBasicFolder ? COPY : NONE;
    }
    
    boolean importSequenceList(TransferSupport support) {
        FolderRecordBasicFolder dest = ((FolderRecordBasicFolder) getTargetNodeObject(support));
        
        GeneticSequenceList seqList = GeneticSequenceListTransferHandler.getTransferGeneticSequenceList(support);
        return !dest.addAllSequences(seqList).isEmpty();
    }
    
    // Files transfer
    public int getFileImportAction(TransferSupport support) {
        Object targetObject = getTargetNodeObject(support);
        return targetObject instanceof ImportableSequenceSource ? COPY : NONE;
    }
    
    public boolean importFile(TransferSupport support) {
        List<File> fileList = GeneticSequenceListTransferHandler.getTransferFile(support);
        ImportableSequenceSource target = (ImportableSequenceSource) getTargetNodeObject(support);
//        taskController.executeLoading(fileList, target);
        
        return false;
    }
    
    @Override
    public boolean canImport(TransferSupport support) {
        Object targetObject = getTargetNodeObject(support);
        logger.debug("ask canImport for target {}", targetObject);
        
        final int action;
        
        // Folder moving
        if (support.isDataFlavorSupported(FOLDER_FLAVOR)) {
            action = getFolderImportAction(support);
        }
        // Sequences
        else if (support.isDataFlavorSupported(GeneticSequenceList.FLAVOR)) {
            action = getSequenceListImportAction(support);
        }
        // Files
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            action = getFileImportAction(support);
        }
        else {
            return false;
        }
        
        if (action == NONE) {
            return false;
        }
        
        if (support.isDrop()) {
            support.setDropAction(action);
            support.setShowDropLocation(true);
        }
        return true;
    }

    static Folder getTransferFolder(TransferSupport support) {
        try {
            return (Folder) support.getTransferable().getTransferData(FOLDER_FLAVOR);
        }
        catch (UnsupportedFlavorException e) {
            logger.error("cannot get transfer Folder", e);
            throw new IllegalStateException(e);
        }
        catch (IOException e) {
            logger.error("cannot get transfer Folder", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean importData(TransferSupport support) {
        Object targetObject = getTargetNodeObject(support);
        logger.debug("importData to target {}", targetObject);
        
        final boolean result;
        
        // folder move
        if (support.isDataFlavorSupported(FOLDER_FLAVOR)) {
            result = importFolder(support);
        }
        // Sequence Records
        else if (support.isDataFlavorSupported(GeneticSequenceList.FLAVOR)) {
            result = importSequenceList(support);
        }
        /// Files
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            result = importFile(support);
        }
        // the others
        else {
            return false;
        }
        
        return result;
    }

    private static Object getTargetNodeObject(TransferSupport support) {
        TreePath targetPath = getTargetTreePath(support);
        
        return getLastPathObject(targetPath);
    }

    private static Object getLastPathObject(TreePath targetPath) {
        Object lastPathObject = targetPath == null ? null : targetPath.getLastPathComponent();
        if (lastPathObject instanceof DefaultMutableTreeNode) {
            return ((DefaultMutableTreeNode) lastPathObject).getUserObject();
        }
        return null;
    }
    
    private static TreePath getTargetTreePath(TransferSupport support) {
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
        return targetPath;
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        if (c instanceof JTree && getLastPathObject(((JTree) c).getSelectionPath()) != null) {
            return COPY_OR_MOVE;
        }
        
        return super.getSourceActions(c);
    }
    
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTree) {
            Object lastPathObject = getLastPathObject(((JTree) c).getSelectionPath());
            if (lastPathObject instanceof Folder) {
                return createTransferData((Folder) lastPathObject);
            }
        }
        
        return null;
    }

    private Transferable createTransferData(Folder folder) {
        return new FolderTransferObject(folder, folder.getGeneticSequences());
    }
    

    static class FolderTransferObject implements Transferable {
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
