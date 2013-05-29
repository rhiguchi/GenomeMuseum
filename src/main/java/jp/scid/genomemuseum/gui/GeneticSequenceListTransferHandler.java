package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jp.scid.genomemuseum.gui.transfer.TransferMuseumExhibit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticSequenceListTransferHandler extends TransferHandler {
    private final static Logger logger = LoggerFactory.getLogger(GeneticSequenceListTransferHandler.class);

    final GeneticSequenceListController controller;
    
    public GeneticSequenceListTransferHandler(GeneticSequenceListController controller) {
        this.controller = controller;
    }
    
    @Override
    public boolean canImport(TransferSupport support) {
        logger.debug("canImport");
        
        if (support.isDataFlavorSupported(TransferMuseumExhibit.Flavor.getInstance())) {
            return controller.canAdd();
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return controller.canImportFile();
        }
        return false;
    }

    private boolean importFiles(Collection<File> files) throws IOException {
        boolean result = false;
        
        for (File file: files) {
            if (file.isDirectory()) {
                Collection<File> children =
                        FileUtils.listFiles(file, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE);
                result |= importFiles(children);
                continue;
            }
            
            // File
            result |= controller.importFile(file);
        }
        
        return result;
    }
    
    @Override
    public boolean importData(TransferSupport support) {
        logger.debug("importData");
        
        if (support.isDataFlavorSupported(TransferMuseumExhibit.Flavor.getInstance())) {
            logger.debug("entity import");
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            logger.debug("file import");
            
            List<File> files = getTransferFile(support);
            try {
                return importFiles(files);
            }
            catch (IOException e) {
                logger.error("cannot import files", e);
                return false;
            }
            catch (RuntimeException e) {
                logger.error("cannot import files", e);
            }
        }
        
        return false;
    }
    
    @Override
    public int getSourceActions(JComponent c) {
        if (c instanceof JTable) {
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
    static List<File> getTransferFile(TransferSupport support) {
        List<File> files = Collections.emptyList();
        try {
            files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        }
        catch (UnsupportedFlavorException e) {
            logger.error("cannot import files", e);
        }
        catch (IOException e) {
            logger.error("cannot import files", e);
        }
        return files;
    }
}