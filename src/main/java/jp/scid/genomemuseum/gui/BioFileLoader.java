package jp.scid.genomemuseum.gui;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

import jp.scid.genomemuseum.model.ExhibitListModel;
import jp.scid.genomemuseum.model.GMExhibit;
import jp.scid.genomemuseum.model.MuseumExhibitLibrary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BioFileLoader {
    private final static Logger logger = LoggerFactory.getLogger(BioFileLoader.class);
    
    final Executor taskExecutor;
    final MuseumExhibitLibrary model;
    
    public BioFileLoader(Executor taskExecutor, MuseumExhibitLibrary model) {
        this.taskExecutor = taskExecutor;
        this.model = model;
    }

    void save(GMExhibit element) {
        model.save(element);
    }
    
    public void loadFilesRecursive(List<File> fileList) {
        loadFilesRecursive(null, fileList);
    }
    
    public void loadFilesRecursive(ExhibitListModel model, List<File> fileList) {
        // TODO
    }
    
    public boolean canLoad(URL source) throws IOException {
        // TODO
        return false;
    }
    
    public Future<LoadResult> executeWithSourceUrl(URL source) throws IOException {
        GMExhibit exhibit = model.newElement();
        try {
            exhibit.setFileUri(source.toURI().toString());
        }
        catch (URISyntaxException e) {
            exhibit.setFileUri(source.toString());
        }
        
        model.save(exhibit);
        
        BioFileLoadTask task = new BioFileLoadTask(exhibit);
        execute(task);
        
        return task;
    }
    
    protected void execute(SwingWorker<?, ?> task) {
        taskExecutor.execute(task);
    }
    
    class BioFileInsertTask extends BioFileLoadTask {

        public BioFileInsertTask(GMExhibit exhibit) {
            super(exhibit);
            // TODO Auto-generated constructor stub
        }
        
    }
    
    class BioFileLoadTask extends SwingWorker<LoadResult, Void> {
        final GMExhibit exhibit;
        
        public BioFileLoadTask(GMExhibit exhibit) {
            this.exhibit = exhibit;
        }
        
        @Override
        protected LoadResult doInBackground() throws Exception {
            LoadResult result;
            
            try {
                boolean reloaded = model.reloadExhibit(exhibit);
                
                if (reloaded) {
                    result = new Success(exhibit);
                }
                else {
                    result = new InvalidFileFormat(exhibit);
                }
            }
            catch (URISyntaxException e) {
                result = new InvalidSourceURI(exhibit, e);
            }
            catch (IOException e) {
                result = new IOExceptionThrown(exhibit, e);
            }
            
            return result;
        }
        
        @Override
        protected void done() {
            if (isCancelled()) {
                return;
            }
            
            final LoadResult loadResult;
            try {
                loadResult = get();
            }
            catch (InterruptedException e) {
                logger.info("unreachable exception usually", e);
                return;
            }
            catch (ExecutionException e) {
                logger.warn("unexpected exception thrown within task", e);
                showLoadingError(exhibit, e);
                return;
            }
            
            processDoneLoading(loadResult);
        }
    }
    
    public void showLoadingError(GMExhibit exhibit, ExecutionException exception) {
        // TODO
    }
    
    public void showLoadingError(GMExhibit exhibit, URISyntaxException exception) {
        // TODO
    }
    
    public void showLoadingError(GMExhibit exhibit, IOException exception) {
        // TODO
    }

    void processDoneLoading(final LoadResult loadResult) {
        if (loadResult instanceof Success) {
            save(loadResult.exhibit());
        }
        else if (loadResult instanceof InvalidFileFormat) {
            // Do nothing
        }
        else if (loadResult instanceof InvalidSourceURI) {
            showLoadingError(loadResult.exhibit(), ((InvalidSourceURI) loadResult).cause());
        }
        else if (loadResult instanceof IOExceptionThrown) {
            showLoadingError(loadResult.exhibit(), ((IOExceptionThrown) loadResult).cause());
        }
    }

    public static abstract class LoadResult {
        private final GMExhibit exhibit;

        LoadResult(GMExhibit exhibit) {
            this.exhibit = exhibit;
        }
        
        public GMExhibit exhibit() {
            return exhibit;
        }
    }
    
    public static class Success extends BioFileLoader.LoadResult {
        Success(GMExhibit exhibit) {
            super(exhibit);
        }
    }
    
    public static class InvalidFileFormat extends BioFileLoader.LoadResult {
        InvalidFileFormat(GMExhibit exhibit) {
            super(exhibit);
        }
    }
    
    public static class InvalidSourceURI extends BioFileLoader.LoadResult {
        private final URISyntaxException e;
        
        InvalidSourceURI(GMExhibit exhibit, URISyntaxException e) {
            super(exhibit);
            this.e = e;
        }
        
        public URISyntaxException cause() {
            return e;
        }
    }
    
    public static class IOExceptionThrown extends BioFileLoader.LoadResult {
        private final IOException e;
        
        IOExceptionThrown(GMExhibit exhibit, IOException e) {
            super(exhibit);
            this.e = e;
        }
        
        public IOException cause() {
            return e;
        }
    }
}