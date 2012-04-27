package jp.scid.genomemuseum.model;

import org.jooq.impl.Factory;

public class MuseumDataSchema {
    private final Factory factory;
    
    private CollectionBoxService collectionBoxService = null;
    
    private MuseumExhibitLibrary library = null;
    
    MuseumDataSchema(Factory factory) {
        this.factory = factory;
    }
    
    public CollectionBoxService getCollectionBoxService() {
        if (collectionBoxService == null) {
            collectionBoxService = new CollectionBoxService(factory);
        }
        
        return collectionBoxService;
    }
    
    public MuseumExhibitLibrary getMuseumExhibitLibrary() {
        if (library == null) {
            library = new MuseumExhibitLibrary(factory);
        }
        return library;
    }
    
}

interface FreeExhibitService {
    
}