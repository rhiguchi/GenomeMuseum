package jp.scid.genomemuseum.model;

import static java.lang.String.*;
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
import jp.scid.genomemuseum.model.MuseumExhibit.FileType;
import jp.scid.genomemuseum.model.sql.tables.records.CollectionBoxItemRecord;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.jooq.Record;
import org.jooq.RecordHandler;
import org.jooq.Result;
import org.jooq.impl.Factory;

public class MuseumExhibitLibrary {
    final ExhibitFileManager fileManager; 
    
    private final GenBankFormat genBankFormat;
    private final FastaFormat fastaFormat;
    
    private final SequenceBioDataFormatSearcher searcher;
    
    final Factory factory;
    
    @SuppressWarnings("unchecked")
    public MuseumExhibitLibrary(Factory factory) {
        genBankFormat = new GenBankFormat();
        fastaFormat = new FastaFormat();
        searcher = new SequenceBioDataFormatSearcher(Arrays.asList(genBankFormat, fastaFormat));
        fileManager = new ExhibitFileManager(); 
        
        this.factory = factory;
    }

    public void setFileBase(File filesDir) {
        fileManager.setDirectory(filesDir);
    }
    
    public List<MuseumExhibitRecord> getAllExhibits() {
        Result<MuseumExhibitRecord> list = factory.selectFrom(MUSEUM_EXHIBIT).fetch();
        
        return list;
    }
    
    public void deleteExhibit(long id) {
        MuseumExhibitRecord exhibit = factory.selectFrom(MUSEUM_EXHIBIT)
                .where(MUSEUM_EXHIBIT.ID.equal(id))
                .fetchOne();
        if (exhibit == null) {
            return;
        }
        
        URI fileUri = URI.create(exhibit.getFileUri());
        if (fileUri != null) {
            File exhibitFile = new File(fileUri);
            exhibitFile.delete();
        }
        
        exhibit.delete();
    }

    public MuseumExhibitRecord createExhibit() {
        MuseumExhibitRecord record = factory.newRecord(MUSEUM_EXHIBIT);
        record.setName("Untitled");
        return record;
    }

    public boolean save(MuseumExhibitRecord exhibit) {
        return exhibit.store() > 0;
    }
    
    public List<MuseumExhibit> getBoxContents(long boxId) {
        List<MuseumExhibit> elements = factory.select()
                .from(COLLECTION_BOX_ITEM)
                .join(MUSEUM_EXHIBIT)
                .on(COLLECTION_BOX_ITEM.EXHIBIT_ID.equal(MUSEUM_EXHIBIT.ID))
                .where(COLLECTION_BOX_ITEM.BOX_ID.equal(boxId))
                .orderBy(COLLECTION_BOX_ITEM.ID)
                .fetchInto(new ContentsExhibitHandler())
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
    
    void loadGenBank(MuseumExhibitRecord element, SequenceBioDataReader<GenBank> dataReader) {
        element.setFileType(FileType.GENBANK.ordinal());
        
        if (dataReader.hasNext()) {
            GenBank data = dataReader.next();
            
            element.setName(data.getLocus().getName());
            element.setSequenceLength(data.getLocus().getSequenceLength());
            element.setAccession(data.getAccession().primary());
            element.setNamespace(data.getLocus().getTopology());
            
            // TODO
        }
    }
    
    void loadFasta(MuseumExhibitRecord element, SequenceBioDataReader<Fasta> dataReader) {
        element.setFileType(FileType.FASTA.ordinal());
        
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

    public void reloadExhibit(MuseumExhibitRecord exhibit, URL source, FileType fileType) throws IOException {
        Reader reader = new InputStreamReader(source.openStream());
        
        try {
            if (fileType == FileType.GENBANK) {
                SequenceBioDataReader<GenBank> dataReader =
                        new SequenceBioDataReader<GenBank>(reader, genBankFormat);
                loadGenBank(exhibit, dataReader);
            }
            else if (fileType == FileType.FASTA) {
                SequenceBioDataReader<Fasta> dataReader =
                        new SequenceBioDataReader<Fasta>(reader, fastaFormat);
                loadFasta(exhibit, dataReader);
            }
            else {
                throw new IllegalArgumentException(format("file type %s is not supported", fileType));
            }
        }
        finally {
            reader.close();
        }
        
        exhibit.setFileType(fileType.ordinal());
        
        try {
            exhibit.setFileUri(source.toURI().toString());
        }
        catch (URISyntaxException e) {
            throw new IOException(e);
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
