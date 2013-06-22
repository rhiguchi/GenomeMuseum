package jp.scid.genomemuseum.view;

import static javax.swing.BorderFactory.*;
import static javax.swing.SpringLayout.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.util.EventObject;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jp.scid.genomemuseum.model.TaskProgressModel;

public class WebSearchResultListView extends RecordListView {

    private final static Icon loadingIcon = new ImageIcon(MainView.class.getResource("loading.gif"));
    
    public final DownloadTaskTableCell taskProgressCell = new DownloadTaskTableCell();

    private final JLabel loadingIconLabel = new JLabel("progress text...", loadingIcon, SwingConstants.LEFT);
    
    private final JButton cancelButton = new JButton("Cancel");
    
    public WebSearchResultListView() {
        SDDefaultTableCellRenderer wrapper = new SDDefaultTableCellRenderer(taskProgressCell, taskProgressCell);
        
        table.setDefaultRenderer(TaskProgressModel.class, wrapper);
        table.setDefaultEditor(TaskProgressModel.class, wrapper);
        
        loadingIconLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        
        SpringLayout layout = new SpringLayout();
        addToToolContainer(cancelButton, loadingIconLabel, searchField);
        toolContainer.setLayout(layout);
        layout.putConstraint(EAST, searchField, 0, EAST, toolContainer);
        layout.putConstraint(NORTH, searchField, 0, NORTH, toolContainer);
        
        layout.putConstraint(BASELINE, cancelButton, 0, BASELINE, searchField);
        layout.putConstraint(EAST, cancelButton, -4, EAST, searchField);
        
        layout.putConstraint(WEST, searchField, 0, EAST, loadingIconLabel);
        layout.putConstraint(VERTICAL_CENTER, loadingIconLabel, 0, VERTICAL_CENTER, searchField);
        layout.putConstraint(WEST, loadingIconLabel, 0, WEST, toolContainer);
    }
    
    @Override
    JComponent getComponent() {
        return getTableContainer();
    }
    
    public JLabel loadingIconLabel() {
        return loadingIconLabel;
    }
    
    public JButton cancelButton() {
        return cancelButton;
    }
    
    public void setDownloadButtonAction(Action action) {
        taskProgressCell.setDownloadButtonAction(action);
    }
}


class SDDefaultTableCellRenderer implements TableCellRenderer, TableCellEditor {
    final static Border rowBorder = createEmptyBorder(0, 5, 0, 5);
    final static Border selectedActiveRowBorder = createCompoundBorder(
            createMatteBorder(0, 0, 1, 0, new Color(125, 170, 234)), 
                    createEmptyBorder(1, 5, 0, 5));
    final static Border selectedInactiveRowBorder = createCompoundBorder(
            createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)), 
                    createEmptyBorder(1, 5, 0, 5));
    
    private final TableCellRenderer delegate;
    private final TableCellEditor editor;
    
    public SDDefaultTableCellRenderer(TableCellRenderer delegate, TableCellEditor editor) {
        this.delegate = delegate;
        this.editor = editor;
    }

    public SDDefaultTableCellRenderer() {
        this(new DefaultTableCellRenderer(), null);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        Component c = delegate.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        
        if (c instanceof JComponent) {
            renderCell((JComponent) c, table, isSelected, row);
        }
        return c;
    }


    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column) {
        
        Component c = editor.getTableCellEditorComponent(table, value, isSelected, row, column);

        if (c instanceof JComponent) {
            renderCell((JComponent) c, table, isSelected, row);
        }
        return c;
    }

    private void renderCell(JComponent cell, JTable table, boolean isSelected, int row) {
        cell.setOpaque(isSelected);
        
        final Border border = !table.isRowSelected(row) ? rowBorder
            : isParentWindowFocused(table) ? selectedActiveRowBorder
                : selectedInactiveRowBorder;
        
        cell.setBorder(border);
    }
    
    public static boolean isParentWindowFocused(Component component) {
        Window window = SwingUtilities.getWindowAncestor(component);
        return window != null && window.isFocused();
    }

    public Object getCellEditorValue() {
        return editor.getCellEditorValue();
    }

    public boolean isCellEditable(EventObject anEvent) {
        return editor.isCellEditable(anEvent);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return editor.shouldSelectCell(anEvent);
    }

    public boolean stopCellEditing() {
        return editor.stopCellEditing();
    }

    public void cancelCellEditing() {
        editor.cancelCellEditing();
    }

    public void addCellEditorListener(CellEditorListener l) {
        editor.addCellEditorListener(l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        editor.removeCellEditorListener(l);
    }
}
