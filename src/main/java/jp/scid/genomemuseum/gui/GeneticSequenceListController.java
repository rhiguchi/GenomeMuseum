package jp.scid.genomemuseum.gui;

import static jp.scid.bio.store.jooq.Tables.*;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Comparator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.SwingWorker;

import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.genomemuseum.model.GeneticSequenceCollection;
import jp.scid.genomemuseum.model.MutableGeneticSequenceCollection;
import jp.scid.genomemuseum.model.SequenceImportable;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.jooq.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.AdvancedTableFormat;

public class GeneticSequenceListController extends ListController<GeneticSequence> {
    private final static Logger logger = LoggerFactory.getLogger(GeneticSequenceListController.class);
    
    private final GeneticSequenceListTransferHandler transferHandler;
    
    private final ListModelEventListAdapter<GeneticSequence> listSource;
    
    private FileDialog fileDialog = null;
    
    private GeneticSequenceCollection model;
    
    public GeneticSequenceListController(EventList<GeneticSequence> source) {
        super(source);
        listSource = ListModelEventListAdapter.newInstanceOf(GeneticSequence.class, source);
        
        transferHandler = new GeneticSequenceListTransferHandler(this);
    }
    
    public GeneticSequenceListController() {
        this(new BasicEventList<GeneticSequence>());
    }
    
    public FileDialog getFileDialog() {
        if (fileDialog == null) {
            fileDialog = new FileDialog((Frame) null);
        }
        return fileDialog;
    }
    
    public void setFileDialog(FileDialog fileDialog) {
        this.fileDialog = fileDialog;
    }
    
    @Override
    public boolean canRemove() {
        return model instanceof MutableGeneticSequenceCollection;
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
    
//    public void addFile() {
//        FileDialog dialog = getFileDialog();
//        
//        dialog.setVisible(true);
//        
//        if (dialog.getFile() == null) {
//            return;
//        }
//        
//        File file = new File(dialog.getDirectory(), dialog.getFile());
//        importFile(file);
//    }
    
//    @Override
//    public List<GeneticSequence> remove() {
//        MutableGeneticSequenceCollection model = mutableModel();
//        List<GeneticSequence> list;
//        
//        if (model instanceof GeneticSequenceLibrary) {
//            ((GeneticSequenceLibrary) model).remove(list = super.remove(), true);
//        }
//        else {
//            model.remove(list = super.remove());
//        }
//        
//        return list;
//    }
    
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
    
    static class GeneticSequenceTableFormat implements AdvancedTableFormat<GeneticSequence> {
        List<Field<?>> fields = GENETIC_SEQUENCE.getFields();
                
        @Override
        public int getColumnCount() {
            return fields.size();
        }

        @Override
        public String getColumnName(int column) {
            Field<?> field = fields.get(column);
            return field.getName();
        }

        @Override
        public Object getColumnValue(GeneticSequence baseObject, int column) {
            Field<?> field = fields.get(column);
            return baseObject.getValue(field);
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return fields.get(column).getType();
        }

        @Override
        public Comparator<?> getColumnComparator(int column) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    private static class ExhibitTextFilterator implements TextFilterator<GeneticSequence> {
        @Override
        public void getFilterStrings(List<String> baseList, GeneticSequence element) {
//            baseList.add(element.getAccession());
//            baseList.add(element.getDefinition());
//            baseList.add(element.getName());
//            baseList.add(element.getNamespace());
//            baseList.add(element.getOrganism());
//            baseList.add(element.getSource());
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
            model.addFile(file);
            return null;
        }
    }
    
    static abstract class Result {
        private final MuseumExhibitRecord exhibit;
        
        Result(MuseumExhibitRecord exhibit) {
            this.exhibit = exhibit;
        }
        
        public MuseumExhibitRecord exhibit() {
            return exhibit;
        }
    }
    
    static class Success extends Result {
        Success(MuseumExhibitRecord exhibit) {
            super(exhibit);
        }
    }
    
    static class InvalidFileFormat extends Result {
        InvalidFileFormat(MuseumExhibitRecord exhibit) {
            super(exhibit);
        }
    }
}
