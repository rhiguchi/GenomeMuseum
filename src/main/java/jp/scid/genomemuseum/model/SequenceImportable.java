package jp.scid.genomemuseum.model;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.concurrent.Callable;

import jp.scid.bio.store.sequence.GeneticSequence;

@Deprecated
public interface SequenceImportable {
    GeneticSequence deleteSequence(int index);
    
    GeneticSequence importSequence(File file) throws IOException, ParseException;
    
    Callable<GeneticSequence> createSequenceImportTask(File file);
}