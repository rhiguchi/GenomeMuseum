package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.MUSEUM_EXHIBIT;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuseumExhibit {
    private final static Logger logger = LoggerFactory.getLogger(MuseumExhibit.class);
    
    public static enum SequenceUnitType {
        UNKNOWN(0),
        BASE_PAIR(1),
        AMINO_ACID(2),
        ;
        
        // TODO test
        private final static SequenceUnitType[] ALL_VALUES =
                new SequenceUnitType[]{UNKNOWN, BASE_PAIR, AMINO_ACID};
        
        // TODO test
        private final int dbValue;
        
        private SequenceUnitType(int dbValue) {
            this.dbValue = dbValue;
        }
        
        public int toIntValue() {
            return dbValue;
        }
        
        public static SequenceUnitType fromIntValue(int value) {
            if (0 <= value && value < ALL_VALUES.length) {
                return ALL_VALUES[value];
            }
            else {
                return UNKNOWN;
            }
        }
    }
    
    public static enum FileType {
        UNKNOWN(0), GENBANK(1), FASTA(2);
        
        private final int intValue;
        
        private FileType(int intValue) {
            this.intValue = intValue;
        }
        
        public int getIntValue() {
            return intValue;
        }
    }
    
    private final MuseumExhibitRecord record;
    
    private Long rowId = null;
    
    MuseumExhibit(MuseumExhibitRecord record) {
        this.record = record;
    }
    
    public MuseumExhibit() {
        this(new MuseumExhibitRecord());
    }
    
    MuseumExhibitRecord getRecord() {
        return record;
    }
    
    // Accessor
    public Long getId() {
        return record.getId();
    }

    public void setId(Long id) {
        record.setId(id);
    }

    // row num
    public boolean hasRowId() {
        return rowId != null;
    }
    
    public long getRowId() {
        return rowId;
    }
    
    void setRowId(long rowId) {
        this.rowId = rowId;
    }
    
    public String getName() {
        return record.getValueAsString(MUSEUM_EXHIBIT.NAME, "");
    }

    public void setName(String name) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        
        record.setName(name);
    }

    public int getSequenceLength() {
        return record.getValueAsInteger(MUSEUM_EXHIBIT.SEQUENCE_LENGTH, 0);
    }

    public void setSequenceLength(int sequenceLength) {
        if (sequenceLength < 0)
            throw new IllegalArgumentException("sequenceLength must not be negative number");
        
        record.setSequenceLength(sequenceLength);
    }

    public String getAccession() {
        return record.getValueAsString(MUSEUM_EXHIBIT.ACCESSION, "");
    }

    public void setAccession(String accession) {
        if (accession == null) throw new IllegalArgumentException("accession must not be null");
        
        record.setAccession(accession);
    }

    public String getNamespace() {
        return record.getValueAsString(MUSEUM_EXHIBIT.NAMESPACE, "");
    }

    public void setNamespace(String namespace) {
        if (namespace == null) throw new IllegalArgumentException("namespace must not be null");
        
        record.setNamespace(namespace);
    }

    public int getVersion() {
        return record.getValueAsInteger(MUSEUM_EXHIBIT.VERSION, 0);
    }

    public void setVersion(int version) {
        if (version < 0)
            throw new IllegalArgumentException("version must not be negative number");

        record.setVersion(version);
    }

    public String getDefinition() {
        return record.getValueAsString(MUSEUM_EXHIBIT.DEFINITION, "");
    }

    public void setDefinition(String definition) {
        if (definition == null) throw new IllegalArgumentException("definition must not be null");
        
        record.setDefinition(definition);
    }

    public String getSourceText() {
        return record.getValueAsString(MUSEUM_EXHIBIT.SOURCE_TEXT, "");
    }

    public void setSourceText(String sourceText) {
        if (sourceText == null) throw new IllegalArgumentException("sourceText must not be null");
        
        record.setSourceText(sourceText);
    }

    public String getOrganism() {
        return record.getValueAsString(MUSEUM_EXHIBIT.ORGANISM, "");
    }

    public void setOrganism(String organism) {
        if (organism == null) throw new IllegalArgumentException("organism must not be null");
        
        record.setOrganism(organism);
    }

    public Date getDate() {
        return record.getDate();
    }

    public void setDate(Date date) {
        record.setDate(new java.sql.Date(date.getTime()));
    }
    
    public SequenceUnitType getSequenceUnitAsTypeValue() {
        Integer value = record.getValueAsInteger(MUSEUM_EXHIBIT.SEQUENCE_UNIT, 0);
        
        return SequenceUnitType.fromIntValue(value);
    }
    
    public void setSequenceUnitAsTypeValue(SequenceUnitType newType) {
        if (newType == null) throw new IllegalArgumentException("newType must not be null");
        
        record.setSequenceUnit(newType.toIntValue());
    }
    
    public String getMoleculeType() {
        return record.getValueAsString(MUSEUM_EXHIBIT.MOLECULE_TYPE, "");
    }
    
    public void setMoleculeType(String moleculeType) {
        if (moleculeType == null)
            throw new IllegalArgumentException("moleculeType must not be null");
        
        record.setMoleculeType(moleculeType);
    }
    
    public FileType getFileType() {
        Integer value = record.getValueAsInteger(MUSEUM_EXHIBIT.FILE_TYPE, 0);
        FileType fileType = FileType.values()[value];
        
        return fileType;
    }
    
    public void setFileType(FileType fileType) {
        if (fileType == null) throw new IllegalArgumentException("fileType must not be null");
        
        record.setFileType(fileType.getIntValue());
    }
    
    public String getFileUri() {
        return record.getValueAsString(MUSEUM_EXHIBIT.FILE_URI, "");
    }
    
    public void setFileUri(String fileUri) {
        if (fileUri == null) throw new IllegalArgumentException("fileUri must not be null");
        
        record.setFileUri(fileUri);
    }
    
    public URL getSourceFileAsUrl() {
        String uriString = getFileUri();
        if (uriString == null) {
            return null;
        }
        URL url;
        try {
            url = new URI(uriString).toURL();
        }
        catch (MalformedURLException e) {
            logger.info("URI string '{}' cannot parse. " + e.getMessage(), uriString);
            url = null;
        }
        catch (URISyntaxException e) {
            logger.info("URI string '{}' cannot parse. " + e.getMessage(), uriString);
            url = null;
        }
        
        return url;
    }
}
