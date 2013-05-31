package jp.scid.genomemuseum.model;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

import jp.scid.bio.store.sequence.GeneticSequence;

public interface SequenceImportable {
    GeneticSequence deleteSequence(int index);
    
    GeneticSequence importSequence(File file) throws IOException, ParseException;
}