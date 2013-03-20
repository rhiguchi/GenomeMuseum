package jp.scid.genomemuseum.gui;

import static jp.scid.bio.store.jooq.Tables.*;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import jp.scid.bio.store.jooq.tables.records.GeneticSequenceRecord;
import jp.scid.genomemuseum.model.GeneticSequenceCollection;
import jp.scid.genomemuseum.model.GeneticSequenceLibrary;
import jp.scid.genomemuseum.model.MutableGeneticSequenceCollection;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.jooq.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.AdvancedTableFormat;

public class GeneticSequenceListController extends ListController<GeneticSequenceRecord> {
    private final static Logger logger = LoggerFactory.getLogger(GeneticSequenceListController.class);
    
    private final ModelChangeListener modelChangeListener = new ModelChangeListener();
    
    private FileDialog fileDialog = null;
    
    private GeneticSequenceCollection model;
    
    public GeneticSequenceListController() {
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
    public boolean importFromFile(File source) {
        logger.debug("add file: %s", source);
        MutableGeneticSequenceCollection model = mutableModel();
        
        FileImportTask task = new FileImportTask(model, source);
        task.execute();
        
        return true;
    }
    
    public void addFile() {
        FileDialog dialog = getFileDialog();
        
        dialog.setVisible(true);
        
        if (dialog.getFile() == null) {
            return;
        }
        
        File file = new File(dialog.getDirectory(), dialog.getFile());
        importFromFile(file);
    }
    
    @Override
    protected Action createAddAction() {
        return new AbstractAction("add") {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFile();
            }
        };
    }
    
    @Override
    public List<GeneticSequenceRecord> remove() {
        MutableGeneticSequenceCollection model = mutableModel();
        List<GeneticSequenceRecord> list;
        
        if (model instanceof GeneticSequenceLibrary) {
            ((GeneticSequenceLibrary) model).remove(list = super.remove(), true);
        }
        else {
            model.remove(list = super.remove());
        }
        
        return list;
    }
    
    @Override
    protected File getFile(GeneticSequenceRecord element) {
        return model.getFilePath(element);
    }
    
    // model
    public GeneticSequenceCollection getModel() {
        return model;
    }

    private MutableGeneticSequenceCollection mutableModel() {
        return (MutableGeneticSequenceCollection) this.model;
    }
    
    public void setModel(GeneticSequenceCollection newModel) {
        if (model != null) {
            model.removeListDataListener(modelChangeListener);
        }
        
        this.model = newModel;
        
        clear();
        
        if (newModel != null) {
            setCanAdd(newModel instanceof MutableGeneticSequenceCollection);
            setCanRemove(newModel instanceof MutableGeneticSequenceCollection);
            
            newModel.addListDataListener(modelChangeListener);
            addAll(newModel.fetch());
        }
    }
    
    @Override
    protected GeneticSequenceRecord createElement() {
        return mutableModel().newElement();
    }
    
    @Override
    protected ExhibitTextFilterator createTextFilterator() {
        return new ExhibitTextFilterator();
    }
    
    /**
     * fetch entities
     */
    public void fetch() {
        fetch(false);
    }
    
    public void fetch(boolean updates) {
        if (model == null) {
            clear();
        }
        else {
            GlazedLists.replaceAll(source, model.fetch(), updates);
        }
    }

    private class ModelChangeListener implements ListDataListener {
        @Override
        public void intervalAdded(ListDataEvent e) {
            fetch();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            fetch();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            fetch(true);
        }
    }
    
    protected static class Binding extends ListController.Binding<GeneticSequenceRecord> {
        final GeneticSequenceListController controller;
        final GeneticSequenceTableFormat tableFormat = new GeneticSequenceTableFormat();
        
        public Binding(GeneticSequenceListController controller) {
            super(controller);
            this.controller = controller;
        }

        public void bindTable(JTable table) {
            bindTable(table, tableFormat);
            bindTableTransferHandler(table);
            bindSortableTableHeader(table.getTableHeader(), tableFormat);
        }
    }
    
    static class GeneticSequenceTableFormat implements AdvancedTableFormat<GeneticSequenceRecord> {
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
        public Object getColumnValue(GeneticSequenceRecord baseObject, int column) {
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

    private static class ExhibitTextFilterator implements TextFilterator<GeneticSequenceRecord> {
        @Override
        public void getFilterStrings(List<String> baseList, GeneticSequenceRecord element) {
            baseList.add(element.getAccession());
            baseList.add(element.getDefinition());
            baseList.add(element.getName());
            baseList.add(element.getNamespace());
            baseList.add(element.getOrganism());
            baseList.add(element.getSource());
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
