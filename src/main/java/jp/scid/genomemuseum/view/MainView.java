package jp.scid.genomemuseum.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.DefaultTreeCellEditor;

import jp.scid.gui.plaf.SourceListTreeUI;

import com.explodingpixels.macwidgets.ComponentBottomBar;
import com.explodingpixels.macwidgets.MacButtonFactory;
import com.explodingpixels.macwidgets.MacIcons;
import com.explodingpixels.macwidgets.MacWidgetFactory;
import com.explodingpixels.macwidgets.UnifiedToolBar;

public class MainView implements GenomeMuseumView {
    private final ComponentFactory factory = new ComponentFactory();
    private final static Icon loadingIcon = new ImageIcon(MainView.class.getResource("loading.gif"));
    
    // Data table
    public final JTable dataTable = MacWidgetFactory.createITunesTable(null);
    public final JScrollPane dataTableScroll = new JScrollPane(dataTable,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    
    // Content Viewer
    public final FileContentView fileContentView = new FileContentView();
    
    // Data and content area
    public final JSplitPane dataListContentSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, true, dataTableScroll,
            fileContentView.getContentPane());
    {
        dataListContentSplit.setDividerLocation(Integer.MAX_VALUE);
        dataListContentSplit.setOneTouchExpandable(true);
        dataListContentSplit.setResizeWeight(1);
    }
    
    // Search Field
    public final JTextField quickSearchField = new JTextField(); {
        quickSearchField.setPreferredSize(new Dimension(200, 28));
    }
    
    // Search Status
    public final JLabel statusLabel = new JLabel("status"); {
        statusLabel.setPreferredSize(new Dimension(200, 28));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    }
    // Loading Icon
    public final JLabel loadingIconLabel = new JLabel(loadingIcon); {
//        loadingIconLabel.setPreferredSize(new Dimension(24, 24));
    }

    public final SourceListCellEditor sourceListCellEditor = new SourceListCellEditor();
    public final JTree sourceList = new JTree(); {
        sourceList.setUI(new SourceListTreeUI());
        sourceList.setCellEditor(new DefaultTreeCellEditor(sourceList,
                null, sourceListCellEditor));
        sourceList.setRootVisible(false);
        sourceList.setInvokesStopCellEditing(true);
        sourceList.setToggleClickCount(0);
        sourceList.setEditable(true);

    }
    public final JScrollPane sourceListScroll = MacWidgetFactory.createSourceListScrollPane(sourceList);
    
    // Top
    private final UnifiedToolBar toolBarView = new UnifiedToolBar();
    public final JComponent toolBarPane = toolBarView.getComponent(); {
        toolBarView.addComponentToRight(statusLabel);
        toolBarView.addComponentToRight(quickSearchField);
        toolBarView.addComponentToRight(loadingIconLabel);
    }
    
    // Bottom left
    public final JMenuItem addListBox = factory.newMenuItem("addListBox");
    public final JMenuItem addSmartBox = factory.newMenuItem("addSmartBox");
    public final JMenuItem addBoxFolder = factory.newMenuItem("addBoxFolder");
    public final JPopupMenu addBoxPopup = factory.createPopupMenu("addBoxPopup"); {
        addBoxPopup.add(addListBox);
        addBoxPopup.add(addSmartBox);
        addBoxPopup.add(addBoxFolder);
    }
    
    public final JMenuItem editSmartCondition = factory.newMenuItem("editSmartCondition");
    public final JPopupMenu boxFunctionPopup = factory.createPopupMenu("boxFunctionPopup"); {
        boxFunctionPopup.add(editSmartCondition);
    }
   
