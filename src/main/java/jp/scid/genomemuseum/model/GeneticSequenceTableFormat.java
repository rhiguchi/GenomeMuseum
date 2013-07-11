package jp.scid.genomemuseum.model;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.bio.store.sequence.SequenceUnit;
import ca.odell.glazedlists.gui.AdvancedTableFormat;

public class GeneticSequenceTableFormat implements AdvancedTableFormat<GeneticSequence>{
    static enum Column implements Comparator<GeneticSequence> {
        ID("id") {
            @Override
            public Long getValue(GeneticSequence e) {
                return e.id();
            }
        },
        NAME("name") {
            @Override
            public String getValue(GeneticSequence e) {
                return e.name();
            }
        },
        SEQUENCE_LENGTH("sequenceLength", Integer.class) {
            @Override
            public Integer getValue(GeneticSequence e) {
                return e.length();
            }
        },
        ACCESSION("accession") {
            @Override
            public String getValue(GeneticSequence e) {
                return e.accession();
            }
        },
        NAMESPACE("namespace") {
            @Override
            public String getValue(GeneticSequence e) {
                return e.namespace();
            }
        },
        VERSION("version", Integer.class) {
            @Override
            public Integer getValue(GeneticSequence e) {
                return e.version();
            }
        },
        DEFINITION("definition") {
            @Override
            public String getValue(GeneticSequence e) {
                return e.definition();
            }
        },
        SOURCE_TEXT("sourceText") {
            @Override
            public String getValue(GeneticSequence e) {
                return e.source();
            }
        },
        ORGANISM("organism") {
            @Override
            public String getValue(GeneticSequence e) {
                return e.organism();
            }
        },
        DATE("date", Date.class) {
            @Override
            public Date getValue(GeneticSequence e) {
                return e.date();
            }
        },
        SEQUENCE_UNIT("sequenceUnit", SequenceUnit.class) {
            @Override
            public SequenceUnit getValue(GeneticSequence e) {
                return e.sequenceUnit();
            }
        },
        MOLECULE_TYPE("moleculeType") {
            @Override
            public String getValue(GeneticSequence e) {
                return e.moleculeType();
            }
        },
        FILE_URI("fileUri") {
            @Override
            public File getValue(GeneticSequence e) {
                return e.getFile();
            }
        },
        ;
        
        private final String name;
        private final Class<?> columnClass;
        
        private Column(String name, Class<?> columnClass) {
            this.name = name;
            this.columnClass = columnClass;
        }
        
        private Column(String name) {
            this(name, String.class);
        }
        
        public String getName() {
            return name;
        }
        
        public abstract Object getValue(GeneticSequence e); 
        
        @SuppressWarnings("unchecked")
        @Override
        public int compare(GeneticSequence o1, GeneticSequence o2) {
            Comparable<Object> v1 = (Comparable<Object>) getValue(o1);
            Comparable<Object> v2 = (Comparable<Object>) getValue(o2);
            
            if (v1 == null) {
                return v2 == null ? 0 : 1;
            }
            else if (v2 == null) {
                return -1;
            }
            
            return v1.compareTo(v2);
        }
        
        public Comparator<GeneticSequence> getComparator() {
            return this;
        }
        
        public Class<?> getColumnClass() {
            return columnClass;
        }
    }

    private final List<Column> columns;
    
    public GeneticSequenceTableFormat(Column... columns) {
        this.columns = Arrays.asList(columns);
    }
    
    public GeneticSequenceTableFormat() {
        this(Column.values());
    }
    
    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return getColumn(column).getName();
    }

    Column getColumn(int column) {
        return columns.get(column);
    }

    @Override
    public Object getColumnValue(GeneticSequence baseObject, int column) {
        return getColumn(column).getValue(baseObject);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return getColumn(column).getColumnClass();
    }

    @Override
    public Comparator<GeneticSequence> getColumnComparator(int column) {
        return getColumn(column).getComparator();
    }
}
