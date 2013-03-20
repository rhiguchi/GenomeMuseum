package jp.scid.genomemuseum.model;

import java.util.List;

import jp.scid.bio.store.jooq.tables.records.GeneticSequenceRecord;

public interface GeneticSequenceLibrary extends MutableGeneticSequenceCollection {
    int remove(List<GeneticSequenceRecord> list, boolean deleteFile);
}