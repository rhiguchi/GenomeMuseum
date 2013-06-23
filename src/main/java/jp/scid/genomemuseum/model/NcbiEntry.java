package jp.scid.genomemuseum.model;

import java.net.URI;

import javax.swing.SwingWorker.StateValue;

import jp.scid.bio.store.remote.RemoteSource.RemoteEntry;

public class NcbiEntry extends AbstractTaskProgressModel implements TaskProgressModel {
    private final RemoteEntry entry;
    private StateValue taskState = StateValue.PENDING;
    private long taskSize = 0;
    private long taskProgress = 0;
    
    private NcbiEntry(RemoteEntry entry) {
        this.entry = entry;
    }
  
    public static NcbiEntry fromRemoteSource(RemoteEntry e) {
        return new NcbiEntry(e);
    }

    public String identifier() {
        return entry.identifier();
    }

    public String accession() {
        return entry.accession();
    }

    public int sequenceLength() {
        return entry.sequenceLength();
    }

    public String definition() {
        return entry.definition();
    }

    public String taxonomy() {
        return entry.taxonomy();
    }
    
    @Override
    public String toString() {
        return identifier();
    }
    
    public URI sourceUri() {
        String uriString = entry.sourceUri();
        if (uriString.isEmpty()) {
            return null;
        }
        return URI.create(uriString);
    }
    
    public StateValue getTaskState() {
        return taskState;
    }
    
    public void setTaskState(StateValue taskState) {
        this.taskState = taskState;
        fireTaskStateChange();
    }
    
    public long getTaskSize() {
        return taskSize;
    }
    
    public void setTaskSize(long taskSize) {
        this.taskSize = taskSize;
        fireTaskStateChange();
    }
    
    public long getTaskProgress() {
        return taskProgress;
    }
    
    public void setTaskProgress(long taskProgress) {
        this.taskProgress = taskProgress;
        fireTaskStateChange();
    }
}