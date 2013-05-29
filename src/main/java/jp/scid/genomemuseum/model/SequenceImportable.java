package jp.scid.genomemuseum.model;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

import jp.scid.bio.store.folder.CollectionType;
import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderTreeNode;

public interface SequenceImportable {
    int remove(Collection<GeneticSequence> list);
    
    FolderTreeNode addChild(CollectionType type);
    
    GeneticSequence importSequence(File file) throws IOException, ParseException;
}