package jp.scid.genomemuseum.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;

public class TableHeaderClickHandler<E> {
    private final static String STATEMENT_MAP_CLIENT_PROPERTY_KEY = "jp.scid.gui.TableHeaderClickHandler.statementMap";
    private final static String SORTING_COLUMN_CLIENT_PROPERTY_KEY = "jp.scid.gui.TableHeaderClickHandler.sortingColumn";
    private final SortedList<E> sortedList;
    private final AdvancedTableFormat<?> tableFormat;
    
    public TableHeaderClickHandler(SortedList<E> sortedList, AdvancedTableFormat<?> tableFormat) {
        this.sortedList = sortedList;
        this.tableFormat = tableFormat;
    }
    
    public static <E> Binder installTo(JTableHeader header, SortedList<E> sortedList, AdvancedTableFormat<?> tableFormat) {
        TableHeaderClickHandler<E> handler = new TableHeaderClickHandler<E>(sortedList, tableFormat);
        BinderImpl binding = new BinderImpl(handler, header);
        binding.bind();
        return binding;
    }

    public void clickColumn(TableColumn column, JTableHeader header) {
        String newOrderStatement = getOrderStatement(
                column.getIdentifier(), header, isColumnSelected(column, header));
        
        Comparator<? super E> comparator;
        if (newOrderStatement.isEmpty()) {
            comparator = null;
        }
        else {
            comparator = getComparator(column, newOrderStatement);
        }
        
        Object sortingColumn = comparator == null ? null : column.getIdentifier();
        updateSortingColumn(header, sortingColumn);
        
        sortedList.setComparator(comparator);
    }
    
    public String getOrderStatement(Object columnIdentifier, JTableHeader header, boolean columnSelected) {
        String columnStatement = getOrderStatement(columnIdentifier, header);
        
        if (columnStatement == null || columnSelected) {
            columnStatement = getNextOrderStatement(columnIdentifier, columnStatement);
            
            if (!columnStatement.isEmpty()) {
                updateOrderStatement(columnIdentifier, header, columnStatement);
            }
        }
        
        return columnStatement;
    }

    String getNextOrderStatement(Object columnIdentifier, String currentStatement) {
        String newStatement;
        List<String> statements = getColumnOrderStatements(columnIdentifier);
        
        if (!statements.isEmpty()) {
            int nextIndex = statements.indexOf(currentStatement) + 1;
            if (statements.size() <= nextIndex) {
                nextIndex = 0;
            }
            newStatement = statements.get(nextIndex);
        }
        else {
            newStatement = "";
        }
        return newStatement;
    }
    
    @SuppressWarnings("unchecked")
    protected Comparator<? super E> getComparator(TableColumn column, String orderStatement) {
        @SuppressWarnings("rawtypes")
        Comparator comparator = getColumnComparator(column.getModelIndex());
        
        if (comparator == null) {
            return null;
        }
        
        if (isDescendingOrder(orderStatement)) {
            comparator = GlazedLists.reverseComparator(comparator);
        }
        
        return comparator;
    }
    
    private boolean isDescendingOrder(String statement) {
        List<String> list = Arrays.asList(statement.split("\\s+"));
        return list.contains("desc") || list.contains("descending");
    }

    private Comparator<?> getColumnComparator(int modelIndex) {
        return tableFormat.getColumnComparator(modelIndex);
    }
    
    private Object getSortingColumn(JTableHeader header) {
        return header.getClientProperty(SORTING_COLUMN_CLIENT_PROPERTY_KEY);
    }
    
    private void updateSortingColumn(JTableHeader header, Object identifier) {
        header.putClientProperty(SORTING_COLUMN_CLIENT_PROPERTY_KEY, identifier);
    }

    protected boolean isColumnSelected(TableColumn column, JTableHeader header) {
        Object identifier = getSortingColumn(header); 
        if (identifier == null) {
            return false;
        }
        else {
            return identifier.equals(column.getIdentifier());
        }
    }
    
    protected List<String> getColumnOrderStatements(Object columnIdentifier) {
        return Arrays.asList("ascending", "descending");
    }

