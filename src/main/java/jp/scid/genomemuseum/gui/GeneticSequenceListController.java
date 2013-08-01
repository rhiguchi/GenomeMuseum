package jp.scid.genomemuseum.gui;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jp.scid.bio.store.sequence.FolderContentGeneticSequence;
import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.bio.store.sequence.GeneticSequenceSource;
import jp.scid.bio.store.sequence.ImportableSequenceSource;
import jp.scid.genomemuseum.model.GeneticSequenceTableFormat;
import jp.scid.gui.control.BooleanModelBindings;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;
import jp.scid.gui.model.connector.ValueConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventListModel;

public class GeneticSequenceListController extends ListController<GeneticSequence> {
    private final static Logger logger = LoggerFactory.getLogger(GeneticSequenceListController.class);
    private final static ResourceBundle resource =
            ResourceBundle.getBundle(GeneticSequenceListController.class.getName());
    
    private final GeneticSequenceListTransferHandler transferHandler;
    
    private GeneticSequenceSource model;
    
    private FileLoadingTaskController taskController;

    private final ValueModel<GeneticSequence> selectedElement;

    private final ChangeListener sequenceSourceChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            fetch();
        }
    };
    
    public GeneticSequenceListController(EventList<GeneticSequence> source) {
        super(source);
        transferHandler = new GeneticSequenceListTransferHandler(this);
        
        ListModel selection = new DefaultEventListModel<GeneticSequence>(selectionModel.getSelected());
        ValueModel<Boolean> isNonEmpty = ValueModels.newListElementsExistenceModel(selection);
        new BooleanModelBindings(isNonEmpty).bindToActionEnabled(removeAction);
        
        EventListSingleSelectAdapter<GeneticSequence> selectAdapter = new EventListSingleSelectAdapter<GeneticSequence>();
        selectAdapter.setSource(selectionModel.getSelected());
        selectedElement = selectAdapter.getTargetModel();
    }
    
    public GeneticSequenceListController() {
        this(new BasicEventList<GeneticSequence>());
    }
    
    public ValueModel<GeneticSequence> getSelectedGeneticSequence() {
        return selectedElement;
    }
    
    @Override
    protected List<GeneticSequence> retrieve() {
        if (model == null) {
            return super.retrieve();
        }
        return new ArrayList<GeneticSequence>(model.getGeneticSequences());
    }
    
    // addFile
    public boolean canImportFile() {
        return model instanceof ImportableSequenceSource;
    }
    
    @Override
    public boolean importFile(File source) throws IOException {
        logger.debug("importFile: {}", source);
        
        ImportableSequenceSource model = (ImportableSequenceSource) this.model;
        
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
        ImportableSequenceSource dest = (ImportableSequenceSource) this.model;
        if (taskController != null) {
            taskController.executeLoading(files, dest);
        }
        else try {
            for (File file: files) {
                dest.importSequence(file);
            }
        }
        catch (IOException e) {
            logger.error("fail to import sequence file", e);
        }
        catch (ParseException e) {
            logger.warn("fail to import sequence file", e);
        }
    }
    
    @Override
    public List<GeneticSequence> remove() {
        List<GeneticSequence> selections = selectionModel.getSelected();
        boolean removeLibfile = false;
        
        if (containsLibraryFile(selections)) {
            int result = askLibraryFileRemove();
            if (result == JOptionPane.CANCEL_OPTION) {
                return Collections.emptyList();
            }
            removeLibfile = result == JOptionPane.OK_OPTION;
        }
        
        for (GeneticSequence sequence: selections) {
            if (removeLibfile) {
                sequence.deleteFileFromLibrary();
            }
            sequence.delete();
        }
        selections.clear();
        return selections;
    }
    
    private static int askLibraryFileRemove() {
        String message = resource.getString("remove.alert.Message");
        String[] options = new String[] {
            resource.getString("remove.alert.OptionOk"),
            resource.getString("remove.alert.OptionNo"),
            resource.getString("remove.alert.OptionCancel"),
        };
        int result = JOptionPane.showOptionDialog(null, message, null,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return result;
    }
    
    private static boolean containsLibraryFile(List<GeneticSequence> list) {
        for (GeneticSequence s: list) {
            if (s instanceof FolderContentGeneticSequence)
                return false;
            if (s.isFileStoredInLibray())
                return true;
        }
        return false;
    }
    
    public void setFileLoadingTaskController(FileLoadingTaskController taskController) {
        this.taskController = taskController;
    }
    
    // model
    public GeneticSequenceSource getModel() {
        return model;
    }

    public void setModel(GeneticSequenceSource newModel) {
        logger.debug("set list model: {}", newModel);
        
        if (this.model != null) {
            this.model.removeSequencesChangeListener(sequenceSourceChangeListener);
        }
        
        this.model = newModel;
        
        if (newModel != null) {
            newModel.addSequencesChangeListener(sequenceSourceChangeListener);
        }
        
        fetch();
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
            baseList.add(element.moleculeType());
            baseList.add(element.name());
            baseList.add(element.namespace());
            baseList.add(element.organism());
            baseList.add(element.source());
        }
    }
    
    static class EventListSingleSelectAdapter<E> extends ValueConnector<E, EventList<E>> implements ListEventListener<E> {
        @Override
        public void listChanged(ListEvent<E> listChanges) {
            this.sourceChange(listChanges.getSourceList());
        }

        @Override
        protected E getModelValue(EventList<E> source) {
            if (source.size() != 1) {
                return null;
            }
            return source.get(0);
        }

        @Override
        protected void installUpdateListener(EventList<E> source) {
            source.addListEventListener(this);
        }

        @Override
        protected void uninstallUpdateListener(EventList<E> source) {
            source.removeListEventListener(this);
        }
    }
}
