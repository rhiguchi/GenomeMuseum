package jp.scid.genomemuseum.view;

import static javax.swing.BorderFactory.*;
import static javax.swing.SpringLayout.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
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
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellEditor;

import jp.scid.genomemuseum.model.TaskProgressModel;
import jp.scid.gui.MessageFormatTableCell;
import jp.scid.gui.plaf.SourceListTreeUI;

import com.explodingpixels.macwidgets.ComponentBottomBar;
import com.explodingpixels.macwidgets.MacButtonFactory;
import com.explodingpixels.macwidgets.MacIcons;
import com.explodingpixels.macwidgets.MacWidgetFactory;
import com.explodingpixels.macwidgets.UnifiedToolBar;
import com.explodingpixels.macwidgets.plaf.ITunesTableUI;

public class MainView implements GenomeMuseumView {
    private static final Color COLOR_ACTIVITY_FOREGROUND = new Color(0f, 0f, 0f, 0.8f);
    
    private final ComponentFactory factory = new ComponentFactory();
    private final static Icon loadingIcon = new ImageIcon(MainView.class.getResource("loading.gif"));
    
    // Data table
    public final JTable dataTable = new JTable(); {
        dataTable.setUI(new ITunesTableUI());
        TableCellRenderer defaultRenderer = dataTable.getDefaultRenderer(Object.class);
        
        // Taskable
        TaskProgressTableCell remoteResourceCell = new TaskProgressTableCell(defaultRenderer);
//        Action dlAction = createSampleDownloadingAction(remoteResourceCell);
        Action dlAction = createSampleDownloadingAction2(dataTable);
        remoteResourceCell.setExecuteButtonAction(dlAction);
        dataTable.setDefaultRenderer(TaskProgressModel.class, remoteResourceCell);
        dataTable.setDefaultEditor(TaskProgressModel.class, remoteResourceCell);
        
        // Integer
        MessageFormatTableCell intValueCell = new MessageFormatTableCell(new DecimalFormat("#,##0"), defaultRenderer);
        intValueCell.getRendererView().setHorizontalAlignment(SwingConstants.RIGHT);
        dataTable.setDefaultRenderer(Integer.class, intValueCell);
        
        dataTable.setModel(new SampleTableModel());
    }
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

    // Source List Area
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
    
    /** EAST Area */
    public final JComponent eastAreaPane = new JPanel(new BorderLayout()); {
        JComponent parent = eastAreaPane;
        parent.setOpaque(false);
        
        parent.add(sourceListScroll, "Center");
        parent.add(activityPane, "South");
    }
    
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
        sourceListPane.add(eastAreaPane, "Center");
        sourceListPane.add(fComponentBottomBar.getComponent(), "South");
    }
    public final JSplitPane sourceListDataTableSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, sourceListPane,
            dataListContentSplit);
    {
        sourceListDataTableSplit.setBorder(null);
    }

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