    /**
     * 
     * @param columnIdentifier
     * @param header
     * @return nullable order string
     */
    @SuppressWarnings("unchecked")
    String getOrderStatement(Object columnIdentifier, JTableHeader header) {
        Map<Object, String> statementMap =
                (Map<Object, String>) header.getClientProperty(STATEMENT_MAP_CLIENT_PROPERTY_KEY);
        if (statementMap == null) {
            return null;
        }
        else {
            return statementMap.get(columnIdentifier);
        }
    }

    @SuppressWarnings("unchecked")
    void updateOrderStatement(Object columnIdentifier, JTableHeader header, String newOrderStatement) {
        Map<Object, String> statementMap =
                (Map<Object, String>) header.getClientProperty(STATEMENT_MAP_CLIENT_PROPERTY_KEY);
        if (statementMap == null) {
            statementMap = new HashMap<Object, String>();
        }
        
        statementMap.put(columnIdentifier, newOrderStatement);
        header.putClientProperty(STATEMENT_MAP_CLIENT_PROPERTY_KEY, statementMap);
    }
    
    protected static interface Binder {
        void release();
        
        TableHeaderClickHandler<?> getHandler();
    }
    
    static class BinderImpl implements Binder, MouseListener {
        private final TableHeaderClickHandler<?> handler;
        private final Component target;
        
        public BinderImpl(TableHeaderClickHandler<?> handler, JTableHeader target) {
            this.handler = handler;
            this.target = target;
        }
        
        public void release() {
            target.removeMouseListener(this);
        }

        public void bind() {
            target.addMouseListener(this);
        }
        
        public TableHeaderClickHandler<?> getHandler() {
            return handler;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            JTableHeader header = (JTableHeader) e.getSource();
            int index = getValidColumnIndex(e);
            
            if (index < 0) {
                return;
            }
            
            TableColumn column = header.getColumnModel().getColumn(index);
            handler.clickColumn(column, header);
            
            updateSortClientProperties(header);
        }

        private void updateSortClientProperties(JTableHeader header) {
            Object identifier = handler.getSortingColumn(header);
            
            Integer selectedColumn;
            String directionValue;

            if (identifier == null) {
                selectedColumn = null;
                directionValue = null;
            }
            else {
                if (header.getColumnModel() != null) {
                    selectedColumn = header.getColumnModel().getColumnIndex(identifier);
                }
                else {
                    selectedColumn = null;
                }
                directionValue = getOrderDirection(header, identifier);
            }

            updateSelectedColumnProperty(header, selectedColumn);
            updateSortDirectionProperty(header, directionValue);
        }

        private String getOrderDirection(JTableHeader header, Object identifier) {
            String direction;
            String statement = handler.getOrderStatement(identifier, header);
            
            if (statement == null) {
                direction = null;
            }
            else if (handler.isDescendingOrder(statement)) {
                direction = "descending";
            }
            else {
                direction = "ascending";
            }
            return direction;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            int index = getValidColumnIndex(e);
            JTableHeader header = (JTableHeader) e.getSource();

            if (index >= 0) {
                Integer value = index >= 0 ? Integer.valueOf(index) : null;
                updatePressedColumnProperty(header, value);
                header.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            JTableHeader header = (JTableHeader) e.getSource();
            updatePressedColumnProperty(header, null);
            if (header.getTable() != null && header.getTable().getParent() != null)
                header.getTable().getParent().repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // do nothing
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // do nothing
        }

        private static void updateSortDirectionProperty(JTableHeader header, String directionValue) {
            header.putClientProperty("JTableHeader.sortDirection", directionValue);
            header.putClientProperty("EPJTableHeader.sortDirection", directionValue);
        }

        private static void updateSelectedColumnProperty(JTableHeader header, Integer selectedColumn) {
            header.putClientProperty("EPJTableHeader.selectedColumn", selectedColumn);
            header.putClientProperty("JTableHeader.selectedColumn", selectedColumn);
        }

        private static void updatePressedColumnProperty(JTableHeader header, Integer value) {
            header.putClientProperty("EPJTableHeader.pressedColumn", value);
            header.putClientProperty("JTableHeader.pressedColumn", value);
        }
        
        static int getValidColumnIndex(MouseEvent e) {
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
}