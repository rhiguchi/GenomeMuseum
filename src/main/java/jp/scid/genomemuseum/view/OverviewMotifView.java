package jp.scid.genomemuseum.view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import jp.scid.motifviewer.view.OverviewPane;

public class OverviewMotifView {
    public final OverviewPane overviewPane = new OverviewPane();
    
    public final JTextField searchMotifField = new JTextField();
    public final JTable motifListTable = new JTable(0, 3);
    public final JScrollPane motifListTableScroll = new JScrollPane(motifListTable);
    
    public final JPanel motifSearchPanel = createMotifSearchPanel(searchMotifField, motifListTableScroll);
    
    public final JSplitPane overviewSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, overviewPane, motifSearchPanel);

    public final JPanel contentPane = createContentPane(overviewSplit);

    public JPanel getContentPane() {
        return contentPane;
    }
    
    JPanel createMotifSearchPanel(JTextField searchMotifField, JScrollPane motifListTableScroll) {
        JPanel motifSearchPanel = new JPanel(new BorderLayout());
        motifSearchPanel.add(searchMotifField, "North");
        motifSearchPanel.add(motifListTableScroll, "Center");
        
        return motifSearchPanel;
    }
    
    JPanel createContentPane(JSplitPane overviewSplit) {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setPreferredSize(new Dimension(400, 300));
        contentPane.add(overviewSplit, "Center");
        
        overviewSplit.setDividerLocation(200);
        
        return contentPane;
    }
}
