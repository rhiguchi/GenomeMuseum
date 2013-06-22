package jp.scid.genomemuseum.view;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.SwingWorker.StateValue;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jp.scid.genomemuseum.model.TaskProgressModel;

public class DownloadTaskTableCell extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
    final TaskProgressView rendererView = new TaskProgressView();
    final TaskProgressView editorView = new TaskProgressView();
    
    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if ((value instanceof TaskProgressModel)) {
            renderCell(rendererView, (TaskProgressModel) value);
        }
        rendererView.setFont(table.getFont());
        return rendererView.getComponent();
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column) {
        if ((value instanceof TaskProgressModel)) {
            renderCell(editorView, (TaskProgressModel) value);
        }
        editorView.setFont(table.getFont());
        return editorView.getComponent();
    }

    protected void renderCell(TaskProgressView cell, TaskProgressModel value) {
        cell.setLabelAvailable(value.isAvailable());
        cell.setDownloadButtonEnabled(value.isAvailable() && value.getState() == StateValue.PENDING);
        cell.setProgressVisible(value.getState() == StateValue.STARTED);
        cell.setProgress(value.getProgress());
        cell.setText(value.getLabel());
    }
    
    public void setDownloadButtonAction(Action action) {
        editorView.setExecuteButtonAction(action);
    }
}
