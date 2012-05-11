package jp.scid.genomemuseum.gui.transfer;

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

import jp.scid.genomemuseum.model.MuseumExhibit;

public class DefaultTransferMuseumExhibit implements Transferable, TransferMuseumExhibit {
    final List<MuseumExhibit> exhibits;
    final List<DataFlavor> flavors;

    public DefaultTransferMuseumExhibit(List<MuseumExhibit> exhibits) {
        this.exhibits = new ArrayList<MuseumExhibit>(exhibits);
        
        flavors = Arrays.asList(
                TransferMuseumExhibit.Flavor.getInstance(), DataFlavor.javaFileListFlavor);
    }
    
    public List<MuseumExhibit> getMuseumExhibitList() {
        return Collections.unmodifiableList(exhibits);
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
        
        if (TransferMuseumExhibit.Flavor.equals(flavor)) {
            data = this;
        }
        else if (DataFlavor.javaFileListFlavor.equals(flavor)) {
            List<File> files = null;
            
            for (MuseumExhibit exhibit: exhibits) {
                File file = exhibit.getSourceFileAsFile();
                
                if (file != null) {
                    if (files == null) {
                        files = new LinkedList<File>();
                    }
                    files.add(file);
                }
            }
            
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
        builder.append(exhibits);
        return builder.toString();
    }
}
