package jp.scid.gui.control;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

abstract class TableHeaderClickHandler {
    private MouseListener tableHeaderMouseHandler = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            processHeaderMouseClicked(e);
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            int index = getAvailableColumnIndex(e);
            JTableHeader header = (JTableHeader) e.getSource();
            
            if (index >= 0 &&
                    isClickableColumn(header.getColumnModel().getColumn(index), e)) {
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
    };

    void putPressedColumnProperty(JTableHeader header, Integer value) {
        header.putClientProperty("EPJTableHeader.pressedColumn", value);
        header.putClientProperty("JTableHeader.pressedColumn", value);
        header.repaint();
    }
    
    protected void processHeaderMouseClicked(MouseEvent e) {
        JTableHeader header = (JTableHeader) e.getSource();
        int index = getAvailableColumnIndex(e);
        List<Integer> selectedColumns = null;
        
        if (index >= 0) { 
            TableColumn column = header.getColumnModel().getColumn(index);
            if (isClickableColumn(column, e))
                columnClicked(column, e);
            
            if (isColumnSelected(column))
                selectedColumns = Collections.singletonList(index);
        }
        
        header.putClientProperty("EPJTableHeader.selectedColumn",
                selectedColumns == null ? null : selectedColumns.get(0));
        header.putClientProperty("JTableHeader.selectedColumns", selectedColumns);
        header.repaint();
    }

    protected int getAvailableColumnIndex(MouseEvent e) {
        JTableHeader header = (JTableHeader) e.getSource();
        
        if (header.getCursor() != Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
            TableColumnModel columnModel = header.getColumnModel();
            int index = columnModel.getColumnIndexAtX(e.getX());
            
            if (0 <= index && index < columnModel.getColumnCount());
            return index;
        }
        
        return -1;
    }
    
    abstract public void columnClicked(TableColumn column, MouseEvent e);
    
    abstract public boolean isColumnSelected(TableColumn column);
    
    protected boolean isClickableColumn(TableColumn column, MouseEvent e) {
        return true;
    }
    
    public void bindTableHeader(JTableHeader header) {
        header.addMouseListener(tableHeaderMouseHandler);
    }
    
    public void unbindTableHeader(JTableHeader header) {
        header.removeMouseListener(tableHeaderMouseHandler);
    }
}
