package jp.scid.genomemuseum.gui;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.SwingWorker;

import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumExhibit.FileType;
import jp.scid.genomemuseum.model.MuseumExhibitLibrary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExhibitDataLoader {
    private final static Logger logger = LoggerFactory.getLogger(ExhibitDataLoader.class);
    final Executor taskExecutor;
    final MuseumExhibitLibrary library;
    
    public ExhibitDataLoader(Executor taskExecutor, MuseumExhibitLibrary library) {
        this.taskExecutor = taskExecutor;
        this.library = library;
    }
    
    public ExhibitDataLoader(MuseumExhibitLibrary model) {
        this(Executors.newSingleThreadExecutor(), model);
    }

    public MuseumExhibit newMuseumExhibit(URI source) {
        MuseumExhibit exhibit = library.newMuseumExhibit();
        exhibit.setFileUri(source.toString());
        
        return exhibit;
    }
    
    public FileType updateFileFormat(MuseumExhibit exhibit) throws IOException {
        URL url = exhibit.getSourceFileAsUrl();
        
        final FileType newFileType = library.findFileType(url);
        
        exhibit.setFileType(newFileType);
        
        return newFileType;
    }
    
    public SwingWorker<LoadResult, ?> executeReload(MuseumExhibit exhibit) {
        BioFileLoadTask task = new BioFileLoadTask(exhibit);
        execute(task);
        
        return task;
    }
    
    protected void execute(SwingWorker<?, ?> task) {
        taskExecutor.execute(task);
    }
    
    class BioFileLoadTask extends SwingWorker<LoadResult, Void> {
        final MuseumExhibit exhibit;
        
        public BioFileLoadTask(MuseumExhibit exhibit) {
            this.exhibit = exhibit;
        }
        
        @Override
        protected LoadResult doInBackground() throws Exception {
            LoadResult result;
            
            try {
                boolean reloaded = library.reloadExhibit(exhibit);
                
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
    
    public void showLoadingError(MuseumExhibit exhibit, ExecutionException exception) {
        // TODO
    }
    
    public void showLoadingError(MuseumExhibit exhibit, URISyntaxException exception) {
        // TODO
    }
    
    public void showLoadingError(MuseumExhibit exhibit, IOException exception) {
        // TODO
    }

    void processDoneLoading(final LoadResult loadResult) {
        if (loadResult instanceof Success) {
            // Do nothing
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
        private final MuseumExhibit exhibit;

        LoadResult(MuseumExhibit exhibit) {
            this.exhibit = exhibit;
        }
        
        public MuseumExhibit exhibit() {
            return exhibit;
        }
    }
    
    public static class Success extends LoadResult {
        Success(MuseumExhibit exhibit) {
            super(exhibit);
        }
    }
    
    public static class InvalidFileFormat extends LoadResult {
        InvalidFileFormat(MuseumExhibit exhibit) {
            super(exhibit);
        }
    }
    
    public static class InvalidSourceURI extends LoadResult {
        private final URISyntaxException e;
        
        InvalidSourceURI(MuseumExhibit exhibit, URISyntaxException e) {
            super(exhibit);
            this.e = e;
        }
        
        public URISyntaxException cause() {
            return e;
        }
    }
    
    public static class IOExceptionThrown extends LoadResult {
        private final IOException e;
        
        IOExceptionThrown(MuseumExhibit exhibit, IOException e) {
            super(exhibit);
            this.e = e;
        }
        
        public IOException cause() {
            return e;
        }
    }
}