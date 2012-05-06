package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jp.scid.bio.Fasta;
import jp.scid.bio.FastaFormat;
import jp.scid.bio.GenBank;
import jp.scid.bio.GenBankFormat;
import jp.scid.bio.SequenceBioDataFormat;
import jp.scid.bio.SequenceBioDataFormatSearcher;
import jp.scid.bio.SequenceBioDataReader;
import jp.scid.genomemuseum.model.MuseumDataSchema.MuseumExhibitService;
import jp.scid.genomemuseum.model.MuseumExhibit.FileType;
import jp.scid.genomemuseum.model.sql.tables.records.CollectionBoxItemRecord;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.jooq.Record;
import org.jooq.RecordHandler;
import org.jooq.impl.Factory;

public class MuseumExhibitLibrary extends AbstractExhibitListModel implements ExhibitLibrary {
    private final GenBankFormat genBankFormat;
    private final FastaFormat fastaFormat;
    
    private final SequenceBioDataFormatSearcher searcher;
    
    final Factory factory;
    
    MuseumExhibitService exhibitService;
    
    @SuppressWarnings("unchecked")
    MuseumExhibitLibrary(Factory factory) {
        genBankFormat = new GenBankFormat();
        fastaFormat = new FastaFormat();
        searcher = new SequenceBioDataFormatSearcher(Arrays.asList(genBankFormat, fastaFormat));
        
        this.factory = factory;
    }
    
    public MuseumExhibit newMuseumExhibit() {
        return exhibitService.newElement();
    }
    
    @Override
    public boolean storeExhibit(MuseumExhibit exhibit) {
        return exhibitService.store(exhibit);
    }
    
    
    @Override
    public List<MuseumExhibit> fetchExhibits() {
        List<MuseumExhibit> list = exhibitService.search(null);
        return list;
    }

    @Override
    public boolean deleteExhibit(MuseumExhibit exhibit) {
        return exhibitService.delete(exhibit);
    }
    
    int recourdCount() {
        return exhibitService.getCount();
    }
    
    public List<MuseumExhibit> getBoxContents(long boxId) {
        ContentsExhibitHandler handler = new ContentsExhibitHandler();
        
        List<MuseumExhibit> elements = factory.select()
                .from(COLLECTION_BOX_ITEM)
                .join(MUSEUM_EXHIBIT)
                .on(COLLECTION_BOX_ITEM.EXHIBIT_ID.equal(MUSEUM_EXHIBIT.ID))
                .where(COLLECTION_BOX_ITEM.BOX_ID.equal(boxId))
                .orderBy(COLLECTION_BOX_ITEM.ID)
                .fetchInto(handler)
                .getElements();
        
        return elements;
    }
    
    public int addBoxContent(long boxId, MuseumExhibit exhibit) {
        CollectionBoxItemRecord record = factory.insertInto(COLLECTION_BOX_ITEM)
                .set(COLLECTION_BOX_ITEM.BOX_ID, boxId)
                .set(COLLECTION_BOX_ITEM.EXHIBIT_ID, exhibit.getId())
                .returning().fetchOne();
        
        Integer count = factory.selectCount().from(COLLECTION_BOX_ITEM)
        .where(COLLECTION_BOX_ITEM.BOX_ID.equal(boxId))
        .and(COLLECTION_BOX_ITEM.ID.lessThan(record.getId())).fetchOne(0, Integer.class);
        
        return count;
    }
    
    public MuseumExhibit newElement() {
        MuseumExhibitRecord record = factory.newRecord(MUSEUM_EXHIBIT);
        record.setName("Untitled");
        MuseumExhibit newExibit = new MuseumExhibit(record);
        return newExibit;
    }

    public boolean save(MuseumExhibit element) {
        int count = element.getRecord().store();
        return count > 0;
    }
    
    public boolean delete(MuseumExhibit exhibit) {
        return deleteExhibit(exhibit, false);
    }
    
    public boolean deleteContent(Long contentId) {
        int count = factory.executeDeleteOne(
                COLLECTION_BOX_ITEM, COLLECTION_BOX_ITEM.ID.equal(contentId));
        return count > 0;
    }
    
