package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListTransferData<E> implements Transferable {
    public final static DataFlavor FLAVOR = new DataFlavor(ListTransferData.class, "ListTransferData");
    
    final List<E> elements;
    
    List<File> files = null;
    
    final Set<DataFlavor> flavors;

    public ListTransferData(List<E> elements) {
        this.elements = new ArrayList<E>(elements);
        
        flavors = new HashSet<DataFlavor>();
        flavors.add(FLAVOR);
    }
    
    public void setFiles(List<File> files) {
        this.files = files;
        if (files == null || files.isEmpty()) {
            flavors.remove(DataFlavor.javaFileListFlavor);
        }
        else if (!flavors.contains(DataFlavor.javaFileListFlavor)) {
            flavors.add(DataFlavor.javaFileListFlavor);
        }
    }
    
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.toArray(new DataFlavor[0]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavors.contains(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        final Object data;
        
        if (FLAVOR.equals(flavor)) {
            data = this;
        }
        else
            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
            data = files;
        }
        else if (DataFlavor.stringFlavor.equals(flavor)) {
            // TODO Auto-generated method stub
            data = "not implemented";
        }
        else {
            data = null;
        }
        
        return data;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("exhibits=");
        builder.append(elements);
        return builder.toString();
    }
}
