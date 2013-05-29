package jp.scid.genomemuseum.model;

import java.io.File;
import java.util.Collection;
import java.util.List;

import jp.scid.bio.store.jooq.tables.records.GeneticSequenceRecord;
import jp.scid.bio.store.sequence.GeneticSequence;

public interface MutableGeneticSequenceCollection extends GeneticSequenceCollection {
    

    boolean add(GeneticSequenceRecord record);
    
    boolean add(Collection<GeneticSequenceRecord> record);
}