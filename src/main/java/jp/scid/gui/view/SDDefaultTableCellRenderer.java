package jp.scid.gui.view;

import static javax.swing.BorderFactory.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class SDDefaultTableCellRenderer implements TableCellRenderer {
    final DefaultTableCellRenderer delegate = new DefaultTableCellRenderer();
    
    final static Border rowBorder = createEmptyBorder(0, 5, 0, 5);
    final static Border selectedActiveRowBorder = createCompoundBorder(
            createMatteBorder(0, 0, 1, 0, new Color(125, 170, 234)), 
                    createEmptyBorder(1, 5, 0, 5));
    final static Border selectedInactiveRowBorder = createCompoundBorder(
            createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)), 
                    createEmptyBorder(1, 5, 0, 5));
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        JLabel cell = (JLabel) delegate.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        
        cell.setOpaque(isSelected);
        
        final Border border = !table.isRowSelected(row) ? rowBorder
                : isParentWindowFocused(table) ? selectedActiveRowBorder
                        : selectedInactiveRowBorder;
        
        cell.setBorder(border);
        return cell;
    }
    
    public static boolean isParentWindowFocused(Component component) {
        Window window = SwingUtilities.getWindowAncestor(component);
        return window != null && window.isFocused();
    }
}
