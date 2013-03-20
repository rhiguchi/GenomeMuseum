package jp.scid.genomemuseum.model;

import java.io.File;
import java.util.Collection;
import java.util.List;

import jp.scid.bio.store.jooq.tables.records.GeneticSequenceRecord;

public interface MutableGeneticSequenceCollection extends GeneticSequenceCollection {
    GeneticSequenceRecord newElement();
    
    int remove(List<GeneticSequenceRecord> list);

    boolean addFile(File file);
    
    boolean add(GeneticSequenceRecord record);
    
    boolean add(Collection<GeneticSequenceRecord> record);

    boolean remove(GeneticSequenceRecord record);
}