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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.explodingpixels.macwidgets.ComponentBottomBar;
import com.explodingpixels.macwidgets.MacButtonFactory;
import com.explodingpixels.macwidgets.MacIcons;
import com.explodingpixels.macwidgets.MacWidgetFactory;
import com.explodingpixels.macwidgets.UnifiedToolBar;

public class MainView {
    private final ComponentFactory factory = new ComponentFactory();
    
    public final JTable dataTable = MacWidgetFactory.createITunesTable(null);
    public final JScrollPane dataTableScroll = MacWidgetFactory.createSourceListScrollPane(dataTable);
    public final JTextField quickSearchField = new JTextField(); {
        quickSearchField.setPreferredSize(new Dimension(200, 28));
    }
    
    public final JTree sourceList = new JTree();
    public final JScrollPane sourceListScroll = new JScrollPane(sourceList,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    // Top
    private final UnifiedToolBar toolBarView = new UnifiedToolBar();
    public final JComponent toolBarPane = toolBarView.getComponent(); {
        toolBarView.addComponentToRight(quickSearchField);
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
            sourceListPane, dataTableScroll);

    public final JPanel contentPane = new JPanel(new BorderLayout()); {
        contentPane.add(toolBarPane, "North");
        contentPane.add(sourceListDataTableSplit, "Center");
        contentPane.setPreferredSize(new Dimension(600, 400));
    }

    public MainView() {
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
}
