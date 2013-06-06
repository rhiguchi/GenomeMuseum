package jp.scid.genomemuseum.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

import jp.scid.bio.store.sequence.GeneticSequence;

import org.jdesktop.application.AbstractBean;

public class GeneticSequenceFileLoadingManager extends AbstractBean {
    private final Executor executor;
    
    private final Set<Future<?>> runningTasks = Collections.synchronizedSet(new HashSet<Future<?>>());
    
    private boolean inProgress = false;
    private int executed = 0;
    private int success = 0;
    
    public GeneticSequenceFileLoadingManager(Executor executor) {
        this.executor = executor;
    }
    
    public void executeLoading(Collection<File> files, SequenceImportable dest) {
        if (!isRunnning()) {
            resetCount();
        }
        
        for (File file: files) {
            LoadingTask task = new LoadingTask(file, dest);
            execute(task);
        }
    }
    
    public boolean isInProgress() {
        return inProgress;
    }

    private void setInProgress(boolean inProgress) {
        firePropertyChange("inProgress", this.inProgress, this.inProgress = inProgress);
    }

    public int getExecuted() {
        return executed;
    }

    private void setExecuted(int executed) {
        firePropertyChange("executed", this.executed, this.executed = executed);
    }

    public int getSuccess() {
        return success;
    }

    private void setSuccess(int success) {
        firePropertyChange("success", this.success, this.success = success);
    }
    
    private boolean isRunnning() {
        return !runningTasks.isEmpty();
    }
    
    private void resetCount() {
        setExecuted(0);
        setSuccess(0);
    }
    
    private void execute(LoadingTask task) {
        runningTasks.add(task);
        setInProgress(true);
        setExecuted(executed + 1);
        executor.execute(task);
    }
    
    private void finished(LoadingTask task) {
        runningTasks.remove(task);
        
        if (!task.isCancelled()) {
            setSuccess(success + 1);
        }
        setInProgress(isRunnning());
    }
    
    class LoadingTask extends SwingWorker<GeneticSequence, Void> {
        private final File file;
        private final SequenceImportable dest;
        
        public LoadingTask(File file, SequenceImportable dest) {
            this.file = file;
            this.dest = dest;
        }

        @Override
        protected GeneticSequence doInBackground() throws Exception {
            Callable<GeneticSequence> task = dest.createSequenceImportTask(file);
            return task.call();
        }
        
        @Override
        protected void done() {
            finished(this);
        }
    }
}
