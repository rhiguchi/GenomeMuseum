package jp.scid.genomemuseum.gui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.TransferHandler.TransferSupport;

import jp.scid.genomemuseum.model.MuseumSourceModel.ExhibitCollectionNode;

public interface TransferExhibitCollectionNode {
    ExhibitCollectionNode getExhibitCollectionNode();
    
    class Flavor {
        private final static DataFlavor FLAVOR =
                new DataFlavor(TransferExhibitCollectionNode.class, "ExhibitCollectionNode");
        
        private Flavor() {
        }
        
        public static DataFlavor getInstance() {
            return FLAVOR;
        }
        
        static boolean equals(DataFlavor flavor) {
            return getInstance().equals(flavor);
        }

        static ExhibitCollectionNode getTransferExhibitCollectionNode(TransferSupport support) {
            TransferExhibitCollectionNode source;
            try {
                source = (TransferExhibitCollectionNode)
                        support.getTransferable().getTransferData(getInstance());
            }
            catch (UnsupportedFlavorException e) {
                throw new IllegalStateException("cannot get a TransferCollectionBoxNode", e);
            }
            catch (IOException e) {
                throw new IllegalStateException("cannot get a TransferCollectionBoxNode", e);
            }
            
            ExhibitCollectionNode sourceNode = source.getExhibitCollectionNode();
            return sourceNode;
        }
    }
}
