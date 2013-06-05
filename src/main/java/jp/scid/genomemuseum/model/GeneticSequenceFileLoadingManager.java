package jp.scid.genomemuseum.model;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;

import javax.swing.SwingWorker;

import jp.scid.bio.store.sequence.GeneticSequence;

import org.jdesktop.application.AbstractBean;

public class GeneticSequenceFileLoadingManager extends AbstractBean {
    private final BlockingQueue<Callable<GeneticSequence>> loadingTaskQueue;
    private final Executor executor;
    private HashSet<ProperetyAdapter> executedTasks;
    
    public GeneticSequenceFileLoadingManager(Executor executor) {
        this.executor = executor;
        loadingTaskQueue = new LinkedBlockingDeque<Callable<GeneticSequence>>();
    }
    
    public void executeLoading(Collection<File> files, SequenceImportable dest) {
        for (File file: files) {
            Callable<GeneticSequence> task = dest.createSequenceImportTask(file);
            loadingTaskQueue.add(task);
        }
        
        Runner<GeneticSequence> runner = new Runner<GeneticSequence>(loadingTaskQueue, executor);
        runner.execute();
    }
    
    void execute(File file, SequenceImportable dest) {
        Callable<GeneticSequence> task = dest.createSequenceImportTask(file);
        ProperetyAdapter adapter = new ProperetyAdapter(task);
        execute(adapter);
    }
    
    private void execute(ProperetyAdapter task) {
        setInProgress(true);
        incrementQueuedTaskCount();
        executedTasks.add(task);
        executor.execute(task);
    }
    
    private void afterExecuted(ProperetyAdapter task) {
        if (!task.isCancelled()) {
            incrementDoneTaskCount();
        }
        
        executedTasks.remove(task);
        setInProgress(!executedTasks.isEmpty());
    }
    
    private void incrementDoneTaskCount() {
        
    }
    
    private void incrementQueuedTaskCount() {
        
    }
    
    private void setInProgress(boolean inProgress) {
        
    }
    
    public class ProperetyAdapter extends SwingWorker<GeneticSequence, Void> {
        private final Callable<GeneticSequence> task;
        
        public ProperetyAdapter(Callable<GeneticSequence> task) {
            this.task = task;
        }

        @Override
        protected GeneticSequence doInBackground() throws Exception {
            return task.call();
        }
        
        @Override
        protected void done() {
            afterExecuted(this);
        }
    }
    
    public static class Runner<T> extends SwingWorker<Void, Future<T>> {
        private final BlockingQueue<Callable<T>> queue;
        private final Executor executor;
        private int executed = 0;
        private int success = 0;
        
        public Runner(BlockingQueue<Callable<T>> queue, Executor executor) {
            if (queue == null) throw new IllegalArgumentException("queue must not be null");
            if (executor == null) throw new IllegalArgumentException("executor must not be null");
            
            this.queue = queue;
            this.executor = executor;
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            Set<Future<?>> runningTasks = Collections.synchronizedSet(new HashSet<Future<?>>());
            
            while (!isCancelled()) {
                Callable<T> callable = queue.poll();
                
                if (callable != null) {
                    FutureTask<T> task = createTask(callable, runningTasks);
                    
                    runningTasks.add(task);
                    incrementExecuted();
                    
                    executor.execute(task);
                }
                
                if (runningTasks.isEmpty()) {
                    break;
                }
            }
            
            if (isCancelled()) {
                for (Future<?> future: runningTasks) {
                    future.cancel(false);
                }
            }
            
            return null;
        }

        private FutureTask<T> createTask(Callable<T> callable, final Collection<Future<?>> runningTasks) {
            FutureTask<T> task = new FutureTask<T>(callable) {
                @SuppressWarnings("unchecked")
                @Override
                protected void done() {
                    if (!isCancelled()) {
                        incrementSuccess();
                    }
                    runningTasks.remove(this);
                    publish(this);
                }
            };
            return task;
        }
        
        public int getExecuted() {
            return executed;
        }
        
        public int getSuccess() {
            return success;
        }
        
        private synchronized void incrementExecuted() {
            firePropertyChange("executed", executed, ++executed);
        }
        
        private synchronized void incrementSuccess() {
            firePropertyChange("success", success, ++success);
        }
    }
}
