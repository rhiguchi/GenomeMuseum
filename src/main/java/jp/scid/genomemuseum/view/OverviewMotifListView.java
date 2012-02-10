package jp.scid.genomemuseum.view;

import static javax.swing.BorderFactory.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import jp.scid.motifviewer.view.OverviewPane;

/**
 * 
 * @author Ryusuke Higuchi
 * @see jp.scid.genomemuseum.controller.OverviewMotifListController
 */
public class OverviewMotifListView {
    // Tools
    // Tools - Strand
    /** Button to make view strand single  */
    public final JToggleButton singeStrandButton = createAnnotationStyleButton("Single", "first");
    /** Button to make view strand double  */
    public final JToggleButton doubleStrandButton = createAnnotationStyleButton("Double", "last");
    
    /** Label for strand buttons  */
    private final JLabel strandButtonsLabel = new JLabel("Strand");
    
    /** Parent pane for strand buttons  */
    private final JPanel strandButtonsPane = createBottomLabeledComponent(
            createStyleButtonsPanel(singeStrandButton, doubleStrandButton), strandButtonsLabel);
    
    // Tools - Style
    /** Button to make view shape circular  */
    public final JToggleButton circularShapeButton = createAnnotationStyleButton("Circular", "first");
    /** Button to make view shape linear  */
    public final JToggleButton linearShapeButton = createAnnotationStyleButton("Linear", "last");
    
    /** Label for shape buttons  */
    private final JLabel shapeButtonsLabel = new JLabel("Shape");
    
    /** Parent pane for shape buttons  */
    private final JPanel shapeButtonsPane = createBottomLabeledComponent(
            createStyleButtonsPanel(circularShapeButton, linearShapeButton), shapeButtonsLabel);

    // Tools - Overview zoom
    public final JSlider zoomSlider = createZoomSlider();

    private final JLabel zoomSliderLabel = new JLabel("Zoom");
    
    /** Parent pane for shape buttons  */
    private final JPanel zoomSliderPane = createBottomLabeledComponent(zoomSlider, zoomSliderLabel);
    
    // Motif search
    /** input field for search motif */
    public final JTextField searchMotifField = new JTextField();
    
    /** label for search motif field */
    private final JLabel searchMotifLabel = new JLabel("Motif Search");
    
    /** parent pane for search motif field */
    private final JPanel searchMotifFieldPane =
            createBottomLabeledComponent(searchMotifField, searchMotifLabel);
    
    /** Tools pane */
    private final JPanel toolsPane = createToolsPane(
            strandButtonsPane, shapeButtonsPane, zoomSliderPane, searchMotifFieldPane);
    
    // Contents area
    public final OverviewPane overviewPane = new OverviewPane();
    
    public final JTable motifListTable = createMotifListTable();

    public final JScrollPane motifListTableScroll = new JScrollPane(motifListTable);
    
    public final JPanel motifSearchPanel = createMotifSearchPanel(motifListTableScroll);
    
    public final JSplitPane overviewSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, overviewPane, motifSearchPanel);

    public final JPanel contentPane = createContentPane(overviewSplit, toolsPane);

    /**
     * Constructor
     */
    public OverviewMotifListView() {
    }
    
    public JPanel getContentPane() {
        return contentPane;
    }
    
    JPanel createMotifSearchPanel(JScrollPane motifListTableScroll) {
        JPanel motifSearchPanel = new JPanel(new BorderLayout());
        motifSearchPanel.add(motifListTableScroll, "Center");
        
        return motifSearchPanel;
    }
    
    JPanel createContentPane(JSplitPane overviewSplit, JPanel toolsPane) {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setPreferredSize(new Dimension(400, 300));
        
        toolsPane.setBorder(createEmptyBorder(8, 16, 8, 16));
        
        contentPane.add(overviewSplit, "Center");
        contentPane.add(toolsPane, "North");
        
        overviewSplit.setDividerLocation(200);
        
        return contentPane;
    }

    static JSlider createZoomSlider() {
        JSlider slider = new JSlider(0, 100, 50);
        slider.setPreferredSize(new Dimension(120, 28));
        slider.setMaximumSize(new Dimension(120, 28));
        
        slider.setExtent(10);
        slider.setMinorTickSpacing(10);
        slider.setSnapToTicks(true);
        
        return slider;
    }
    
    static JToggleButton createAnnotationStyleButton(String text, String segmentPosition) {
        JToggleButton button = new JToggleButton(text);
        button.putClientProperty("JButton.buttonType", "segmentedTextured");
        button.putClientProperty("JButton.segmentPosition", segmentPosition);
        button.setFocusPainted(false);
        return button;
    }

    static JTable createMotifListTable() {
        JTable table = new JTable(0, 3);
        table.setFont(new Font("monospaced", Font.PLAIN, 13));
        return table;
    }
    
    static JPanel createToolsPane(
            JComponent strandButtonsPane, JComponent shapeButtonsPane, JPanel zoomSliderPane,
            JComponent searchMotifFieldPane) {
        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.LINE_AXIS);
        panel.setLayout(layout);
        
        panel.add(strandButtonsPane);
        strandButtonsPane.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        
        panel.add(Box.createHorizontalStrut(16));
        
        panel.add(shapeButtonsPane);
        shapeButtonsPane.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        
        panel.add(Box.createHorizontalStrut(16));
        
        panel.add(zoomSliderPane);
        zoomSliderPane.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        
        panel.add(Box.createHorizontalGlue());
        
        panel.add(Box.createHorizontalStrut(16));
        
        panel.add(searchMotifFieldPane);
        searchMotifFieldPane.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        
        return panel;
    }
    
    static JPanel createBottomLabeledComponent(JComponent comp, JLabel label) {
        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(layout);
        panel.setOpaque(false);
        
        panel.add(comp);
        comp.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(label);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(label.getFont().deriveFont(11f));
        
        return panel;
    }
    
    static JPanel createStyleButtonsPanel(AbstractButton... buttons) {
        JPanel buttonPane = new JPanel();
        BoxLayout layout = new BoxLayout(buttonPane, BoxLayout.LINE_AXIS);
        buttonPane.setLayout(layout);
        buttonPane.setOpaque(false);
        
        for (AbstractButton button: buttons) {
            button.setAlignmentY(Component.CENTER_ALIGNMENT);
            buttonPane.add(button);
        }
        
        return buttonPane;
    }
}
