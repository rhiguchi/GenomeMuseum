package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jp.scid.genomemuseum.gui.transfer.TransferMuseumExhibit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListTransferHandler extends TransferHandler {
    private final static Logger logger = LoggerFactory.getLogger(ListTransferHandler.class);

    final ListController<?> controller;
    
    public ListTransferHandler(ListController<?> controller) {
        this.controller = controller;
    }
    
    @Override
    public boolean canImport(TransferSupport support) {
        if (support.isDataFlavorSupported(TransferMuseumExhibit.Flavor.getInstance())) {
            return controller.canAdd();
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return controller.canAdd();
        }
        return false;
    }
    
    @Override
    public boolean importData(TransferSupport support) {
        final boolean result;
        
        if (support.isDataFlavorSupported(TransferMuseumExhibit.Flavor.getInstance())) {
            logger.debug("entity import");
            
            result = false;
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            logger.debug("file import");
            
            int loadFileCount = 0;
            
            try {
                List<File> files = getTransferFile(support);
                
                for (File file: files) {
                    boolean accepted = controller.importFromFile(file);

                    if (accepted) {
                        loadFileCount++;
                    }
                }
            }
            catch (IOException e) {
                return false;
            }
            
            result = loadFileCount > 0;
        }
        else {
            result = false;
        }
        
        return result;
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        if (c.getClientProperty("Binding.controller") == controller
                && c instanceof JTable) {
            return COPY;
        }
        
        return super.getSourceActions(c);
    }
    
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c.getClientProperty("Binding.controller") == controller
                && c instanceof JTable) {
            return controller.createTransferData();
        }
        
        return super.createTransferable(c);
    }

    @SuppressWarnings("unchecked")
    static List<File> getTransferFile(TransferSupport support) throws IOException {
        List<File> files = Collections.emptyList();
        try {
            files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        }
        catch (UnsupportedFlavorException e) {
            throw new IOException(e);
        }
        return files;
    }
}