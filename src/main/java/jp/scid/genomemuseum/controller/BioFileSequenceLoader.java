package jp.scid.genomemuseum.controller;

import java.io.Reader;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import jp.scid.gui.control.AbstractValueController;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class BioFileSequenceLoader extends AbstractValueController<BioFileSequenceLoader.BioFileSource> {
    private ContentLoader currentLoader = null;
    
    private final ValueModel<String> sequence = ValueModels.newValueModel("");
    
    public BioFileSequenceLoader() {
    }
    
    public ValueModel<String> getSequence() {
        return sequence;
    }
    
    @Override
    protected void processValueChange(BioFileSource newValue) {
        clearContent();
        
        if (newValue != null) {
            ContentLoader task = createContentLoader(newValue);
            stopCurrentTask();
            execute(task);
        }
    }
    
    public void reloadSource() {
        processPropertyChange(getModel(), getProperty());
    }
    
    ContentLoader createContentLoader(BioFileSource source) {
        return new ContentLoader(source, getSequence());
    }
    
    synchronized void stopCurrentTask() {
        if (currentLoader != null && !currentLoader.isDone())
            currentLoader.cancel(true);
    }
    
    synchronized void execute(ContentLoader task) {
        currentLoader = task;
        task.execute();
    }
    
    void clearContent() {
        getSequence().setValue("");
    }

    static class ContentLoader extends SwingWorker<String, Void> {
        private final BioFileSource source;
        private final ValueModel<String> sequence;
        
        public ContentLoader(BioFileSource source, ValueModel<String> sequence) {
            this.source = source;
            this.sequence = sequence;
        }

        @Override
        protected String doInBackground() throws Exception {
            String sequence = source.getBioFileFormat().getSequence(source.getReader());
            return sequence;
        }
        
        @Override
        protected void done() {
            if (isCancelled())
                return;
            
            final String sequenceString;
            try {
                sequenceString = get();
            }
            catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            catch (ExecutionException e) {
                e.printStackTrace();
                return;
            }
            
            sequence.setValue(sequenceString);
        }
    }
    
    public static interface BioFileSource {
        Reader getReader();
        BioFileFormat getBioFileFormat();
    }
}
