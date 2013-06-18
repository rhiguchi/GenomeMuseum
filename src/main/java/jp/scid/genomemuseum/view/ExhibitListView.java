package jp.scid.genomemuseum.view;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;
//import jp.scid.motifviewer.gui.MotifViewerView;

public class ExhibitListView extends RecordListView {
    // Content Viewer
    public final FileContentView fileContentView = new FileContentView();
    
    // Overview
//    public final MotifViewerView overviewMotifView = new MotifViewerView();
    
    // Tab view
    public final JTabbedPane contentsViewTabbedPane =
            createContentsViewTabbedPane(fileContentView);
//            createContentsViewTabbedPane(fileContentView, overviewMotifView);
    
    // Data and content area
    public final JSplitPane dataListContentSplit;
    
    public ExhibitListView() {
        dataListContentSplit =
                createTableContentSplit(tableContainer, contentsViewTabbedPane);
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
    
    /**
     * @return tabbed pane
     */
//    private static JTabbedPane createContentsViewTabbedPane(
//            FileContentView fileContentView, MotifViewerView overviewMotifView) {
    private static JTabbedPane createContentsViewTabbedPane(
            FileContentView fileContentView) {
        JTabbedPane contentsViewTabbedPane = new JTabbedPane();
        
        contentsViewTabbedPane.addTab("Content", fileContentView.getContentPane());
//        contentsViewTabbedPane.addTab("MotifView", overviewMotifView.getContentPane());
        
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
