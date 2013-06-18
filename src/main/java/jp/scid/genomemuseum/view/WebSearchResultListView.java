package jp.scid.genomemuseum.view;

import jp.scid.genomemuseum.model.TaskProgressModel;
import jp.scid.gui.view.SDDefaultTableCellRenderer;

public class WebSearchResultListView extends RecordListView {

    public final TaskProgressTableCell taskProgressCell =
            new TaskProgressTableCell(new SDDefaultTableCellRenderer());
    
    public WebSearchResultListView() {
        table.setDefaultRenderer(TaskProgressModel.class, taskProgressCell);
        table.setDefaultEditor(TaskProgressModel.class, taskProgressCell);
    }
}