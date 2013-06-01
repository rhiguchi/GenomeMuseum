package jp.scid.genomemuseum.gui;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.SwingWorker;

import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.genomemuseum.model.GeneticSequenceCollection;
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
    
    private final ListModelEventListAdapter<GeneticSequence> listSource;
    
    private GeneticSequenceCollection model;
    
    public GeneticSequenceListController(EventList<GeneticSequence> source) {
        super(source);
        listSource = ListModelEventListAdapter.newInstanceOf(GeneticSequence.class, source);
        
        transferHandler = new GeneticSequenceListTransferHandler(this);
        
        ListModel selection = new DefaultEventListModel<GeneticSequence>(selectionModel.getSelected());
        ValueModel<Boolean> isNonEmpty = ValueModels.newListElementsExistenceModel(selection);
        new BooleanModelBindings(isNonEmpty).bindToActionEnabled(removeAction);
    }
    
    public GeneticSequenceListController() {
        this(new BasicEventList<GeneticSequence>());
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
        
        
//        FileImportTask task = new FileImportTask(model, source);
//        task.execute();
        
        return true;
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
        model.fetchSequences();
    }
    
    // model
    public GeneticSequenceCollection getModel() {
        return model;
    }

    private MutableGeneticSequenceCollection mutableModel() {
        return (MutableGeneticSequenceCollection) this.model;
    }
    
    public void setModel(GeneticSequenceCollection newModel) {
        this.model = newModel;
        
        ListModel collection = newModel == null ? null : newModel.getCollection();
        listSource.setSource(collection);
    }
    
    @Override
    protected ExhibitTextFilterator createTextFilterator() {
        return new ExhibitTextFilterator();
    }
    
    @Override
    public GeneticSequenceListTransferHandler getTransferHandler() {
        return transferHandler;
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

    static class FileImportTask extends SwingWorker<Result, Void> {
        private final File file;
        private final MutableGeneticSequenceCollection model;
        
        public FileImportTask(MutableGeneticSequenceCollection model, File file) {
            this.model = model;
            this.file = file;
        }

        @Override
        protected Result doInBackground() throws Exception {
//            model.addFile(file);
            return null;
        }
    }
    
    static abstract class Result {
        private final GeneticSequence sequence;
        
        Result(GeneticSequence sequence) {
            this.sequence = sequence;
        }
        
        public GeneticSequence sequence() {
            return sequence;
        }
    }
    
    static class Success extends Result {
        Success(GeneticSequence sequence) {
            super(sequence);
        }
    }
    
    static class InvalidFileFormat extends Result {
        InvalidFileFormat(GeneticSequence sequence) {
            super(sequence);
        }
    }
}
