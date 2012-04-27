package jp.scid.genomemuseum.model.tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList.Function;

public class DefaultSourceTreeModel<E> extends AbstractSourceTreeModel<E> {
    
    protected EventList<E> elementList;
    
    protected Function<E, E> parentFunction = null;
    
    public DefaultSourceTreeModel() {
        elementList = new BasicEventList<E>();
    }
    
    protected void fireListDataChange(E parent, ListDataEvent event) {
        
    }
    
    protected E getParent(E element) {
        if (parentFunction == null) {
            return null;
        }
        
        return parentFunction.evaluate(element);
    }
    
    void fireElementInserted(E element) {
        E parent = getParent(element);
        int index = getChildren(parent).indexOf(element);
        
        fireChildAdded(parent, index);
    }
    
    void fireElementChanged(E element) {
        E parent = getParent(element);
        int index = getChildren(parent).indexOf(element);
        
        fireChildChanged(parent, index);
    }
    
    @Override
    public List<E> getChildren(E element) {
        // TODO Auto-generated method stub
        return (List<E>) Arrays.asList("1", "2", "3");
    }

    @Override
    public boolean isLeaf(E element) {
        return false;
    }
}

abstract class AbstractSourceTreeModel<E> implements SourceTreeModel<E> {
    private final Map<E, List<ListDataListener>> listenerMap =
            new HashMap<E, List<ListDataListener>>();
    
    @Override
    public void addChildrenListener(E parent, ListDataListener l) {
        List<ListDataListener> listeners = listenerMap.get(parent);
        if (listeners == null) {
            listenerMap.put(parent, listeners = new LinkedList<ListDataListener>());
        }
        
        listeners.add(l);
    }
    
    @Override
    public void removeChildrenListener(E parent, ListDataListener l) {
        List<ListDataListener> listeners = listenerMap.get(parent);
        if (listeners != null) {
            listeners.remove(l);
        }
    }
    
    List<ListDataListener> getListeners(E parent) {
        List<ListDataListener> list = listenerMap.get(parent);
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }
    
    protected void fireChildAdded(E parent, int index) {
        List<ListDataListener> listeners = getListeners(parent);
        if (listeners.isEmpty()) return;
        
        ListDataEvent event =
                new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index);
        
        for (ListDataListener l: getListeners(parent)) {
            l.intervalAdded(event);
        }
    }
    
    protected void fireChildRemoved(E parent, int index) {
        List<ListDataListener> listeners = getListeners(parent);
        if (listeners.isEmpty()) return;
        
        ListDataEvent event =
                new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index);
        
        for (ListDataListener l: getListeners(parent)) {
            l.intervalRemoved(event);
        }
    }
    
    protected void fireChildChanged(E parent, int index) {
        List<ListDataListener> listeners = getListeners(parent);
        if (listeners.isEmpty()) return;
        
        ListDataEvent event =
                new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index);
        
        for (ListDataListener l: getListeners(parent)) {
            l.contentsChanged(event);
        }
    }
}
