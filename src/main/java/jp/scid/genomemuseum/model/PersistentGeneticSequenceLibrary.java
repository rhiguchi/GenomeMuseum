package jp.scid.genomemuseum.model;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.scid.bio.store.FileLibrary;
import jp.scid.bio.store.GeneticSequenceParser;
import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.jooq.tables.records.GeneticSequenceRecord;
import jp.scid.genomemuseum.gui.GeneticSequenceListController;

public class PersistentGeneticSequenceLibrary extends AbstractGeneticSequenceCollection implements GeneticSequenceLibrary {
    final static Logger logger = LoggerFactory.getLogger(GeneticSequenceListController.class);
    
    private final GeneticSequenceParser geneticSequenceparser;
    private final SequenceLibrary sequenceLibrary;
    private FileLibrary fileLibrary = null;

    public PersistentGeneticSequenceLibrary(SequenceLibrary sequenceLibrary) {
        this.sequenceLibrary = sequenceLibrary;
        geneticSequenceparser = new GeneticSequenceParser();
    }

    public void setFileLibrary(FileLibrary fileLibrary) {
        this.fileLibrary = fileLibrary;
    }
    
    @Override
    public GeneticSequenceRecord newElement() {
        return sequenceLibrary.createRecord();
    }

    @Override
    public boolean remove(GeneticSequenceRecord record) {
        boolean result = sequenceLibrary.delete(record);
        fireListDataRemoved();
        return result;
    }

    @Override
    public int remove(List<GeneticSequenceRecord> list, boolean deleteFile) {
        if (deleteFile) for (GeneticSequenceRecord record: list) {
            File file = getFilePath(record);
            if (file == null) {
                continue;
            }

            if (file.delete()) {
                logger.info("delete file {}", file);
            }
            else {
                logger.warn("cannot delete file {}", file);
            }
        }

        int result = sequenceLibrary.delete(list);
        fireListDataRemoved();
        return result;
    }

    @Override
    public int remove(List<GeneticSequenceRecord> list) {
        return remove(list, false);
    }

    @Override
    public List<GeneticSequenceRecord> fetch() {
        return sequenceLibrary.findAll();
    }

    @Override
    public boolean update(GeneticSequenceRecord record) {
        boolean result = sequenceLibrary.store(record);
        fireListDataChanged();
        return result;
    }

    @Override
    public boolean add(GeneticSequenceRecord record) {
        boolean result = sequenceLibrary.addNew(record);
        fireListDataAdded();
        return result;
    }

    @Override
    public boolean add(Collection<GeneticSequenceRecord> records) {
        boolean result = false;
        for (GeneticSequenceRecord record: records) {
            result |= sequenceLibrary.addNew(record);
        }
        fireListDataAdded();
        return result;
    }

    @Override
    public boolean addFile(File source) {
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

                add(records);

                // store to local storage
                File path = storeGeneticSequenceFile(file, records.get(0));
                for (GeneticSequenceRecord record: records) {
                    record.setFileUri(path.toString());
                    // elementChanged(record);
                }

                result = true;
            }
        }
        catch (IOException e) {
            logger.error("fail to read gene file", e);
        }

        return result;
    }

    @Override
    public File getFilePath(GeneticSequenceRecord record) {
        String uriString = record.getFileUri();
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

    private File storeGeneticSequenceFile(File file, GeneticSequenceRecord record)
            throws IOException {
        File dest = fileLibrary.storeFile(file, record);
        logger.info("copy file {} to {}", file, dest);

        File relativePath = fileLibrary.convertLibraryRelativePath(dest);
        return relativePath;
    }

    static Collection<File> listFiles(File file) {
        Collection<File> files;
        
        if (file.isDirectory()) {
            files = FileUtils.listFiles(file, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE);
        }
        else {
            files = Collections.singleton(file);
        }
        
        return files;
    }
}