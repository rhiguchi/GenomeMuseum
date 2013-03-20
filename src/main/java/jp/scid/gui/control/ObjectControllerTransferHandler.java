package jp.scid.gui.control;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jp.scid.gui.control.ObjectControllerTransferHandler.TransferData;

import ca.odell.glazedlists.swing.EventTableModel;

public class ObjectControllerTransferHandler extends TransferHandler {
    protected Transferable createTableTransferData(JTable table) {
        if (!(table.getModel() instanceof EventTableModel))
            return null;
        
        if (table.getSelectedRowCount() == 0)
            return null;
        
        EventTableModel<?> model = (EventTableModel<?>) table.getModel();
        int[] selectedRows = table.getSelectedRows();
        
        List<Object> selectedElements = new ArrayList<Object>(selectedRows.length);
        List<File> fileList = new ArrayList<File>(selectedRows.length);
        
        for (int row: selectedRows) {
            Object element = model.getElementAt(row);
            selectedElements.add(element);
            
            File file = getSourceFile(element);
            if (file != null)
                fileList.add(file);
        }
        
        TransferDataImpl data = new TransferDataImpl(model, selectedElements, fileList);
        return data;
    }
    
    protected Transferable createTreeTransferData(JTree tree) {
        TreeModel model = tree.getModel();
        TreePath[] selectionPaths = tree.getSelectionPaths();
        
        if (selectionPaths == null || selectionPaths.length == 0)
            return null;
        
        List<TreePath> selectedElements = Arrays.asList(selectionPaths);
        List<File> fileList = new ArrayList<File>(selectionPaths.length);
        
        for (TreePath path: selectionPaths) {
            File file = getSourceFile(path);
            
            if (file != null)
                fileList.add(file);
        }
        
        TransferDataImpl data = new TransferDataImpl(model, selectedElements, fileList);
        return data;
    }
    
    protected File getSourceFile(TreePath element) {
        return null;
    }
    
    protected File getSourceFile(Object element) {
        return null;
    }
    
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTable)
            return createTableTransferData((JTable) c);
        else if (c instanceof JTree)
            return createTreeTransferData((JTree) c);
        
        return super.createTransferable(c);
    }
    
    public boolean importFile(int rowIndex, List<File> fileList) {
        return false;
    }
    
    public boolean importTransferData(int rowIndex, TransferData data) {
        return false;
    }
    
    @Override
    public boolean importData(TransferSupport support) {
        if (support.isDataFlavorSupported(TransferData.flavor)) {
            TransferData data = getTransferData(support.getTransferable());
            int index = getInsertRowIndex(support);
            
            return importTransferData(index, data);
            
        }
        else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            List<File> fileList = getFileList(support.getTransferable());
            int index = getInsertRowIndex(support);
            
            return importFile(index, fileList);
        }
        else {
            return super.importData(support);
        }
    }
    
    int getInsertRowIndex(TransferSupport support) {
        int index = -1;
        
        if (support.getDropLocation() instanceof JTable.DropLocation) {
            JTable.DropLocation loc = (JTable.DropLocation) support.getDropLocation();
            index = loc.getRow();
        }
        else if (support.getComponent() instanceof JTree) {
            JTree loc = (JTree) support.getComponent();
            Point point = support.getDropLocation().getDropPoint();
            index = loc.getRowForLocation(point.x, point.y);
        }
        return index;
    }
    
    @SuppressWarnings("unchecked")
    List<File> getFileList(Transferable t) {
        List<File> fileList = Collections.emptyList();
        try {
            fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
        }
        catch (UnsupportedFlavorException e) {
            throw new IllegalStateException(e);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return fileList;
    }
    
    TransferData getTransferData(Transferable t) {
        TransferData data = null;
        try {
            data = (TransferData) t.getTransferData(TransferData.flavor);
        }
        catch (UnsupportedFlavorException e) {
            throw new IllegalStateException(e);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return data;
    }

    /** TransferData */
    public static interface TransferData {
        public static final DataFlavor flavor = new DataFlavor(TransferData.class, "EventListController.TransferData");
        
        Object getSourceModel();
        List<?> getSelectedElements();
    }
}

/** TransferData implementation */
class TransferDataImpl implements Transferable, TransferData {
    private final Object sourceModel;
    private final List<?> selectedElements;
    private final List<File> fileList;
    
    public TransferDataImpl(Object sourceModel, List<?> rowElements, List<File> fileList) {
        this.sourceModel = sourceModel;
        this.selectedElements = new ArrayList<Object>(rowElements);
        this.fileList = fileList == null ? Collections.<File>emptyList() : new ArrayList<File>(fileList);
    }
    
    public TransferDataImpl(Object sourceModel, List<?> rowElements) {
        this(sourceModel, rowElements, null);
    }
    
    public Object getSourceModel() {
        return sourceModel;
    }
    
    public List<?> getSelectedElements() {
        return selectedElements;
    }
    
    boolean containsFileElement() {
        return !fileList.isEmpty();
    }
    
    List<File> getTransferFileList() {
        return fileList;
    }
    
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        List<DataFlavor> flavors = new LinkedList<DataFlavor>();
        flavors.add(TransferData.flavor);
        if (containsFileElement())
            flavors.add(DataFlavor.javaFileListFlavor);
        
        return flavors.toArray(new DataFlavor[0]);
    }
    
    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (TransferData.flavor.equals(flavor))
            return true;
        if (DataFlavor.javaFileListFlavor.equals(flavor) && containsFileElement())
            return true;
        
        return false;
    }
    
    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (TransferData.flavor.equals(flavor))
            return this;
        
        if (DataFlavor.javaFileListFlavor.equals(flavor) && containsFileElement())
            return getTransferFileList();
        
        throw new UnsupportedFlavorException(flavor);
    }
}
