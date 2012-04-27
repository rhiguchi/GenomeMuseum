package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jp.scid.bio.Fasta;
import jp.scid.bio.FastaFormat;
import jp.scid.bio.GenBank;
import jp.scid.bio.GenBankFormat;
import jp.scid.bio.SequenceBioDataFormat;
import jp.scid.bio.SequenceBioDataFormatSearcher;
import jp.scid.bio.SequenceBioDataReader;
import jp.scid.genomemuseum.model.GMExhibit.FileType;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.jooq.impl.Factory;

public class MuseumExhibitLibrary extends AbstractExhibitListModel {
    private SequenceBioDataFormatSearcher searcher = new SequenceBioDataFormatSearcher();
    
    private final Factory factory;
    
    public MuseumExhibitLibrary(Factory factory) {
        this.factory = factory;
    }
    
    protected GMExhibit createExhibit() {
        return new GMExhibit();
    }
    
    public GMExhibit newElement() {
        GMExhibit newExibit = createExhibit();
        factory.newRecord(MUSEUM_EXHIBIT, newExibit);
        
        return newExibit;
    }

    public boolean save(GMExhibit element) {
        MuseumExhibitRecord newRecord = factory.newRecord(MUSEUM_EXHIBIT, element);
        int count = newRecord.store();
        return count > 0;
    }
    
    public boolean delete(GMExhibit element) {
        int count = factory.executeDelete(MUSEUM_EXHIBIT, MUSEUM_EXHIBIT.ID.equal(element.id));
        return count > 0;
    }
    
    public boolean reloadExhibit(GMExhibit exhibit) throws URISyntaxException, IOException {
        URL sourceUrl = new URI(exhibit.getFileUri()).toURL();
        
        SequenceBioDataFormat<?> format = searcher.findFormat(sourceUrl);
        
        Reader reader = new InputStreamReader(sourceUrl.openStream());
        
        try {
            if (format instanceof GenBankFormat) {
                loadGenBank(exhibit, reader, (GenBankFormat) format);
            }
            else if (format instanceof FastaFormat) {
                loadFasta(exhibit, reader, (FastaFormat) format);
            }
            else {
                return false;
            }
        }
        finally {
            reader.close();
        }
        
        return true;
    }
    
    public void deleteExhibitAndFile() {
        
    }

    
    void loadGenBank(GMExhibit element, Reader source, SequenceBioDataFormat<GenBank> format) {
        SequenceBioDataReader<GenBank> dataReader = new SequenceBioDataReader<GenBank>(source, format);
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
    
    void loadFasta(GMExhibit element, Reader source, SequenceBioDataFormat<Fasta> format) {
        SequenceBioDataReader<Fasta> dataReader = new SequenceBioDataReader<Fasta>(source, format);
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
}
