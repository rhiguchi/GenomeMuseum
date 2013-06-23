package jp.scid.genomemuseum.model;

import java.util.Comparator;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;

public class WebServiceResultTableFormat implements AdvancedTableFormat<NcbiEntry>, WritableTableFormat<NcbiEntry> {
    enum Column implements Comparator<NcbiEntry> {
        DOWNLOAD_ACTION(TaskProgressModel.class) {
            @Override
            NcbiEntry getColumnValue(NcbiEntry e) {
                return e;
            }
            
            @Override
            public boolean isEditable(NcbiEntry e) {
                return e.sourceUri() != null;
            }
        },
        IDENTIFIER(String.class) {
            @Override
            String getColumnValue(NcbiEntry e) {
                return e.identifier();
            }
        },
        ACCESSION(String.class) {
            @Override
            String getColumnValue(NcbiEntry e) {
                return e.accession();
            }
        },
        SEQUENCE_LENGTH(Integer.class) {
            @Override
            Integer getColumnValue(NcbiEntry e) {
                return e.sequenceLength();
            }
        },
        DEFINITION(String.class) {
            @Override
            String getColumnValue(NcbiEntry e) {
                return e.definition();
            }
        },
        TAXONOMY(String.class) {
            @Override
            String getColumnValue(NcbiEntry e) {
                return e.taxonomy();
            }
        },
        ;
        private final Class<?> dataClass;
        
        private Column(Class<?> dataClass) {
            this.dataClass = dataClass;
        }
        
        public Comparator<NcbiEntry> comparator() {
            return Comparable.class.isAssignableFrom(dataClass) ? this : null;
        }
        
        abstract Object getColumnValue(NcbiEntry e);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public int compare(NcbiEntry o1, NcbiEntry o2) {
            Comparable val1 = (Comparable<?>) getColumnValue(o1);
            Comparable val2 = (Comparable<?>) getColumnValue(o2);
            return val1.compareTo(val2);
        }
        
        public boolean isEditable(@SuppressWarnings("unused") NcbiEntry e) {
            return false;
        }
    }
    
    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return Column.values()[column].name();
    }

    @Override
    public Object getColumnValue(NcbiEntry baseObject, int column) {
        return Column.values()[column].getColumnValue(baseObject);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return Column.values()[column].dataClass;
    }

    @Override
    public Comparator<NcbiEntry> getColumnComparator(int columnNumber) {
        return Column.values()[columnNumber].comparator();
    }

    @Override
    public boolean isEditable(NcbiEntry baseObject, int column) {
        return Column.values()[column].isEditable(baseObject);
    }

    @Override
    public NcbiEntry setColumnValue(NcbiEntry baseObject, Object editedValue, int column) {
        return baseObject;
    }
}