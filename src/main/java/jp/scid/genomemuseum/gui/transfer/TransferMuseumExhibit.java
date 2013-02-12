package jp.scid.genomemuseum.gui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import javax.swing.TransferHandler.TransferSupport;

import jp.scid.genomemuseum.model.MuseumExhibit;

public interface TransferMuseumExhibit {
    List<MuseumExhibit> getMuseumExhibitList();
    
    boolean hasCollectionOwner(); 
    
    long getCollectionOwnerId(); 
    
    class Flavor {
        private final static DataFlavor FLAVOR = new DataFlavor(TransferMuseumExhibit.class, "MuseumExhibit");
        
        private Flavor() {
        }
        
        public static DataFlavor getInstance() {
            return FLAVOR;
        }
        
        static boolean equals(DataFlavor flavor) {
            return getInstance().equals(flavor);
        }

        public static List<MuseumExhibit> getTransferMuseumExhibit(TransferSupport support) {
            TransferMuseumExhibit source;
            try {
                source = (TransferMuseumExhibit)
                        support.getTransferable().getTransferData(getInstance());
            }
            catch (UnsupportedFlavorException e) {
                throw new IllegalStateException("cannot get a TransferMuseumExhibit", e);
            }
            catch (IOException e) {
                throw new IllegalStateException("cannot get a TransferMuseumExhibit", e);
            }
            
            List<MuseumExhibit> list = source.getMuseumExhibitList();
            return list;
        }
    }
}