    @Override
    public boolean deleteExhibit(MuseumExhibit exhibit, boolean withFile) {
        if (withFile) {
            File exhibitFile = new File(URI.create(exhibit.getFileUri()));
            exhibitFile.delete();
        }
        
        int count = factory.executeDelete(MUSEUM_EXHIBIT, MUSEUM_EXHIBIT.ID.equal(exhibit.getId()));
        return count > 0;
    }
    
    public SequenceBioDataFormat<?> getFormat(FileType fileType) {
        SequenceBioDataFormat<?> format;
        
        switch (fileType) {
        case GENBANK: format = genBankFormat; break;
        case FASTA: format = fastaFormat; break;
        default: format = null; break;
        }
        
        return format;
    }

    public FileType findFileType(URL url) throws IOException {
        SequenceBioDataFormat<?> format = searcher.findFormat(url);
        final FileType newFileType;
        
        if (format instanceof GenBankFormat) {
            newFileType = FileType.GENBANK;
        }
        else if (format instanceof FastaFormat) {
            newFileType = FileType.FASTA;
        }
        else {
            newFileType = FileType.UNKNOWN;
        }
        return newFileType;
    }
    
    public boolean reloadExhibit(MuseumExhibit exhibit) throws URISyntaxException, IOException {
        URL sourceUrl = exhibit.getSourceFileAsUrl();
        
        Reader source = new InputStreamReader(sourceUrl.openStream());
        
        boolean result;
        try {
            FileType fileType = exhibit.getFileType();
            if (fileType == FileType.GENBANK) {
                SequenceBioDataReader<GenBank> dataReader =
                        new SequenceBioDataReader<GenBank>(source, genBankFormat);
                loadGenBank(exhibit, dataReader);
                result = true;
            }
            else if (fileType == FileType.FASTA) {
                SequenceBioDataReader<Fasta> dataReader =
                        new SequenceBioDataReader<Fasta>(source, fastaFormat);
                loadFasta(exhibit, dataReader);
                result = true;
            }
            else {
                result = false;
            }
        }
        finally {
            source.close();
        }
        
        return result;
    }
        
    void loadGenBank(MuseumExhibit element, SequenceBioDataReader<GenBank> dataReader) {
        element.setFileType(FileType.GENBANK);
        
        if (dataReader.hasNext()) {
            GenBank data = dataReader.next();
            
            element.setName(data.getLocus().getName());
            element.setSequenceLength(data.getLocus().getSequenceLength());
            element.setAccession(data.getAccession().primary());
            element.setNamespace(data.getLocus().getTopology());
            
            // TODO
        }
    }
    
    void loadFasta(MuseumExhibit element, SequenceBioDataReader<Fasta> dataReader) {
        element.setFileType(FileType.GENBANK);
        
        if (dataReader.hasNext()) {
            Fasta data = dataReader.next();
            
            element.setName(data.name());
            element.setSequenceLength(data.sequence().length());
            element.setAccession(data.accession());
            element.setNamespace(data.namespace());
            element.setDefinition(data.description());
            element.setVersion(data.version());
            
            // TODO identifier
        }
    }

    @Override
    public String toString() {
        return "Local Files";
    }
    
    static class ContentsExhibitHandler implements RecordHandler<Record> {
        private final List<MuseumExhibit> list = new LinkedList<MuseumExhibit>();
        
        @Override
        public void next(Record record) {
            MuseumExhibitRecord exhibitRecord = record.into(MUSEUM_EXHIBIT);
            MuseumExhibit element = new MuseumExhibit(exhibitRecord);
            
            Integer rowId = record.getValueAsInteger(COLLECTION_BOX_ITEM.ID);
            element.setRowId(rowId);
            
            list.add(element);
        }

        public List<MuseumExhibit> getElements() {
            return new ArrayList<MuseumExhibit>(list);
        }
    }
    
    static class ExhibitHandler implements RecordHandler<MuseumExhibitRecord> {
        private final List<MuseumExhibit> list;
        public ExhibitHandler() {
            list = new LinkedList<MuseumExhibit>();
        }
        
        public void next(MuseumExhibitRecord record) {
            MuseumExhibit element = new MuseumExhibit(record);
            list.add(element);
        }
        
        public List<MuseumExhibit> getElements() {
            return new ArrayList<MuseumExhibit>(list);
        }
    }
}
