package jp.scid.genomemuseum.view;

import static javax.swing.BorderFactory.*;
import static javax.swing.SpringLayout.*;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.DefaultTreeCellEditor;

import jp.scid.genomemuseum.view.folder.FolderTreeCellRenderer;
import jp.scid.gui.plaf.SourceListTreeUI;
import jp.scid.motifviewer.gui.MotifViewerView;

import com.explodingpixels.macwidgets.ComponentBottomBar;
import com.explodingpixels.macwidgets.MacButtonFactory;
import com.explodingpixels.macwidgets.MacIcons;
import com.explodingpixels.macwidgets.MacWidgetFactory;
import com.explodingpixels.macwidgets.UnifiedToolBar;

public class MainView implements GenomeMuseumView {
    public static enum ContentsMode {
        LOCAL, NCBI;
    }
    
    private static final Color COLOR_ACTIVITY_FOREGROUND = new Color(0f, 0f, 0f, 0.8f);
    
    
    // Exhibit List Table
    private final ExhibitListView exhibitListView = new ExhibitListView();
    private final WebSearchResultListView webSearchResultListView = new WebSearchResultListView();
    private final ModeChangePane modeChangePane = new ModeChangePane();
    
    // Search Status
    public final JLabel statusLabel = new JLabel("status"); {
        statusLabel.setPreferredSize(new Dimension(200, 28));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    // Source List Area
    public final FolderTreeCellRenderer sourceListCellRenderer = new FolderTreeCellRenderer();
    public final SourceListCellEditor sourceListCellEditor = new SourceListCellEditor();
    public final JTree sourceList = createSourceList(sourceListCellRenderer, sourceListCellEditor);

    public final JScrollPane sourceListScroll = MacWidgetFactory.createSourceListScrollPane(sourceList); {
        sourceListScroll.setPreferredSize(new Dimension(260, 260));
        sourceListScroll.setMinimumSize(new Dimension(120, 260));
    }
    
    // Activity Area
    // - Loading status
    public final JProgressBar fileLoadingProgress = new JProgressBar();
    public final JLabel fileLoadingStatus = new JLabel("Loading Status"); {
        fileLoadingStatus.setFont(fileLoadingStatus.getFont().deriveFont(11f));
        fileLoadingStatus.setMinimumSize(new Dimension(0, 0));
        fileLoadingStatus.setForeground(COLOR_ACTIVITY_FOREGROUND);
        fileLoadingStatus.setHorizontalAlignment(SwingConstants.RIGHT);
    }
    public final JComponent fileLoadingActivityPane = new JPanel(); {
        JComponent parent = fileLoadingActivityPane;
        parent.setOpaque(false);
        
        SpringLayout layout = new SpringLayout();
        parent.setLayout(layout);
        
        parent.add(fileLoadingProgress);
        layout.putConstraint(WEST, fileLoadingProgress, 0, WEST, parent);
        layout.putConstraint(NORTH, fileLoadingProgress, 6, NORTH, parent);
        layout.putConstraint(EAST, fileLoadingProgress, 0, EAST, parent);
        
        parent.add(fileLoadingStatus);
        layout.putConstraint(WEST, fileLoadingStatus, 0, WEST, parent);
        layout.putConstraint(NORTH, fileLoadingStatus, 6, SOUTH, fileLoadingProgress);
        layout.putConstraint(EAST, fileLoadingStatus, 0, EAST, parent);
        
        layout.putConstraint(SOUTH, parent, 6, SOUTH, fileLoadingStatus);
    }
    
    public final JLabel activityAreaTitle = new JLabel("GenomeMuseum Activity"); {
        activityAreaTitle.setFont(activityAreaTitle.getFont().deriveFont(11f).deriveFont(Font.BOLD));
        activityAreaTitle.setForeground(COLOR_ACTIVITY_FOREGROUND);
        activityAreaTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        activityAreaTitle.setMinimumSize(new Dimension(0, 0));
    }
    /** Activity Pane */
    public final JComponent activityPane = new JPanel(); {
        JComponent parent = activityPane;
        parent.setOpaque(false);
        parent.setBorder(createCompoundBorder(
                createMatteBorder(1, 0, 0, 0, new Color(0f, 0f, 0f, 0.3f)),
                createEmptyBorder(4, 8, 0, 8)));
        parent.setPreferredSize(new Dimension(500, 100));
        
        parent.setLayout(new BoxLayout(parent, BoxLayout.PAGE_AXIS));
        
        parent.add(activityAreaTitle);
        parent.add(fileLoadingActivityPane);
    }
    
    // Top
    private final UnifiedToolBar toolBarView = new UnifiedToolBar();
    
    // Bottom left
    public final JMenuItem addListBox = new JMenuItem("addBasicRoom");
    public final JMenuItem addSmartBox = new JMenuItem("addSmartRoom");
    public final JMenuItem addBoxFolder = new JMenuItem("addGroupRoom");
    public final JPopupMenu addBoxPopup =
            createPopupMenu("addBoxPopup", addBoxFolder, addListBox, addBoxFolder);
    
    public final JMenuItem editSmartCondition = new JMenuItem("editSmartCondition");
    public final JPopupMenu boxFunctionPopup =
            createPopupMenu("boxFunctionPopup", editSmartCondition);
   
    public final JToggleButton addBoxButton = new JToggleButton(MacIcons.PLUS);
    public final JButton removeBoxButton =
            (JButton) MacButtonFactory.createGradientButton(MacIcons.MINUS, null);
    public final JToggleButton boxFunctionsButton = new JToggleButton(MacIcons.GEAR); {
        boxFunctionsButton.setEnabled(false);
    }
    
    public final JButton activityPaneVisiblityButton = new JButton("activityPaneVisiblity");
    
    private final ComponentBottomBar fComponentBottomBar =
            createSourceFunctionsBar(addBoxButton, removeBoxButton, boxFunctionsButton);
    
    public final JPanel sourceListPane =
            createSourceListPane(sourceListScroll, activityPane, fComponentBottomBar.getComponent());
    
    public final JSplitPane sourceListDataTableSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, sourceListPane,
            modeChangePane.recordListViewContainer());

