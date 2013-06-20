package jp.scid.genomemuseum.gui;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.genomemuseum.model.GeneticSequenceCollection;
import jp.scid.genomemuseum.model.GeneticSequenceCollections;
import jp.scid.genomemuseum.model.GeneticSequenceFileLoadingManager.LoadingSuccessHandler;
import jp.scid.genomemuseum.model.GeneticSequenceTableFormat;
import jp.scid.genomemuseum.model.MutableGeneticSequenceCollection;
import jp.scid.genomemuseum.model.SequenceImportable;
import jp.scid.gui.control.BooleanModelBindings;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.DefaultEventListModel;

public class GeneticSequenceListController extends ListController<GeneticSequence> {
    private final static Logger logger = LoggerFactory.getLogger(GeneticSequenceListController.class);
    
    private final GeneticSequenceListTransferHandler transferHandler;
    
    private GeneticSequenceCollection model;
    
    private FileLoadingTaskController taskController;

    private ValueModel<?> selectedSource;
    
    private final ChangeListener selectedSourceModelHandler = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            tryUpdateModel();
        }
    };
    
    public GeneticSequenceListController(EventList<GeneticSequence> source) {
        super(source);
        transferHandler = new GeneticSequenceListTransferHandler(this);
        
        ListModel selection = new DefaultEventListModel<GeneticSequence>(selectionModel.getSelected());
        ValueModel<Boolean> isNonEmpty = ValueModels.newListElementsExistenceModel(selection);
        new BooleanModelBindings(isNonEmpty).bindToActionEnabled(removeAction);
    }
    
    public GeneticSequenceListController() {
        this(new BasicEventList<GeneticSequence>());
    }
    
    @Override
    protected List<GeneticSequence> retrieve() {
        if (model == null) {
            return super.retrieve();
        }
        return model.fetchSequences();
    }
    
    // addFile
    public boolean canImportFile() {
        return model instanceof SequenceImportable;
    }
    
    @Override
    public boolean importFile(File source) throws IOException {
        logger.debug("importFile: {}", source);
        
        SequenceImportable model = (SequenceImportable) this.model;
        
        // TODO use import asynchronous queue
        try {
            model.importSequence(source);
        }
        catch (ParseException e) {
            logger.info("cannot parse to import: {}", e.getLocalizedMessage());
            return false;
        }
        
        return true;
    }
    
    public void importFiles(Collection<File> files) {
        if (taskController != null) {
            SequenceImportable dest = (SequenceImportable) this.model;
            ImportSuccessHandler handler = new ImportSuccessHandler(dest);
            
            taskController.executeLoading(files, dest, handler);
        }
        else try {
            for (File file: files) {
                importFile(file);
            }
        }
        catch (IOException e) {
            logger.error("fail to import sequence file", e);
        }
    }
    
    class ImportSuccessHandler implements LoadingSuccessHandler {
        private final SequenceImportable model;
        
        public ImportSuccessHandler(SequenceImportable model) {
            this.model = model;
        }

        @Override
        public void handle(GeneticSequence newElement) {
            if (model.equals(getModel())) {
                add(newElement);
            }
        }
    }
    
    @Override
    public boolean canRemove() {
        return model instanceof SequenceImportable;
    }

    @Override
    public void remove() {
        for (GeneticSequence sequence: selectionModel.getSelected()) {
            sequence.delete();
        }
        fetch();
    }
    
    public void setFileLoadingTaskController(FileLoadingTaskController taskController) {
        this.taskController = taskController;
    }
    
    // model
    public GeneticSequenceCollection getModel() {
        return model;
    }

    private MutableGeneticSequenceCollection mutableModel() {
        return (MutableGeneticSequenceCollection) this.model;
    }
    
    public void setModel(GeneticSequenceCollection newModel) {
        logger.debug("set list model: {}", newModel);
        this.model = newModel;
        
        fetch();
    }

    private void tryUpdateModel() {
        if (selectedSource == null) {
            return;
        }
        Object object = selectedSource.get(); 
        final GeneticSequenceCollection newModel;
        if (object instanceof SequenceLibrary) {
            newModel = GeneticSequenceCollections.fromSequenceLibrary((SequenceLibrary) object);
        }
        else {
            newModel = null;
        }
        setModel(newModel);
    }
    
    @Override
    protected ExhibitTextFilterator createTextFilterator() {
        return new ExhibitTextFilterator();
    }
    
    @Override
    public GeneticSequenceListTransferHandler getTransferHandler() {
        return transferHandler;
    }
    
    public void setModelHolder(ValueModel<?> holder) {
        if (selectedSource != null) {
            selectedSource.removeValueChangeListener(selectedSourceModelHandler);
        }
        selectedSource = holder;
        
        if (holder != null) {
            holder.addValueChangeListener(selectedSourceModelHandler);
        }
        tryUpdateModel();
    }

    protected class Binding extends ListController<GeneticSequence>.Binding {
        final GeneticSequenceTableFormat tableFormat = new GeneticSequenceTableFormat();

        public Binding() {
        }

        public void bindTable(JTable table) {
            bindTable(table, tableFormat);
            bindTableTransferHandler(table);
            bindSortableTableHeader(table.getTableHeader(), tableFormat);
        }
    }
    
    private static class ExhibitTextFilterator implements TextFilterator<GeneticSequence> {
        @Override
        public void getFilterStrings(List<String> baseList, GeneticSequence element) {
            baseList.add(element.accession());
            baseList.add(element.definition());
            baseList.add(element.name());
            baseList.add(element.organism());
            baseList.add(element.source());
        }
    }
}
