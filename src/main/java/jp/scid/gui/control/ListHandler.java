package jp.scid.gui.control;

import java.util.ArrayList;
import java.util.List;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

abstract public class ListHandler<E> extends AbstractController<EventList<E>> {
    private ListEventListener<E> changeListener = new ListEventListener<E>() {
        @Override
        public void listChanged(ListEvent<E> listChanges) {
            processPropertyChange(listChanges.getSourceList(), null);
        }
    };
    
    @Override
    final protected void processPropertyChange(EventList<E> model, String property) {
        final List<E> list;
        
        model.getReadWriteLock().readLock().lock();
        try {
            list = new ArrayList<E>(model);
        }
        finally {
            model.getReadWriteLock().readLock().unlock();
        }
        processValueChange(list);
    }
    
    protected abstract void processValueChange(List<E> modelList);
    
    @Override
    protected void listenTo(EventList<E> model) {
        model.addListEventListener(changeListener);
    }
    
    @Override
    protected void deafTo(EventList<E> model) {
        model.removeListEventListener(changeListener);
    }
}