    public final JPanel contentPane = createContentPane(toolBarView.getComponent(), sourceListDataTableSplit);

    public MainView() {
        PopupToggleHandler addBoxToggleHandler = new PopupToggleHandler(addBoxPopup);
        addBoxToggleHandler.installTo(addBoxButton);
        
        PopupToggleHandler functionToggleHandler = new PopupToggleHandler(boxFunctionPopup);
        functionToggleHandler.installTo(boxFunctionsButton);
        
        modeChangePane.addView(ContentsMode.LOCAL,
                exhibitListView.toolContainer(), exhibitListView.getContainer());
        modeChangePane.addView(ContentsMode.NCBI,
                webSearchResultListView.toolContainer(), webSearchResultListView.getTableContainer());
        
        toolBarView.addComponentToRight(modeChangePane.toolBarContainer());
    }
    
    public JPanel getContentPane() {
        return contentPane;
    }
    
    public JTable sequenceTable() { return exhibitListView.getTable(); }
    public JTextField sequenceSearchField() { return exhibitListView.getSearchField();}
    
    public FileContentView getFileContentView() { return exhibitListView.getFileContentView(); }
    public MotifViewerView getMotifViewerView() { return exhibitListView.getMotifViewerView(); }
    
    public JTable websearchTable() { return webSearchResultListView.getTable(); }
    public JTextField websearchField() { return webSearchResultListView.getSearchField(); }
    public JLabel loadingIconLabel() { return webSearchResultListView.loadingIconLabel(); }
    public JButton websearchCancelButton() { return webSearchResultListView.cancelButton(); }
    public WebSearchResultListView webSearchResultListView() { return webSearchResultListView; }
    
    public void setContentsMode(ContentsMode mode) {
        modeChangePane.setContentsMode(mode);
    }
    
    static class PopupToggleHandler {
        private final JPopupMenu popupMenu;
        private final ActionListener toggleAction;
        
        public PopupToggleHandler(JPopupMenu popupMenu) {
            this.popupMenu = popupMenu;
            toggleAction = new PopupToggleAction(popupMenu);
        }

        public void installTo(JToggleButton toggle) {
            toggle.addActionListener(toggleAction);
            
            ButtonSelectionCancelHandler cancelHandler = new ButtonSelectionCancelHandler(toggle);
            popupMenu.addPopupMenuListener(cancelHandler);
            popupMenu.addPropertyChangeListener(cancelHandler);
            
            // Install a special client property on the button to prevent it from
            // closing of the popup when the down arrow is pressed.
            Object preventHide = new JComboBox().getClientProperty("doNotCancelPopup");
            toggle.putClientProperty("doNotCancelPopup", preventHide);
        }
        
        private static class PopupToggleAction implements ActionListener {
            private final JPopupMenu menu;
            
            public PopupToggleAction(JPopupMenu menu) {
                this.menu = menu;
            }
            
