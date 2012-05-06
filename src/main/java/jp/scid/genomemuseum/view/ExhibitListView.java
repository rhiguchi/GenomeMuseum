package jp.scid.genomemuseum.view;

import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;

import jp.scid.gui.MessageFormatTableCell;
import jp.scid.motifviewer.gui.MotifViewerView;

import com.explodingpixels.macwidgets.plaf.ITunesTableUI;

public class ExhibitListView {
    // Data List
    public final JTable dataTable = createTable();
    public final JScrollPane dataTableScroll = new JScrollPane(dataTable,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    // Content Viewer
    public final FileContentView fileContentView = new FileContentView();
    
    // Overview
    public final MotifViewerView overviewMotifView = new MotifViewerView();
    
    // Tab view
    public final JTabbedPane contentsViewTabbedPane =
            createContentsViewTabbedPane(fileContentView, overviewMotifView);
    
    // Data and content area
    public final JSplitPane dataListContentSplit =
            createTableContentSplit(dataTableScroll, contentsViewTabbedPane);
    
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
     * @return exhibit list table
     */
    private static JTable createTable() {
        JTable table = new JTable();
        
        table.setUI(new ITunesTableUI());
        TableCellRenderer defaultRenderer = table.getDefaultRenderer(Object.class);
        
        MessageFormatTableCell intValueCell =
            new MessageFormatTableCell(new DecimalFormat("#,##0"), defaultRenderer);
        intValueCell.getRendererView().setHorizontalAlignment(SwingConstants.RIGHT);
        table.setDefaultRenderer(Integer.class, intValueCell);

        return table;
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
