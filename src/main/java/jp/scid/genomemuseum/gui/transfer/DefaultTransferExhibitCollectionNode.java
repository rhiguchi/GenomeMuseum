package jp.scid.genomemuseum.gui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jp.scid.genomemuseum.model.MuseumSourceModel.ExhibitCollectionNode;

public class DefaultTransferExhibitCollectionNode implements TransferExhibitCollectionNode, Transferable {
    final ExhibitCollectionNode node;
    final List<DataFlavor> flavors;
    
    public DefaultTransferExhibitCollectionNode(ExhibitCollectionNode node) {
        this.node = node;
        
        flavors = Arrays.asList(
                TransferExhibitCollectionNode.Flavor.getInstance(), DataFlavor.javaFileListFlavor);
    }

    @Override
    public ExhibitCollectionNode getExhibitCollectionNode() {
        return node;
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
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        final Object data;
        
        if (TransferExhibitCollectionNode.Flavor.equals(flavor)) {
            data = this;
        }
        else {
            data = null;
        }
        return data;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("node=");
        builder.append(node);
        return builder.toString();
    }
}
