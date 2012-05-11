package jp.scid.genomemuseum.gui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.util.List;

import jp.scid.genomemuseum.model.MuseumExhibit;

public interface TransferMuseumExhibit {
    List<MuseumExhibit> getMuseumExhibitList();
    
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
    }
}
