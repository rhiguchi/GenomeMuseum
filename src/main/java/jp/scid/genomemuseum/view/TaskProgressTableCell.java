package jp.scid.genomemuseum.view;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingWorker.StateValue;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jp.scid.genomemuseum.model.TaskProgressModel;
import jp.scid.gui.AbstractTableCellComponent;

public class TaskProgressTableCell extends AbstractTableCellComponent implements TableCellRenderer, TableCellEditor {
    final TableCellEditor baseEditor;
    
    final TaskProgressView rendererView = new TaskProgressView();
    final TaskProgressView editorView = new TaskProgressView();
    
    private TaskProgressModel editorValue = null;
    
    public TaskProgressTableCell() {
        this(new DefaultTableCellRenderer());
    }
    
    public TaskProgressTableCell(TableCellRenderer baseRenderer) {
        this(baseRenderer, new DefaultCellEditor(new JCheckBox()));
    }
    
    public TaskProgressTableCell(TableCellRenderer baseRenderer, TableCellEditor baseEditor) {
        super(baseRenderer);
        this.baseEditor = baseEditor;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        Component comp = baseRenderer.getTableCellRendererComponent(table,
                value, true, false, row, column);
        makeCell(editorView, comp);
        setValueTo(editorView, (TaskProgressModel) value);
        
        return editorView;
    }
    
    public void setExecuteButtonAction(Action action) {
        editorView.setExecuteButtonAction(action);
    }
    
    @Override
    public TaskProgressView getRendererView() {
        return rendererView;
    }
    
    @Override
    protected void setValueToRendererView(Object value) {
        setValueTo(rendererView, (TaskProgressModel) value);
    }

    protected void setValueTo(TaskProgressView cell, TaskProgressModel value) {
        cell.setLabelAvailable(value.isAvailable());
        cell.setDownloadButtonEnabled(value.isAvailable() && value.getState() == StateValue.PENDING);
        cell.setProgressVisible(value.getState() == StateValue.STARTED);
        cell.setProgress(value.getProgress());
        cell.setText(value.getLabel());
    }
    
    protected void setValueToRenderView(Object value) {
        setValueTo(rendererView, (TaskProgressModel) value);
    }
    
    public TaskProgressModel getCellEditorValue() {
        return editorValue;
    }
    
    public boolean isCellEditable(EventObject anEvent) {
        return baseEditor.isCellEditable(anEvent);
    }
    
    public boolean shouldSelectCell(EventObject anEvent) {
        return baseEditor.shouldSelectCell(anEvent);
    }
    
    public void addCellEditorListener(CellEditorListener l) {
        baseEditor.addCellEditorListener(l);
    }
    
    public void removeCellEditorListener(CellEditorListener l) {
        baseEditor.removeCellEditorListener(l);
    }
    
    public boolean stopCellEditing() {
        return baseEditor.stopCellEditing();
    }
    
    public void cancelCellEditing() {
        baseEditor.cancelCellEditing();
    }
}
