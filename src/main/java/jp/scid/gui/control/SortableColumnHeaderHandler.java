package jp.scid.gui.control;

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

import ca.odell.glazedlists.gui.AdvancedTableFormat;

@Deprecated
public class SortableColumnHeaderHandler extends TableHeaderClickHandler {
    protected Model model = new DefaultModel();
    
    public SortableColumnHeaderHandler() {
    }
    
    public SortableColumnHeaderHandler(Model model) {
        this();
        this.model = model;
    }

    @Override
    public void columnClicked(TableColumn column, MouseEvent e) {
        JTableHeader header = (JTableHeader) e.getSource();
        List<String> statements = getModel().getAvailableOrderStatements(column);
        String directionValue = null;
        
        if (statements != null && !statements.isEmpty()) {
            if (getModel().isSelected(column)) {
                int nextIndex = statements.indexOf(getModel().getOrderStatement(column)) + 1;

                if (statements.size() <= nextIndex)
                    nextIndex = 0;

                getModel().putOrderStatement(column, statements.get(nextIndex));
            }
            else {
                getModel().setSelected(column);
            }
            
            directionValue = isDescendingStatement(
                    getModel().getOrderStatement(column)) ? "descending" : "ascending";
        }
        
        header.putClientProperty("JTableHeader.sortDirection", directionValue);
    }
    
    @Override
    public boolean isColumnSelected(TableColumn column) {
        return getModel().isSelected(column);
    }
    
    public Model getModel() {
        return model;
    }
    
    public void setModel(Model model) {
        this.model = model;
    }
    
    boolean isDescendingStatement(String statement) {
        List<String> strings = Arrays.asList(statement.split("\\s+"));
        
        if (strings.isEmpty())
            return false;
        
        return strings.subList(1, strings.size()).contains("desc");
    }
    
    public static interface Model {
        @Deprecated
        List<String> getAvailableOrderStatements(TableColumn column);
        
        String getOrderStatement(TableColumn column);
        
        void putOrderStatement(TableColumn column, String statement);
        
        boolean isSelected(TableColumn column);
        
        void setSelected(TableColumn column);
    }
    
    public static class DefaultModel implements Model {
        protected TableColumn selectedColumn = null;
        protected final Map<TableColumn, String> statementMap = new HashMap<TableColumn, String>();
        
        public DefaultModel() {
        }
        
        @Override
        public List<String> getAvailableOrderStatements(TableColumn column) {
            String name = getOrderPropertyName(column);
            if (name == null)
                return Collections.emptyList();
            return Arrays.asList(name, name + " desc");
        }
        
        protected String getOrderPropertyName(TableColumn column) {
            if (column.getIdentifier() == null)
                return null;
            
            return column.getIdentifier().toString();
        }

        @Override
        public boolean isSelected(TableColumn column) {
            if (selectedColumn == null)
                return false;
            return selectedColumn.equals(column);
        }
        
        @Override
        public void setSelected(TableColumn column) {
            selectedColumn = column;
        }
        
        public String getOrderStatement() {
            if (selectedColumn == null)
                return "";
            
            String statement = statementMap.get(selectedColumn);
            if (statement == null)
                return "";
            
            return statement;
        }

        @Override
        public String getOrderStatement(TableColumn column) {
            String statement = statementMap.get(column);
            if (statement == null) {
                List<String> statements = getAvailableOrderStatements(column);
                if (statements.isEmpty())
                    return "";
                return statements.get(0);
            }
            
            return statement;
        }

        @Override
        public void putOrderStatement(TableColumn column, String statement) {
            statementMap.put(column, statement);
        }
    }
    
    public static class TableFormatSortingModel<E> extends DefaultModel implements ValueModel<Comparator<? super E>> {
        private final AdvancedTableFormat<E> tableFormat;
        
        private ValueModel<Comparator<? super E>> modelDelegate = ValueModels.newNullableValueModel();
        
        public TableFormatSortingModel(AdvancedTableFormat<E> fromat) {
            this.tableFormat = fromat;
        }

        @Override
        public void setSelected(TableColumn column) {
            super.setSelected(column);
            
            updateCurrentStatement();
        }
        
        @Override
        public void putOrderStatement(TableColumn column, String statement) {
            super.putOrderStatement(column, statement);
            
            updateCurrentStatement();
        }
        
        @SuppressWarnings("unchecked")
        protected void updateCurrentStatement() {
            String statement = getOrderStatement();
            
            setValue((Comparator<E>) getComparator(statement));
        }
        
        @Override
        protected String getOrderPropertyName(TableColumn column) {
            int modelIndex = column.getModelIndex();
            Comparator<?> comparator = tableFormat.getColumnComparator(modelIndex);
            if (comparator == null)
                return null;
            
            return tableFormat.getColumnName(modelIndex);
        }
        
        public Comparator<?> getComparator(final String statement) {
            if (statement == null)
                return null;
            
            String trimedStatement = statement.trim();
            if (trimedStatement.isEmpty())
                return null;
            
            final boolean isDescending;
            final String propertyName;
            
            if (trimedStatement.endsWith(" desc")) {
                isDescending = true;
                propertyName = statement.substring(0, statement.length() - 5);
            }
            else {
                isDescending = false;
                propertyName = trimedStatement;
            }
            
            Comparator<?> comparator = findComparator(propertyName);
            
            if (comparator == null)
                return null;
            
            if (isDescending)
                return Collections.reverseOrder(comparator);
            else
                return comparator;
        }
        
        Comparator<?> findComparator(String statement) {
            for (int index = 0; index < tableFormat.getColumnCount(); index++) {
                if (statement.equals(tableFormat.getColumnName(index)))
                    return tableFormat.getColumnComparator(index);
            }
            
            return null;
        }

        @Override
        public Comparator<? super E> getValue() {
            return modelDelegate.getValue();
        }

        @Override
        public void setValue(Comparator<? super E> newValue) {
            modelDelegate.setValue(newValue);
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            modelDelegate.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            modelDelegate.removePropertyChangeListener(listener);
        }
    }
}
