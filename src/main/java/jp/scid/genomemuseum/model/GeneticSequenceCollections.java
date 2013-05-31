package jp.scid.genomemuseum.model;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

import javax.swing.ListModel;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.folder.CollectionType;
import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.sequence.GeneticSequence;

public class GeneticSequenceCollections {
    public static GeneticSequenceCollection fromSequenceLibrary(SequenceLibrary library) {
        SequenceLibraryGeneticSequenceCollection collection = new SequenceLibraryGeneticSequenceCollection(library);
        return collection;
    }
}

class SequenceLibraryGeneticSequenceCollection implements GeneticSequenceCollection, SequenceImportable {
    private final SequenceLibrary library;
    
    public SequenceLibraryGeneticSequenceCollection(SequenceLibrary library) {
        this.library = library;
    }

    @Override
    public ListModel getCollection() {
        return library.getAllSequences();
    }

    @Override
    public GeneticSequence importSequence(File file) throws IOException, ParseException {
        return library.importSequence(file);
    }
    
    @Override
    public int remove(Collection<GeneticSequence> list) {
        // TODO Auto-generated method stub
        return 0;
    }
}

class FolderTreeNodeAdapter implements FolderTreeNode {
    private final Folder folder;

    public FolderTreeNodeAdapter(Folder folder) {
        this.folder = folder;
    }

    @Override
    public FolderContainer getParentContainer() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
}