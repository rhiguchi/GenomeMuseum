package jp.scid.genomemuseum.gui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jp.scid.genomemuseum.gui.ExhibitListViewController;
import jp.scid.genomemuseum.model.MuseumExhibit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExhibitTransferHandler extends TransferHandler {
    private final static Logger logger = LoggerFactory.getLogger(ExhibitTransferHandler.class);
    
    final ExhibitListViewController controller;
    
    public ExhibitTransferHandler(ExhibitListViewController controller) {
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
            result = false;
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            List<File> files = listFiles(getImportFileList(support));
            int loadFileCount = 0;
            
            for (File file: files) {
                boolean accepted = controller.addExhibitFromFile(file);
                if (accepted) {
                    loadFileCount++;
                }
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
        if (c instanceof JTable && ((JTable) c).getModel() == controller.getTableModel()) {
            return COPY;
        }
        
        return super.getSourceActions(c);
    }
    
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTable && ((JTable) c).getModel() == controller.getTableModel()) {
            List<MuseumExhibit> list = controller.getSelections();
            
            final DefaultTransferMuseumExhibit data;
            if (!list.isEmpty()) {
                data = new DefaultTransferMuseumExhibit(list);
            }
            else {
                data = null;
            }
            
            return data;
        }
        
        return super.createTransferable(c);
    }

    @SuppressWarnings("unchecked")
    List<File> getImportFileList(TransferSupport support) {
        List<File> files = Collections.emptyList();
        try {
            files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        }
        catch (UnsupportedFlavorException e) {
            logger.warn("cannot import file", e);
        }
        catch (IOException e) {
            logger.warn("cannot import file", e);
        }
        return files;
    }
    
    List<File> listFiles(List<File> base) {
        List<File> list = new LinkedList<File>();
        
        for (File file: base) {
            if (file.isDirectory()) {
                Collection<File> files =
                        FileUtils.listFiles(file, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE);
                list.addAll(files);
            }
            else {
                list.add(file);
            }
        }
        
        return list;
    }
}