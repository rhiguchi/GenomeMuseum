package jp.scid.genomemuseum.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ca.odell.glazedlists.gui.AdvancedTableFormat;

public class MuseumExhibitTableFormat implements AdvancedTableFormat<GMExhibit>{
    static enum Column implements Comparator<GMExhibit> {
        ID("id") {
            @Override
            public Long getValue(GMExhibit e) {
                return e.getId();
            }
        },
        NAME("name") {
            @Override
            public String getValue(GMExhibit e) {
                return e.getName();
            }
        },
        SEQUENCE_LENGTH("sequenceLength", Integer.class) {
            @Override
            public Integer getValue(GMExhibit e) {
                return e.getSequenceLength();
            }
        },
        ACCESSION("accession") {
            @Override
            public String getValue(GMExhibit e) {
                return e.getAccession();
            }
        },
        NAMESPACE("namespace") {
            @Override
            public String getValue(GMExhibit e) {
                return e.getNamespace();
            }
        },
        VERSION("version", Integer.class) {
            @Override
            public Integer getValue(GMExhibit e) {
                return e.getVersion();
            }
        },
        DEFINITION("definition") {
            @Override
            public String getValue(GMExhibit e) {
                return e.getDefinition();
            }
        },
        SOURCE_TEXT("sourceText") {
            @Override
            public String getValue(GMExhibit e) {
                return e.getSourceText();
            }
        },
        ORGANISM("organism") {
            @Override
            public String getValue(GMExhibit e) {
                return e.getOrganism();
            }
        },
        DATE("date", Date.class) {
            @Override
            public Date getValue(GMExhibit e) {
                return e.getDate();
            }
        },
        SEQUENCE_UNIT("sequenceUnit", GMExhibit.SequenceUnitType.class) {
            @Override
            public GMExhibit.SequenceUnitType getValue(GMExhibit e) {
                return e.getSequenceUnitAsTypeValue();
            }
        },
        MOLECULE_TYPE("moleculeType") {
            @Override
            public String getValue(GMExhibit e) {
                return e.getMoleculeType();
            }
        },
        FILE_URI("fileUri") {
            @Override
            public String getValue(GMExhibit e) {
                return e.getFileUri();
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
        
        public abstract Object getValue(GMExhibit e); 
        
        @SuppressWarnings("unchecked")
        @Override
        public int compare(GMExhibit o1, GMExhibit o2) {
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
        
        public Comparator<GMExhibit> getComparator() {
            return this;
        }
        
        public Class<?> getColumnClass() {
            return columnClass;
        }
    }

    private final List<Column> columns;
    
    public MuseumExhibitTableFormat(Column... columns) {
        this.columns = Arrays.asList(columns);
    }
    
    public MuseumExhibitTableFormat() {
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
    public Object getColumnValue(GMExhibit baseObject, int column) {
        return getColumn(column).getValue(baseObject);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return getColumn(column).getColumnClass();
    }

    @Override
    public Comparator<GMExhibit> getColumnComparator(int column) {
        return getColumn(column).getComparator();
    }
}
