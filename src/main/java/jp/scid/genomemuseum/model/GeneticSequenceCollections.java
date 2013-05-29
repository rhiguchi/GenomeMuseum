package jp.scid.genomemuseum.model;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.swing.ListModel;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.folder.CollectionType;
import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderContainer;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderTreeNode;

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
    public FolderTreeNode addChild(CollectionType type) {
        Folder folder = library.addFolder(type);
        return new FolderTreeNodeAdapter(folder);
    }

    @Override
    public GeneticSequence importSequence(File file) throws IOException, ParseException {
        return library.importSequence(file);
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