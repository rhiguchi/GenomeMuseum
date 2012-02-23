package jp.scid.genomemuseum.controller;

import jp.scid.genomemuseum.gui.ExhibitTableFormat;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Comparator;
import java.util.List;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import jp.scid.genomemuseum.controller.BioFileSequenceLoader.BioFileSource;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumExhibit$;
import jp.scid.genomemuseum.model.MuseumExhibitListModel;
import jp.scid.genomemuseum.view.ExhibitListView;
import jp.scid.motifviewer.gui.MotifViewerController;
import jp.scid.motifviewer.gui.OverviewMotifListView;
import jp.scid.gui.control.EventListController;
import jp.scid.gui.control.ListHandler;
import jp.scid.gui.control.StringPropertyBinder;
import jp.scid.gui.control.TextMatcherEditorRefilterator;
import jp.scid.gui.control.UriDocumentLoader;
import jp.scid.gui.control.ViewValueConnector;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.matchers.MatcherEditor;

public class MuseumExhibitController extends EventListController<MuseumExhibit, MuseumExhibitListModel> {
    static enum DataViewMode {
        CONTENTS, MOTIFVIEW;
    }
    
    // Controllers
    private final MotifViewerController motifViewerController =
        new MotifViewerController();
    
    private final ExhibitTableFormat tableFormat = new ExhibitTableFormat();
    
    private final ContentViewStateListener contentViewStateListener = new ContentViewStateListener();
    
    private final SelectionChangeHandler selectionChangeHandler = new SelectionChangeHandler();
    
    private final UriDocumentLoader documentLoader = new UriDocumentLoader();
    
    private final BioFileSequenceLoader documentSourceController = new BioFileSequenceLoader();
    
    private final StringPropertyBinder searchFieldBinder = new StringPropertyBinder();
    
    private final TableFilterator filterator = new TableFilterator();
    
    private final TextMatcherEditorRefilterator tableRefilterator =
            new TextMatcherEditorRefilterator(filterator);
    
    // Models
    private final ValueModel<Boolean> dataViewVisibled = ValueModels.newBooleanModel(false);
    
    private final ValueModel<DataViewMode> dataViewMode = ValueModels.newValueModel(DataViewMode.CONTENTS); 
    
    private final ValueModel<URI> selectedUri = ValueModels.newNullableValueModel();
    
    private final ValueModel<BioFileSource> bioFileSource = ValueModels.newNullableValueModel();
    
    private final ValueModel<String> title = ValueModels.newValueModel("");
    
    // Binding
    public MuseumExhibitController() {
        selectionChangeHandler.setModel(getSelectionModel().getSelected());
        
        dataViewVisibled.addPropertyChangeListener(contentViewStateListener);
        dataViewMode.addPropertyChangeListener(contentViewStateListener);
        
        documentLoader.setModel(selectedUri);
        
        motifViewerController.setModel(documentSourceController.getSequence());
        documentSourceController.setModel(bioFileSource);
        
        // refilter
        tableRefilterator.setModel(getFilterTextModel());
        setMatcherEditor((MatcherEditor<? super MuseumExhibit>) tableRefilterator.getTextMatcherEditor());
        
        setTableFormat(tableFormat);
    }
    
    public void bind(ExhibitListView view) {
        motifViewerController.bind(view.overviewMotifView);
        
        bindTable(view.dataTable);
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
    
    public ValueModel<String> getTitleModel() {
        return title;
    }
    
    public ValueModel<String> getFilterTextModel() {
        return searchFieldBinder.getValueModel();
    }
    
    void updateContents() {
        EventList<MuseumExhibit> selected = getSelectionModel().getSelected();
        if (selected.isEmpty()) {
            return;
        }
        
    }
    
    class SelectionChangeHandler extends ListHandler<MuseumExhibit> {
        @Override
        protected void processValueChange(List<MuseumExhibit> paramList) {
            final URI uri;
            final String titleText;
            final BioFileSource newBioFileSource;
            
            if (paramList.isEmpty()) {
                uri = null;
                titleText = "";
                newBioFileSource = null;
            }
            else {
                uri = paramList.get(0).filePathAsURI();
                titleText = uri.toString();
                newBioFileSource = new SimpleBioFileSource(paramList.get(0));
            }
            
            title.setValue(titleText);
            selectedUri.setValue(uri);
            bioFileSource.setValue(newBioFileSource);
        }
    }
    
    static class SimpleBioFileSource implements BioFileSource {
        private final MuseumExhibit exhibit; 
        
        public SimpleBioFileSource(MuseumExhibit exhibit) {
            this.exhibit = exhibit;
        }

        @Override
        public Reader getReader() {
            Reader reader = new StringReader("");
            try {
                reader = new InputStreamReader(exhibit.filePathAsURI().toURL().openStream());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return reader;
        }

        @Override
        public BioFileFormat getBioFileFormat() {
            if (exhibit.fileType().id() == 1) {
                return BioFileFormat.GEN_BANK;
            }
            else if (exhibit.fileType().id() == 2) {
                return BioFileFormat.FASTA;
            }
            return null;
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
    
    class TableFilterator implements TextFilterator<MuseumExhibit> {
        @Override
        public void getFilterStrings(List<String> baseList, MuseumExhibit element) {
            for (int i = tableFormat.getColumnCount() - 1; i >= 0; i--) {
                Object value = tableFormat.getColumnValue(element, i);
                baseList.add(value.toString());
            }
        }
    }
}