    public final JToggleButton addBoxButton = factory.newToggleButton("addBox"); {
        addBoxButton.setBorder(BorderFactory.createEmptyBorder());
        addBoxButton.setPreferredSize(new Dimension(32, -1));
        addBoxButton.setText("");
        addBoxButton.setIcon(MacIcons.PLUS);
        factory.installPopupButton(addBoxButton, addBoxPopup);
    }
    public final JButton removeBoxButton = factory.newEPButton("removeBox"); {
        removeBoxButton.setBorder(BorderFactory.createEmptyBorder());
        removeBoxButton.setPreferredSize(new Dimension(32, -1));
        removeBoxButton.setText("");
        removeBoxButton.setIcon(MacIcons.MINUS);
    }
    public final JToggleButton boxFunctionsButton = factory.newToggleButton("boxFunctions"); {
        boxFunctionsButton.setBorder(BorderFactory.createEmptyBorder());
        boxFunctionsButton.setPreferredSize(new Dimension(32, -1));
        boxFunctionsButton.setText("");
        boxFunctionsButton.setIcon(MacIcons.GEAR);
        factory.installPopupButton(boxFunctionsButton, boxFunctionPopup);
    }
    public final JButton activityPaneVisiblityButton = factory.newButton("activityPaneVisiblity");
    
    private final ComponentBottomBar fComponentBottomBar = MacWidgetFactory.createComponentStatusBar(); {
        fComponentBottomBar.getComponent().setBorder(null);
        fComponentBottomBar.addComponentToLeftWithBorder(addBoxButton);
        fComponentBottomBar.addComponentToLeftWithBorder(removeBoxButton);
        fComponentBottomBar.addComponentToLeftWithBorder(boxFunctionsButton);
        fComponentBottomBar.getComponent().setPreferredSize(new Dimension(-1, 24));
    }
    public final JPanel sourceListPane = new JPanel(new BorderLayout()); {
        sourceListPane.add(sourceListScroll, "Center");
        sourceListPane.add(fComponentBottomBar.getComponent(), "South");
    }
    public final JSplitPane sourceListDataTableSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true,
            sourceListPane, dataListContentSplit);

    public final JPanel contentPane = new JPanel(new BorderLayout()); {
        contentPane.add(toolBarPane, "North");
        contentPane.add(sourceListDataTableSplit, "Center");
        contentPane.setPreferredSize(new Dimension(600, 400));
    }

    public MainView() {
    }
    
    public JPanel getContentPane() {
        return contentPane;
    }
    
    /**
     * @return whether viewer is closed
     */
    public boolean isContentViewerClosed() {
        return dataListContentSplit.getDividerLocation() >= dataListContentSplit.getMaximumDividerLocation();
    }
    
    /**
     * set the size for the viewer
     * @param sizeForViewer
     */
    public void openContentViewer(int sizeForViewer) {
        dataListContentSplit.setDividerLocation(dataListContentSplit.getSize().height - sizeForViewer);
    }
    
    static class ComponentFactory {
        private Font normalFont = UIManager.getFont("Label.font").deriveFont(11.0f);
        
        public JToggleButton newToggleButton(String name) {
            JToggleButton button = new JToggleButton(name);
            makeComponent(button, name);
            return button;
        }
        
        public JButton newButton(String name) {
            JButton button = new JButton(name);
            makeComponent(button, name);
            return button;
        }

        public JButton newEPButton(String name) {
            JButton button = (JButton) MacButtonFactory.createGradientButton(null,null);
            makeComponent(button, name);
            return button;
        }
        
        public JPopupMenu createPopupMenu(String name){
            JPopupMenu popup = new JPopupMenu(name);
            makeComponent(popup, name);
            return popup;
        }
        
        public JMenuItem newMenuItem(String name) {
            JMenuItem item = new JMenuItem(name);
            item.setName(name);
            return item;
        }
        
        public void makeComponent(JComponent comp, String name){
            comp.setFont(normalFont);
            comp.setName(name);
        }
        
        public void installPopupButton(JToggleButton toggle, JPopupMenu popupMenu) {
            ActionListener toggleAction = new PopupToggleAction(popupMenu);
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

    // view test
    public static void main(String[] args) {
        GUICheckApp.launch(args, MainView.class);
    }
}
