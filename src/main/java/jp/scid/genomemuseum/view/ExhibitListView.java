package jp.scid.genomemuseum.view;

import static javax.swing.SpringLayout.*;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpringLayout;
import javax.swing.text.JTextComponent;
import jp.scid.motifviewer.gui.MotifViewerView;

public class ExhibitListView extends RecordListView {
    // Content Viewer
    public final FileContentView fileContentView = new FileContentView();
    
    // Overview
    public final MotifViewerView motifViewerView = new MotifViewerView();
    
    // Tab view
    public final JTabbedPane contentsViewTabbedPane =
            createContentsViewTabbedPane(fileContentView, motifViewerView);
    
    // Data and content area
    public final JSplitPane dataListContentSplit;
    
    public ExhibitListView() {
        dataListContentSplit =
                createTableContentSplit(tableContainer, contentsViewTabbedPane);
        
        SpringLayout layout = new SpringLayout();
        toolContainer.setLayout(layout);
        
        addToToolContainer(searchField);
        layout.putConstraint(EAST, searchField, 0, EAST, toolContainer);
        layout.putConstraint(NORTH, searchField, 0, NORTH, toolContainer);
        layout.getConstraints(toolContainer).setWidth(layout.getConstraint(WIDTH, searchField));
        layout.getConstraints(toolContainer).setHeight(layout.getConstraint(HEIGHT, searchField));
    }
    
    /**
     * @return container
     */
    public JComponent getContainer() {
        return dataListContentSplit;
    }
    
    public JTextComponent getContentViewComponent() {
        return fileContentView.textArea;
    }
    
    public FileContentView getFileContentView() {
        return fileContentView;
    }
    
    public MotifViewerView getMotifViewerView() {
        return motifViewerView;
    }
    
    /**
     * @return tabbed pane
     */
    private static JTabbedPane createContentsViewTabbedPane(
            FileContentView fileContentView, MotifViewerView overviewMotifView) {
        JTabbedPane contentsViewTabbedPane = new JTabbedPane();
        
        contentsViewTabbedPane.addTab("Content", fileContentView.getContentPane());
        contentsViewTabbedPane.addTab("MotifView", overviewMotifView.getContentPane());
        
        return contentsViewTabbedPane;
    }

    /**
     * @return table detail split
     */
    private static JSplitPane createTableContentSplit(
            JScrollPane dataTableScroll, JTabbedPane contentsViewTabbedPane) {
        JSplitPane splitPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, dataTableScroll,
                contentsViewTabbedPane);
        splitPane.setDividerLocation(Integer.MAX_VALUE);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1);
        
        return splitPane;
    }
}
