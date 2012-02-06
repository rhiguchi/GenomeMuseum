package jp.scid.genomemuseum.controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.Comparator;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import jp.scid.genomemuseum.controller.BioFileSequenceLoader.BioFileSource;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.view.ExhibitListView;
import jp.scid.gui.control.EventListController;
import jp.scid.gui.control.UriDocumentLoader;
import jp.scid.gui.control.ViewValueConnector;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;

public class MuseumExhibitController extends EventListController<MuseumExhibit> {
    static enum DataViewMode {
        CONTENTS, MOTIFVIEW;
    }
    
    // Controllers
    private final OverviewMotifListController overviewMotifListController =
        new OverviewMotifListController();
    
    private final MuseumExhibitTableFormat tableFormat = new MuseumExhibitTableFormat();
    
    private final ContentViewStateListener contentViewStateListener = new ContentViewStateListener();
    
    private final UriDocumentLoader documentLoader = new UriDocumentLoader();
    
    private final BioFileSequenceLoader documentSourceController = new BioFileSequenceLoader();
    
    // Models
    private final ValueModel<Boolean> dataViewVisibled = ValueModels.newBooleanModel(false);
    
    private final ValueModel<DataViewMode> dataViewMode = ValueModels.newValueModel(DataViewMode.CONTENTS); 
    
    private final ValueModel<URI> selectedUri = ValueModels.newNullableValueModel();
    
    private final ValueModel<BioFileSource> bioFileSource = ValueModels.newNullableValueModel();
    
    // Binding
    
    public MuseumExhibitController() {
        // TODO selection
//        documentSourceController.listenTo(getSelectionModel().getSelected());
        
        dataViewVisibled.addPropertyChangeListener(contentViewStateListener);
        dataViewMode.addPropertyChangeListener(contentViewStateListener);
        
        documentLoader.setModel(selectedUri);
        
        overviewMotifListController.setModel(documentSourceController.getSequence());
        documentSourceController.setModel(bioFileSource);
    }
    
    public void bind(ExhibitListView view) {
        overviewMotifListController.bind(view.overviewMotifView);
        
        bindTable(view.dataTable, tableFormat);
        bindDataViewsTab(view.contentsViewTabbedPane);
        bindDataViewSplit(view.dataListContentSplit);
        
        documentLoader.bindTextComponent(view.getContentViewComponent());
    }
    
    public void bindDataViewsTab(JTabbedPane tabbedPane) {
        TabComponentSelector selector = new TabComponentSelector(tabbedPane);
        tabbedPane.setSelectedIndex(dataViewMode.getValue().ordinal());
        selector.setModel(dataViewMode);
        tabbedPane.getModel().addChangeListener(selector);
    }
    
    public void bindDataViewSplit(JSplitPane pane) {
        
    }
    
    void updateContents() {
        EventList<MuseumExhibit> selected = getSelectionModel().getSelected();
        if (selected.isEmpty()) {
            return;
        }
        
    }
    
    class ContentViewStateListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
//            updateContents();
        }
    }
    
    class TabComponentSelector extends ViewValueConnector<JTabbedPane, DataViewMode> implements ChangeListener {
        public TabComponentSelector(JTabbedPane targetView) {
            super(targetView);
        }
        
        @Override
        protected void updateView(JTabbedPane target, DataViewMode modelValue) {
            target.setSelectedIndex(modelValue.ordinal());
        }
        
        void updateModelFromView() {
            int selectedIndex = getView().getSelectedIndex();
            DataViewMode newMode = DataViewMode.values()[selectedIndex];
            
            getModel().setValue(newMode);
        }
        
        @Override
        public void stateChanged(ChangeEvent e) {
            updateModelFromView();
        }
    }
}

class MuseumExhibitTableFormat implements AdvancedTableFormat<MuseumExhibit> {
    static enum Column {
        NAME("name") {
            @Override
            Object getValue(MuseumExhibit e) {
                return e.id();
            }
        },
//        SEQUENCE_LENGTH,
//        ACCESSION,
//        IDENTIFIER,
//        NAMESPACE,
//        VERSION,
//        DEFINITION,
//        SOURCE,
//        ORGANISM,
//        DATE,
        ;
        
        private final String columnName;
        private final Class<?> columnClass;
        
        private Column(String columnName, Class<?> columnClass) {
            this.columnName = columnName;
            this.columnClass = columnClass;
        }
        
        private Column(String columnName) {
            this(columnName, Object.class);
        }

        public String getColumnName() {
            return columnName;
        }
        
        abstract Object getValue(MuseumExhibit e);
    }

    Column getColumn(int index) {
        return Column.values()[index];
    }
    
    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return getColumn(column).name();
    }

    @Override
    public Object getColumnValue(MuseumExhibit baseObject, int column) {
        return getColumn(column).getValue(baseObject);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return getColumn(column).columnClass;
    }

    @Override
    public Comparator<MuseumExhibit> getColumnComparator(int column) {
        return null;
    }
}