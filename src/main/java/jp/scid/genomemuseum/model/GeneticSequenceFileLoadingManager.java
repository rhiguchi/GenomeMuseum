package jp.scid.genomemuseum.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.swing.SwingWorker;

import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.bio.store.sequence.ImportableSequenceSource;

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
    
    public List<Future<GeneticSequence>> executeLoading(Collection<File> files, ImportableSequenceSource dest) {
        if (!isRunnning()) {
            resetCount();
        }
        
        List<Future<GeneticSequence>> futures = new ArrayList<Future<GeneticSequence>>(files.size());
        
        for (File file: files) {
            LoadingTask task = new LoadingTask(file, dest);
            execute(task);
            futures.add(task);
        }
        
        return futures;
    }
    
    public <V> Future<V> execute(final Callable<V> command) {
        final FutureTask<V> future = new FutureTask<V>(command) {
            @Override
            protected void done() {
                removeFuture(this);
            }
        };
        
        addFuture(future);
        executor.execute(future);
        return future;
    }

    private void addFuture(FutureTask<?> future) {
        runningTasks.add(future);
        setExecuted(executed + 1);
        setInProgress(true);
    }

    private void removeFuture(FutureTask<?> future) {
        runningTasks.remove(future);
        setInProgress(!runningTasks.isEmpty());
        
        if (!future.isCancelled()) try {
            future.get();
            setSuccess(success + 1);
        }
        catch (InterruptedException ignore) {
            // ignore
        }
        catch (ExecutionException e) {
            e.printStackTrace();
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
        private final ImportableSequenceSource dest;
        
        public LoadingTask(File file, ImportableSequenceSource dest) {
            this.file = file;
            this.dest = dest;
        }

        @Override
        protected GeneticSequence doInBackground() throws Exception {
            return dest.importSequence(file);
        }
        
        @Override
        protected void done() {
            finished(this);
            try {
                if (!isCancelled()) get();
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
