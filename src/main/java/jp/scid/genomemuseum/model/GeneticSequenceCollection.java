package jp.scid.genomemuseum.model;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import jp.scid.bio.store.jooq.tables.records.GeneticSequenceRecord;

public interface GeneticSequenceCollection {
    List<GeneticSequenceRecord> fetch();
    
    File getFilePath(GeneticSequenceRecord record);
    
    boolean update(GeneticSequenceRecord record);
    
    void addListDataListener(ListDataListener listener);
    
    void removeListDataListener(ListDataListener listener);
}

abstract class AbstractGeneticSequenceCollection implements GeneticSequenceCollection {
    private final List<ListDataListener> listenerList = new LinkedList<ListDataListener>();
    
    public void addListDataListener(ListDataListener listener) {
        listenerList.add(listener);
    }
    
    public void removeListDataListener(ListDataListener listener) {
        listenerList.remove(listener);
    }
    
    protected void fireListDataAdded() {
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, -1, -1);
        
        for (ListDataListener listener: listenerList) {
            listener.intervalAdded(event);
        }
    }
    
    protected void fireListDataRemoved() {
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, -1, -1);
        
        for (ListDataListener listener: listenerList) {
            listener.intervalRemoved(event);
        }
    }
    
    protected void fireListDataChanged() {
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        
        for (ListDataListener listener: listenerList) {
            listener.contentsChanged(event);
        }
    }
}