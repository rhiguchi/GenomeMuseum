package jp.scid.genomemuseum.gui;

import static jp.scid.bio.store.jooq.Tables.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingWorker;

import jp.scid.bio.store.FileLibrary;
import jp.scid.bio.store.GeneticSequenceParser;
import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.jooq.tables.records.GeneticSequenceRecord;
import jp.scid.genomemuseum.model.MuseumExhibit.FileType;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.jooq.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.AdvancedTableFormat;

public class GeneticSequenceListController extends ListController<GeneticSequenceRecord> {
    private final static Logger logger = LoggerFactory.getLogger(GeneticSequenceListController.class);
    
    private GeneticSequenceParser geneticSequenceparser;
    GeneticSequenceCollection model;
    
    FileLibrary fileLibrary = null;
    
    public GeneticSequenceListController() {
        geneticSequenceparser = new GeneticSequenceParser();
    }
    
    @Override
    public boolean importFromFile(File source) {
        logger.debug("add file: %s", source);
        MutableGeneticSequenceCollection model = (MutableGeneticSequenceCollection) this.model;
        boolean result = false;
        
        try {
            for (File file: listFiles(source)) {
                List<GeneticSequenceRecord> records;
                
                try {
                    records = geneticSequenceparser.parse(file);
                }
                catch (ParseException e) {
                    logger.info("file {} is invalid genetic sequence", file, e);
                    continue;
                }
                
                if (records.isEmpty()) {
                    logger.info("file {} is not a genetic sequence", file);
                    continue;
                }
                
                addAll(records);
                
                // store to local storage
                File path = storeGeneticSequenceFile(file, records.get(0));
                for (GeneticSequenceRecord record: records) {
                    record.setFileUri(path.toString());
                    elementChanged(record);
                }
                
                model.add(records);
                result = true;
            }
        }
        catch (IOException e) {
            logger.error("fail to read gene file", e);
        }
        
        return result;
    }
    
    @Override
    public boolean canRemove() {
        return super.canRemove() && model instanceof MutableGeneticSequenceCollection;
    }
    
    @Override
    public List<GeneticSequenceRecord> remove() {
        MutableGeneticSequenceCollection model = (MutableGeneticSequenceCollection) this.model;
        boolean removeLibraryFile = false;
        
        if (model instanceof GeneticSequenceLibrary) {
            removeLibraryFile = true;
        }
        
        List<GeneticSequenceRecord> list = super.remove();
        for (GeneticSequenceRecord record: list) {
            model.remove(record);
            
            // file deletion
            if (!removeLibraryFile) {
                continue;
            }
            
            
            File file = getFile(record);
            if (file == null) {
                continue;
            }
            
            if (file.delete()) {
                logger.info("delete file %s", file);
            }
        }
        
        return list;
    }
    
    public void setFileLibrary(FileLibrary fileLibrary) {
        this.fileLibrary = fileLibrary;
    }
    
    private File storeGeneticSequenceFile(File file, GeneticSequenceRecord record) throws IOException {
        File dest = fileLibrary.storeFile(file, record);
        logger.info("copy file {} to {}", file, dest);
        
        File relativePath = fileLibrary.convertLibraryRelativePath(dest);
        return relativePath;
    }
    
    @Override
    protected File getFile(GeneticSequenceRecord element) {
        String uriString = element.getFileUri();
        if (uriString == null) {
            return null;
        }

        File file = new File(uriString);
        if (fileLibrary == null || file.isAbsolute()) {
            return file;
        }

        file = fileLibrary.convertLibraryAbsolutePath(file);
        return file;
    }
    
    // model
    public GeneticSequenceCollection getModel() {
        return model;
    }
    
    public void setModel(GeneticSequenceCollection model) {
        this.model = model;
        
        setCanAdd(model instanceof MutableGeneticSequenceCollection);
        
        fetch();
    }
    
    @Override
    protected ExhibitTextFilterator createTextFilterator() {
        return new ExhibitTextFilterator();
    }
    
    /**
     * fetch entities
     */
    public void fetch() {
        if (model == null) {
            clear();
        }
        else {
            GlazedLists.replaceAll(source, model.fetch(), false);
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
    
    public static interface GeneticSequenceCollection {
        List<GeneticSequenceRecord> fetch();
        
        boolean update(GeneticSequenceRecord record);
    }
    
    public static interface MutableGeneticSequenceCollection extends GeneticSequenceCollection {
        boolean add(GeneticSequenceRecord record);
        
        boolean add(Collection<GeneticSequenceRecord> record);

        boolean remove(GeneticSequenceRecord record);
    }
    
    public static interface GeneticSequenceLibrary extends MutableGeneticSequenceCollection {
    }
    
    public static class PersistentGeneticSequenceLibrary implements GeneticSequenceLibrary {
        private final SequenceLibrary sequenceLibrary;
        
        public PersistentGeneticSequenceLibrary(SequenceLibrary sequenceLibrary) {
            this.sequenceLibrary = sequenceLibrary;
        }
        
        @Override
        public boolean remove(GeneticSequenceRecord record) {
            return sequenceLibrary.delete(record);
        }

        @Override
        public List<GeneticSequenceRecord> fetch() {
            return sequenceLibrary.findAll();
        }

        @Override
        public boolean update(GeneticSequenceRecord record) {
            return sequenceLibrary.store(record);
        }

        @Override
        public boolean add(GeneticSequenceRecord record) {
            return sequenceLibrary.addNew(record);
        }

        @Override
        public boolean add(Collection<GeneticSequenceRecord> records) {
            boolean result = false;
            for (GeneticSequenceRecord record: records) {
                result |= sequenceLibrary.addNew(record);
            }
            return result;
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
    
    private static Collection<File> listFiles(File file) {
        Collection<File> files;
        
        if (file.isDirectory()) {
            files = FileUtils.listFiles(file, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE);
        }
        else {
            files = Collections.singleton(file);
        }
        
        return files;
    }

    class BioFileImportTask extends SwingWorker<Result, Void> {
        private final URL source;
        private final FileType fileType;
        
        public BioFileImportTask(URL source, FileType fileType) {
            this.source = source;
            this.fileType = fileType;
        }

        @Override
        protected Result doInBackground() throws Exception {
//            controller.loadExhibit(source, fileType);
            
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
