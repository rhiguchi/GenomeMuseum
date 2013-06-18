package jp.scid.genomemuseum.view;

import static javax.swing.BorderFactory.*;
import static javax.swing.SpringLayout.*;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
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
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultTreeCellEditor;

import jp.scid.genomemuseum.model.TaskProgressModel;
import jp.scid.genomemuseum.view.folder.FolderTreeCellRenderer;
import jp.scid.gui.plaf.SourceListTreeUI;
import jp.scid.gui.view.SDDefaultTableCellRenderer;

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
    
    private final static Icon loadingIcon = new ImageIcon(MainView.class.getResource("loading.gif"));
    
    // Exhibit List Table
    public final ExhibitListView exhibitListView = new ExhibitListView();
    
    public final TaskProgressTableCell taskProgressCell =
            new TaskProgressTableCell(new SDDefaultTableCellRenderer());
    
    // Web Search Table
    private final WebSearchResultListView webSearchResultListView = new WebSearchResultListView();
    public final JTable websearchTable = webSearchResultListView.getTable();
    public final JScrollPane websearchTableScroll = webSearchResultListView.getTableContainer();
    
    private final CardLayout dataListPaneLayout = new CardLayout();
    private final JPanel dataListPane = createDataListPane(
            dataListPaneLayout, exhibitListView.getContainer(), websearchTableScroll);

    // Search Status
    public final JLabel statusLabel = new JLabel("status"); {
        statusLabel.setPreferredSize(new Dimension(200, 28));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    }
    // Loading Icon
    public final JLabel loadingIconLabel = new JLabel(loadingIcon);

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
    public final JComponent toolBarPane = toolBarView.getComponent();
    
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
            dataListPane);

    public final JPanel contentPane = createContentPane(toolBarPane, sourceListDataTableSplit);

    public MainView() {
        PopupToggleHandler addBoxToggleHandler = new PopupToggleHandler(addBoxPopup);
        addBoxToggleHandler.installTo(addBoxButton);
        
        PopupToggleHandler functionToggleHandler = new PopupToggleHandler(boxFunctionPopup);
        functionToggleHandler.installTo(boxFunctionsButton);
        
        toolBarView.addComponentToRight(sequenceSearchField());
    }
    
    public JPanel getContentPane() {
        return contentPane;
    }
    
    public JTable sequenceTable() {
        return exhibitListView.getTable();
    }

    public JTextField sequenceSearchField() {
        return exhibitListView.getSearchField();
    }

    public void setContentsMode(ContentsMode mode) {
        dataListPaneLayout.show(dataListPane, mode.name());
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

    private static JPanel createDataListPane(CardLayout layout, JComponent exhibitListView, JComponent websearchView) {
        JPanel pane = new JPanel(layout);
        pane.add(exhibitListView, ContentsMode.LOCAL.name());
        pane.add(websearchView, ContentsMode.NCBI.name());
        layout.show(pane, ContentsMode.LOCAL.name());
        
        return pane;
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
    
    private static class SampleTableModel extends AbstractTableModel {
        private static enum Column {
            STRING_VALUE(String.class) {
                @Override Object getValue(int row) {
                    return "String Value " + row;
                }
            },
            INTEGER_VALUE(Integer.class) {
                @Override Object getValue(int row) {
                    return 1000 + row;
                }
            },
            REMOTE_SOURCE(TaskProgressModel.class) {
                List<RemoteSourceImpl> sources = new ArrayList<RemoteSourceImpl>(); {
                    for (int i = 0; i < 10; i++) {
                        RemoteSourceImpl data = new RemoteSourceImpl();
                        sources.add(data);
                    }
                }
                
                @Override Object getValue(int row) {
                    return sources.get(row);
                }
            },
            ;
            
            final Class<?> columnClass;

            abstract Object getValue(int row);
            
            private Column(Class<?> columnClass) {
                this.columnClass = columnClass;
            }
        }
        
        public int getRowCount() {
            return 10;
        }

        public int getColumnCount() {
            return Column.values().length;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return Column.values()[column].getValue(row);
        }
        
        @Override
        public Class<?> getColumnClass(int column) {
            return Column.values()[column].columnClass;
        }
        
        @Override
        public String getColumnName(int column) {
            return Column.values()[column].name();
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }
    
    @SuppressWarnings("unused")
    private Action createSampleDownloadingAction(final TaskProgressTableCell editor) {
        return new AbstractAction("Download") {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteSourceImpl remoteSource = (RemoteSourceImpl) editor.getCellEditorValue();
                new RemoteSourceDownloadTask(remoteSource).execute();
            }
        };
    }
    
    private Action createSampleDownloadingAction2(final JTable table) {
        return new AbstractAction("Download") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int editingRow = table.getEditingRow();
                Component editor = table.getEditorComponent();
                System.out.println("row: " + editingRow + " editor: " + editor);
            }
        };
    }

    static class RemoteSourceDownloadTask extends SwingWorker<Void, Void> {
        final RemoteSourceImpl remoteSource;
        AbstractTableModel model;
        
        public RemoteSourceDownloadTask(RemoteSourceImpl remoteSource) {
            this.remoteSource = remoteSource;
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            remoteSource.state = StateValue.STARTED;
            
            int count = 0;
            
            while (count < 500) {
                Thread.sleep(10);
                count++;
                remoteSource.progress = count / 5.0f;
                System.out.println("progress: " + remoteSource.progress);
                publish();
            }
            remoteSource.progress = 100;
            Thread.sleep(1000);
            
            remoteSource.state = StateValue.DONE;
            
            return null;
        }
        
        @Override
        protected void process(List<Void> chunks) {
        }
    };
    
    static class RemoteSourceImpl implements TaskProgressModel {
        boolean available = true;
        float progress = 1;
        String message = "";
        StateValue state = StateValue.PENDING;
        
        @Override
        public StateValue getState() {
            return state;
        }
        
        public float getProgress() {
            return progress;
        }
        
        public String getLabel() {
            return message;
        }
        
        public boolean isAvailable() {
            return available;
        }
    }
}
