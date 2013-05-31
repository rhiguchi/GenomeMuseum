package jp.scid.genomemuseum.model;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

import jp.scid.bio.store.folder.CollectionType;
import jp.scid.bio.store.sequence.GeneticSequence;

public interface SequenceImportable {
    int remove(Collection<GeneticSequence> list);
    
    GeneticSequence importSequence(File file) throws IOException, ParseException;
}