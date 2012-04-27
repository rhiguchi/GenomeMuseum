package jp.scid.genomemuseum.model;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GMExhibit {
    private final static Logger logger = LoggerFactory.getLogger(GMExhibit.class);
    
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
    
    public Long id = null;
    
    public String name = "Untitled";
    
    public int sequenceLength = 0;
    
    public String accession = "";
    
    public String namespace = "";
    
    public int version = 0;

    public String definition = "";
    
    public String sourceText = "";
    
    public String organism = "";
    
    public Date date = null;
    
    public int sequenceUnit;
    
    public String moleculeType = "";
    
    public FileType fileType;
    
    public String fileUri = "";

    // Accessor
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        
        this.name = name;
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength(int sequenceLength) {
        if (sequenceLength < 0)
            throw new IllegalArgumentException("sequenceLength must not be negative number");
        this.sequenceLength = sequenceLength;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        if (accession == null) throw new IllegalArgumentException("accession must not be null");
        this.accession = accession;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        if (namespace == null) throw new IllegalArgumentException("namespace must not be null");
        this.namespace = namespace;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        if (version < 0)
            throw new IllegalArgumentException("version must not be negative number");

        this.version = version;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        if (definition == null) throw new IllegalArgumentException("definition must not be null");
        this.definition = definition;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        if (sourceText == null) throw new IllegalArgumentException("sourceText must not be null");
        this.sourceText = sourceText;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        if (organism == null) throw new IllegalArgumentException("organism must not be null");
        this.organism = organism;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
    public SequenceUnitType getSequenceUnitAsTypeValue() {
        return SequenceUnitType.fromIntValue(sequenceUnit);
    }
    
    public void setSequenceUnitAsTypeValue(SequenceUnitType newType) {
        if (newType == null) throw new IllegalArgumentException("newType must not be null");
        
        sequenceUnit = newType.toIntValue();
    }
    
    public String getMoleculeType() {
        return moleculeType;
    }
    
    public void setMoleculeType(String moleculeType) {
        if (moleculeType == null)
            throw new IllegalArgumentException("moleculeType must not be null");
        this.moleculeType = moleculeType;
    }
    
    public FileType getFileType() {
        return fileType;
    }
    
    public void setFileType(FileType fileType) {
        if (fileType == null) throw new IllegalArgumentException("fileType must not be null");
        
        this.fileType = fileType;
    }
    
    public String getFileUri() {
        return fileUri;
    }
    
    public void setFileUri(String fileUri) {
        if (fileUri == null) throw new IllegalArgumentException("fileUri must not be null");
        this.fileUri = fileUri;
    }
    
    public boolean isInserted() {
        return getId() != null;
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
