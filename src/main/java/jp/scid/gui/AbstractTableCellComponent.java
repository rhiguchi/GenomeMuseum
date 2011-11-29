package jp.scid.gui;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public abstract class AbstractTableCellComponent implements TableCellRenderer {
    protected final TableCellRenderer baseRenderer;

    public AbstractTableCellComponent() {
        this(new DefaultTableCellRenderer());
    }
    
    public AbstractTableCellComponent(TableCellRenderer baseRenderer) {
        this.baseRenderer = baseRenderer;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        final Component comp = baseRenderer.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
        final JComponent cell = getRendererView();
        
        makeCell(cell, comp);
        setValueToRendererView(value);
        
        return cell;
    }
    
    abstract public JComponent getRendererView();
    
    abstract protected void setValueToRendererView(Object value);

    protected void makeCell(JComponent cell, Component renderedCell) {
        cell.setOpaque(renderedCell.isOpaque());
        cell.setFont(renderedCell.getFont());
        cell.setBackground(renderedCell.getBackground());
        cell.setForeground(renderedCell.getForeground());
        
        if (renderedCell instanceof JComponent) {
            JComponent label = (JComponent) renderedCell;
            cell.setBorder(label.getBorder());
        }
    }
}
