package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jp.scid.bio.store.sequence.GeneticSequence;

import org.apache.commons.io.FileUtils;
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
        
        if (support.isDataFlavorSupported(GeneticSequenceList.FLAVOR)) {
            return controller.canAdd();
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return controller.canImportFile();
        }
        return false;
    }

    private boolean importFiles(Collection<File> base) throws IOException {
        List<File> files = new LinkedList<File>();
        
        for (File file: base) {
            if (file.isDirectory()) {
                Collection<File> children = FileUtils.listFiles(file, null, true);
                files.addAll(children);
            }
            else {
                files.add(file);
            }
        }
        
        for (Iterator<File> it = files.iterator(); it.hasNext();) {
            File file = it.next();
            if (file.isHidden()) {
                it.remove();
            }
        }
        
        controller.importFiles(files);
        
        return !files.isEmpty();
    }
    
    @Override
    public boolean importData(TransferSupport support) {
        logger.debug("importData");
        
        if (support.isDataFlavorSupported(GeneticSequenceList.FLAVOR)) {
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
        if (c instanceof JTable) {
            return createTransferData();
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

    // transferring
    protected Transferable createTransferData() {
        if (controller.isSelectionEmpty()) {
            return null;
        }
        
        List<GeneticSequence> selections = controller.getSelections();
        return new GeneticSequenceTransferObject(selections);
    }
    
    private CharSequence getElementText(GeneticSequence sequence) {
        // TODO
        return sequence.toString();
    }
    
    static GeneticSequenceList getTransferGeneticSequenceList(TransferSupport support) {
        GeneticSequenceList seqList;
        try {
            seqList = (GeneticSequenceList) support.getTransferable().getTransferData(GeneticSequenceList.FLAVOR);
        }
        catch (UnsupportedFlavorException e) {
            logger.error("cannot import", e);
            throw new IllegalStateException(e);
        }
        catch (IOException e) {
            logger.error("cannot import", e);
            throw new IllegalStateException(e);
        }
        return seqList;
    }

    class GeneticSequenceTransferObject implements Transferable {
        private final ProxyGeneticSequenceTransferObject elements;
        private List<DataFlavor> flavors = null;
        
        public GeneticSequenceTransferObject(List<GeneticSequence> elements) {
            this.elements = new ProxyGeneticSequenceTransferObject(elements);
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
                if (flavor.equals(GeneticSequenceList.FLAVOR)) {
                    return elements;
                }
                else if (flavor.equals(DataFlavor.stringFlavor)) {
                    return getText();
                }
                else if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                    return ProxyGeneticSequenceTransferObject.getFiles(elements);
                }
            }
            
            throw new UnsupportedFlavorException(flavor);
        }
        
        public String getText() {
            String ln = System.getProperty("line.separator");
            StringBuilder text = new StringBuilder();
            
            for (GeneticSequence sequence: elements) {
                CharSequence elementText = getElementText(sequence);
                if (elementText != null) {
                    text.append(elementText).append(ln);
                }
            }
            return text.toString();
        }
        
        public List<DataFlavor> getFlavors() {
            if (flavors == null) {
                flavors = new ArrayList<DataFlavor>(3);
                flavors.add(GeneticSequenceList.FLAVOR);
                flavors.add(DataFlavor.stringFlavor);
                
                if (elements.hasFile()) {
                    flavors.add(DataFlavor.javaFileListFlavor);
                }
            }
            return flavors;
        }
    }
}

class ProxyGeneticSequenceTransferObject extends AbstractList<GeneticSequence> implements GeneticSequenceList {
    private final List<GeneticSequence> delegate;
    
    public ProxyGeneticSequenceTransferObject(List<? extends GeneticSequence> delegate) {
        this.delegate = new ArrayList<GeneticSequence>(delegate);
    }
    
    public boolean hasFile() {
        for (GeneticSequence sequence: delegate) {
            if (sequence.getFile() != null) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public GeneticSequence get(int index) {
        return delegate.get(index);
    }
    
    @Override
    public int size() {
        return delegate.size();
    }
    
    @Override
    public Iterator<GeneticSequence> iterator() {
        return delegate.iterator();
    }

    static List<File> getFiles(Collection<GeneticSequence> elements) {
        List<File> files = new LinkedList<File>();
        
        for (GeneticSequence sequence: elements) {
            File file = sequence.getFile();
            if (file != null) {
                files.add(file);
            }
        }
        return files;
    }
}
