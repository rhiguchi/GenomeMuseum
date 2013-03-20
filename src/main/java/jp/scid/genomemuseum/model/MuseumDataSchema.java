package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.util.List;

import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.jooq.UpdatableTable;
import org.jooq.impl.Factory;

public class MuseumDataSchema {
    private final Factory factory;
    
    private CollectionBoxService collectionBoxService = null;
    
    private MuseumExhibitLibrary library = null;
    
    MuseumDataSchema(Factory factory) {
        this.factory = factory;
    }
    
    Factory getFactory() {
        return factory;
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
//            library.exhibitService = new MuseumExhibitService(factory, MUSEUM_EXHIBIT);
        }
        return library;
    }
    
    public static class MuseumExhibitService extends JooqEntityService<MuseumExhibit, MuseumExhibitRecord> {
        MuseumExhibitService(Factory factory, UpdatableTable<MuseumExhibitRecord> table) {
            super(factory, table);
        }
        
        @Override
        protected MuseumExhibit createElement(MuseumExhibitRecord record) {
            MuseumExhibit exhibit = new MuseumExhibit(record);
            
            return exhibit;
        }

        @Override
        protected MuseumExhibitRecord recordOfElement(MuseumExhibit element) {
            return element.getRecord();
        }
    }
}
