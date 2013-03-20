package jp.scid.gui.model;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

abstract class AbstractChangableModel {
    List<ChangeListener> listeners = new LinkedList<ChangeListener>();
    
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }
    
    protected void fireStateChanged() {
        if (listeners.isEmpty())
            return;
        
        ChangeEvent event = new ChangeEvent(this);
        
        for (ChangeListener listener: listeners) {
            listener.stateChanged(event);
        }
    }
}