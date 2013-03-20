package jp.scid.gui.control;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;
import ca.odell.glazedlists.gui.AdvancedTableFormat;

public class ColumnOrderStatementHandler<E> extends ClickColumnSelectHandler {
    protected final Map<TableColumn, String> statementMap =
            new HashMap<TableColumn, String>();
    
    private final ValueModel<TableColumn> selectedColumn =
            ValueModels.newNullableValueModel();
    
    private final AdvancedTableFormat<? super E> tableFormat;
    
    private final ValueModel<Comparator<? super E>> comparator;
    
    public ColumnOrderStatementHandler(ValueModel<Comparator<? super E>> comparator,
            AdvancedTableFormat<? super E> tableFormat) {
        this.tableFormat = tableFormat;
        this.comparator = comparator;
    }

    @Override
    public void processHeaderMouseClicked(MouseEvent e) {
        JTableHeader header = (JTableHeader) e.getSource();
        int index = getValidColumnIndex(e);
        
        if (index >= 0) { 
            TableColumn column = header.getColumnModel().getColumn(index);

            List<String> statements = getOrderStatementList(column);
            
            if (statements != null && !statements.isEmpty()) {
                if (isColumnSelected(column))
                    updateStatementNext(column, statements);
                
                super.processHeaderMouseClicked(e);
            }

            String directionValue = isDescendingStatement(
                    getOrderStatement(column)) ? "descending" : "ascending";
            header.putClientProperty("JTableHeader.sortDirection", directionValue);
            header.putClientProperty("EPJTableHeader.sortDirection", directionValue);
        }
    }

    @Override
    public boolean isColumnSelected(TableColumn column) {
        if (column != null)
            return column.equals(selectedColumn.getValue());
        return column == selectedColumn.getValue();
    }
    
    @Override
    public void setColumnSelected(TableColumn column) {
        selectedColumn.setValue(column);
        
        updateCurrentStatement();
    }
    
    
    public String getOrderStatement(TableColumn column) {
        if (statementMap.containsKey(column)) {
            String statement = statementMap.get(column);
            if (statement == null) {
                statement = "";
            }
            return statement;
        }
        else {
            List<String> list = getOrderStatementList(column);
            if (list == null || list.isEmpty())
                return "";
            return list.get(0);
        }
    }
    
    public void putOrderStatement(TableColumn column, String newStatement) {
        statementMap.put(column, newStatement);
        
        updateCurrentStatement();
    }

    protected List<String> getOrderStatementList(TableColumn column) {
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
    
    protected boolean isDescendingStatement(String statement) {
        List<String> strings = Arrays.asList(statement.split("\\s+"));
        
        if (strings.isEmpty())
            return false;
        
        return strings.subList(1, strings.size()).contains("desc");
    }

    void updateStatementNext(TableColumn column, List<String> statements) {
        int nextIndex = statements.indexOf(getOrderStatement(column)) + 1;

        if (statements.size() <= nextIndex)
            nextIndex = 0;

        putOrderStatement(column, statements.get(nextIndex));
    }
    
    public static interface ColumnOrderStatementModel {
        String getOrderStatement(TableColumn column);
        
        void putOrderStatement(TableColumn column, String statement);
        
        boolean isSelected(TableColumn column);
        
        void setSelected(TableColumn column);
    }

    public String getOrderStatement() {
        if (selectedColumn.getValue() == null)
            return "";
        
        return getOrderStatement(selectedColumn.getValue());
    }
    
    protected void updateCurrentStatement() {
        String statement = getOrderStatement();
        
        comparator.setValue(getComparator(statement));
    }
    
    protected Comparator<? super E> getComparator(final String statement) {
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
        
        Comparator<? super E> comparator = findComparator(propertyName);
        
        if (comparator == null)
            return null;
        
        if (isDescending)
            return Collections.reverseOrder(comparator);
        else
            return comparator;
    }
    
    @SuppressWarnings("unchecked")
    Comparator<? super E> findComparator(String statement) {
        for (int index = 0; index < tableFormat.getColumnCount(); index++) {
            if (statement.equals(tableFormat.getColumnName(index)))
                return tableFormat.getColumnComparator(index);
        }
        
        return null;
    }
}

abstract class ClickColumnSelectHandler implements MouseListener {
    public ClickColumnSelectHandler() {
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        processHeaderMouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int index = getValidColumnIndex(e);
        JTableHeader header = (JTableHeader) e.getSource();

        if (index >= 0 &&
                isColumnClickable(header.getColumnModel().getColumn(index), e)) {
            Integer value = index >= 0 ? Integer.valueOf(index) : null;

            putPressedColumnProperty(header, value);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        JTableHeader header = (JTableHeader) e.getSource();
        putPressedColumnProperty(header, null);
        if (header.getTable() != null && header.getTable().getParent() != null)
            header.getTable().getParent().repaint();
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {}
    
    @Override
    public void mouseExited(MouseEvent e) {}

    void putPressedColumnProperty(JTableHeader header, Integer value) {
        header.putClientProperty("EPJTableHeader.pressedColumn", value);
        header.putClientProperty("JTableHeader.pressedColumn", value);
        header.repaint();
    }
    
    void processHeaderMouseClicked(MouseEvent e) {
        JTableHeader header = (JTableHeader) e.getSource();
        int index = getValidColumnIndex(e);
        List<Integer> selectedColumns = null;
        
        if (index >= 0) { 
            TableColumn column = header.getColumnModel().getColumn(index);
            setColumnSelected(column);
            
            if (isColumnSelected(column))
                selectedColumns = Collections.singletonList(index);
        }
        
        header.putClientProperty("EPJTableHeader.selectedColumn",
                selectedColumns == null ? null : selectedColumns.get(0));
        header.putClientProperty("JTableHeader.selectedColumns", selectedColumns);
        header.repaint();
    }
    
    abstract public boolean isColumnSelected(TableColumn column);
    
    abstract public void setColumnSelected(TableColumn column);
    
    protected boolean isColumnClickable(TableColumn column, MouseEvent e) {
        return true;
    }
    
    public void bindTableHeader(JTableHeader header) {
        header.addMouseListener(this);
    }
    
    public void unbindTableHeader(JTableHeader header) {
        header.removeMouseListener(this);
    }
    
    int getValidColumnIndex(MouseEvent e) {
        JTableHeader header = (JTableHeader) e.getSource();
        
        if (header.getCursor() != Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
            TableColumnModel columnModel = header.getColumnModel();
            int index = columnModel.getColumnIndexAtX(e.getX());
            
            if (0 <= index && index < columnModel.getColumnCount());
            return index;
        }
        
        return -1;
    }
}
