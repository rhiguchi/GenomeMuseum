package jp.scid.genomemuseum.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import ca.odell.glazedlists.EventList;

public class ListModelEventListAdapter<E> {
    private final SyncHandler syncHandler = new SyncHandler();
    private final EventList<E> target;
    private final Class<E> elementClass;
    private ListModel source;

    private ListModelEventListAdapter(Class<E> elementClass, EventList<E> target) {
        if (elementClass == null) throw new IllegalArgumentException("elementClass must not be null");
        if (target == null) throw new IllegalArgumentException("target must not be null");
        
        this.elementClass = elementClass;
        this.target = target;
    }

    public static <E> ListModelEventListAdapter<E> newInstanceOf(Class<E> elementClass, EventList<E> target) {
        return new ListModelEventListAdapter<E>(elementClass, target);
    }
    
    public Class<E> getElementClass() {
        return elementClass;
    }
    
    public void setSource(ListModel source) {
        if (this.source != null) {
            this.source.removeListDataListener(syncHandler);
        }
        this.source = source;
        
        List<E> elements = Collections.emptyList();
        if (target != null) {
            elements = getElements(source, 0, source.getSize());
        }
        
        target.getReadWriteLock().writeLock().lock();
        try {
            target.clear();
            target.addAll(elements);
        }
        finally {
            target.getReadWriteLock().writeLock().unlock();
        }
        
        if (source != null) {
            source.addListDataListener(syncHandler);
        }
    }
    
    public EventList<E> getTarget() {
        return target;
    }

    private List<E> getElements(ListModel source, int start, int end) {
        List<E> newElements = new ArrayList<E>(end - start);

        for (int index = start; index < end; index++) {
            Object object = source.getElementAt(index);
            E element = getElementClass().cast(object);
            newElements.add(element);
        }
        return newElements;
    }
    
    private class SyncHandler implements ListDataListener {
        @Override
        public void intervalAdded(ListDataEvent e) {
            ListModel source = (ListModel) e.getSource();
            int start = e.getIndex0();

            List<E> newElements = getElements(source, start, e.getIndex1() + 1);
            target.addAll(start, newElements);
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            int start = e.getIndex0();
            int end = e.getIndex1() + 1;
            target.subList(start, end).clear();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            ListModel source = (ListModel) e.getSource();
            int start = e.getIndex0();
            int end = e.getIndex1() + 1;

            for (int index = start; index < end; index++) {
                Object object = source.getElementAt(index);
                E element = getElementClass().cast(object);
                target.set(index, element);
            }
        }
    }
}