            public void actionPerformed(ActionEvent e) {
                AbstractButton button = (AbstractButton) e.getSource();
                if( button.isSelected() )
                    menu.show(button, 0, button.getHeight());
                else
                    menu.setVisible(false);
            }
        }
        
        private static class ButtonSelectionCancelHandler implements PopupMenuListener, PropertyChangeListener {
            private final AbstractButton button;
            public ButtonSelectionCancelHandler(AbstractButton button) {
                this.button = button;
            }
            
            public void popupMenuWillBecomeVisible ( PopupMenuEvent e ) {}
            public void popupMenuWillBecomeInvisible ( PopupMenuEvent e ) {}
            public void popupMenuCanceled ( PopupMenuEvent e ) {
                button.setSelected(false);
            }
            
            public void propertyChange(PropertyChangeEvent e) {
                if(Boolean.FALSE.equals(e.getNewValue()))
                    button.setSelected(false);
            }
        }
    }

    private static JTree createSourceList(FolderTreeCellRenderer cellRenderer, SourceListCellEditor cellEditor) {
        JTree tree = new JTree();
        
        tree.setUI(new SourceListTreeUI());
        tree.setCellEditor(new DefaultTreeCellEditor(tree, null, cellEditor));
        
        tree.setCellRenderer(cellRenderer);
        
        tree.setRootVisible(false);
        tree.setInvokesStopCellEditing(true);
        tree.setToggleClickCount(0);
        tree.setEditable(true);
        
        return tree;
    }
    
    private static JPopupMenu createPopupMenu(String name, JMenuItem... items){
        JPopupMenu popup = new JPopupMenu(name);
        for (JMenuItem item: items) {
            popup.add(item);
        }
        return popup;
    }

    private static ComponentBottomBar createSourceFunctionsBar(
            JToggleButton addBoxButton, JButton removeBoxButton, JToggleButton boxFunctionsButton) {
        ComponentBottomBar bar = MacWidgetFactory.createComponentStatusBar();
        
        bar.getComponent().setBorder(null);
        
        makeComponentBottomButton(addBoxButton);
        bar.addComponentToLeftWithBorder(addBoxButton);
        
        makeComponentBottomButton(removeBoxButton);
        bar.addComponentToLeftWithBorder(removeBoxButton);
        
        makeComponentBottomButton(boxFunctionsButton);
        bar.addComponentToLeftWithBorder(boxFunctionsButton);
        bar.getComponent().setPreferredSize(new Dimension(-1, 24));
        
        return bar;
    }
    
    private static JPanel createSourceListPane(
            JComponent sourceListScroll, JComponent activityPane, JComponent sourceFunction) {
        JComponent sourceTop = new JPanel(new BorderLayout());
        sourceTop.setOpaque(false);

        sourceTop.add(sourceListScroll, "Center");
        sourceTop.add(activityPane, "South");
        
        JPanel sourceListPane = new JPanel(new BorderLayout());
        sourceListPane.add(sourceTop, "Center");
        sourceListPane.add(sourceFunction, "South");
        
        return sourceListPane;
    }
    
    private static JPanel createContentPane(JComponent toolBarPane, JSplitPane sourceListDataTableSplit) {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(toolBarPane, "North");
        contentPane.add(sourceListDataTableSplit, "Center");
        contentPane.setPreferredSize(new Dimension(600, 400));
        
        return contentPane;
    }
    
    private static void makeComponentBottomButton(AbstractButton button) {
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setPreferredSize(new Dimension(32, -1));
    }
    
    static class ModeChangePane {
        private final CardLayout toolBarContainerLayout;
        private final JPanel toolBarContainer = new JPanel(toolBarContainerLayout = new CardLayout());
        
        private final CardLayout recordListViewContainerLayout;
        private final JPanel recordListViewContainer = new JPanel(recordListViewContainerLayout = new CardLayout());
        
        public ModeChangePane() {
            toolBarContainer.setOpaque(false);
            toolBarContainer.setPreferredSize(new Dimension(240, 28));
        }
        
        public void addView(ContentsMode mode, JComponent toolBar, JComponent recordListView) {
            recordListViewContainer.add(recordListView, mode.name());
            toolBarContainer.add(toolBar, mode.name());
        }

        public void setContentsMode(ContentsMode mode) {
            String modeName = mode.name();
            toolBarContainerLayout.show(toolBarContainer, modeName);
            recordListViewContainerLayout.show(recordListViewContainer, modeName);
        }
        
        public JComponent toolBarContainer() {
            return toolBarContainer;
        }
        
        public JComponent recordListViewContainer() {
            return recordListViewContainer;
        }
    }
}
