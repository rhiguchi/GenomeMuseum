package jp.scid.genomemuseum.view;

import static javax.swing.SpringLayout.*;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import jp.scid.genomemuseum.model.TaskProgressModel;
import jp.scid.gui.view.SDDefaultTableCellRenderer;

public class WebSearchResultListView extends RecordListView {

    private final static Icon loadingIcon = new ImageIcon(MainView.class.getResource("loading.gif"));
    
    public final TaskProgressTableCell taskProgressCell =
            new TaskProgressTableCell(new SDDefaultTableCellRenderer());
    
    private final JLabel loadingIconLabel = new JLabel("progress text...", loadingIcon, SwingConstants.LEFT);
    
    private final JButton cancelButton = new JButton("Cancel");
    
    
    public WebSearchResultListView() {
        table.setDefaultRenderer(TaskProgressModel.class, taskProgressCell);
        table.setDefaultEditor(TaskProgressModel.class, taskProgressCell);
        
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
    
    public JLabel loadingIconLabel() {
        return loadingIconLabel;
    }
    
    public JButton cancelButton() {
        return cancelButton;
    }
}