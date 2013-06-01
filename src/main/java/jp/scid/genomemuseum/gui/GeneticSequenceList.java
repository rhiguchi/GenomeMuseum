package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.util.List;

import jp.scid.bio.store.sequence.GeneticSequence;

public interface GeneticSequenceList extends List<GeneticSequence> {
    public final static DataFlavor FLAVOR = new DataFlavor(GeneticSequenceList.class, "GeneticSequenceList");
}
