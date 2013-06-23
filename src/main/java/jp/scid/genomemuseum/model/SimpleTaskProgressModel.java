package jp.scid.genomemuseum.model;

import java.net.URI;

import javax.swing.SwingWorker.StateValue;

public class SimpleTaskProgressModel extends AbstractTaskProgressModel {
    private URI sourceUri = null;
    private StateValue taskState = StateValue.PENDING;
    private long taskSize = 0;
    private long taskProgress = 0;
    
    @Override
    public long getTaskSize() {
        return taskSize;
    }
    
    public void setTaskSize(long taskSize) {
        this.taskSize = taskSize;
        fireTaskStateChange();
    }

    @Override
    public long getTaskProgress() {
        return taskProgress;
    }
    
    public void setTaskProgress(long taskProgress) {
        this.taskProgress = taskProgress;
        fireTaskStateChange();
    }

    @Override
    public StateValue getTaskState() {
        return taskState;
    }
    
    public void setTaskState(StateValue taskState) {
        this.taskState = taskState;
        fireTaskStateChange();
    }

    @Override
    public URI sourceUri() {
        return sourceUri;
    }
    
    public void setSourceUri(URI sourceUri) {
        this.sourceUri = sourceUri;
    }
}