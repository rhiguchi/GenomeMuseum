package jp.scid.gui.model;

import java.util.Comparator;
import java.util.List;

import ca.odell.glazedlists.AbstractEventList;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

abstract public class AbstractPersistentEventList<E> extends AbstractEventList<E> implements ListEventListener<E> {
    private final EventList<E> delegate;
    
    private final Comparator<E> idComparator;
    
    private boolean isInitialized = false;
    
    public AbstractPersistentEventList(EventList<E> evetList, Comparator<E> identifierComparator) {
        super(evetList.getPublisher());
        
        if (identifierComparator == null)
            throw new IllegalArgumentException("identifierComparator must not be null");
        
        this.delegate = evetList;
        this.readWriteLock = evetList.getReadWriteLock();
        
        idComparator = identifierComparator;
        
        evetList.addListEventListener(this);
    }
    
    public AbstractPersistentEventList(ListEventPublisher publisher, ReadWriteLock readWriteLock,
            Comparator<E> identifierComparator) {
        this(new BasicEventList<E>(publisher, readWriteLock), identifierComparator);
    }
    
    public AbstractPersistentEventList(Comparator<E> identifierComparator) {
        this(new BasicEventList<E>(), identifierComparator);
    }
    
    public <T extends Comparable<T>> AbstractPersistentEventList(Function<E, T> identifierFunction) {
        this(new UniqueIdComparator<E, T>(identifierFunction));
    }
    
    @Override
    public void dispose() {
        delegate.removeListEventListener(this);
    }

    public void reload() {
        List<E> list = fetchAll();
        
        delegate.getReadWriteLock().writeLock().lock();
        try {
            GlazedLists.replaceAll(delegate, list, false, idComparator);
        }
        finally {
            delegate.getReadWriteLock().writeLock().unlock();
        }
        isInitialized = true;
    }
    
    // Read
    EventList<E> getFetchedList() {
        if (!isInitialized) {
            reload();
        }
        
        return delegate;
    }
    
    abstract protected List<E> fetchAll();
    
    @Override
    public int size() {
        return getFetchedList().size();
    }
    
    @Override
    public E get(int index) {
        return getFetchedList().get(index);
    }
    
    @Override
    public int indexOf(Object object) {
        return getFetchedList().indexOf(object);
    }
    
    // insert
    abstract protected boolean insertToTable(E element);
    
    @Override
    public void add(int index, E element) {
        insertToTable(element);
        
        getFetchedList().add(index, element);
    }
    
    // Update
    abstract protected void updateToTable(E element);
    
    public void elementChanged(E element) {
        int updatingIndex = indexOf(element);
        set(updatingIndex, element);
    }
    
    @Override
    public E set(int index, E element) {
        updateToTable(element);
        
        E old = getFetchedList().set(index, element);
        
        return old;
    }
    
    // Remove
    abstract protected boolean deleteFromTable(E element);
    
    @Override
    public E remove(int index) {
        E element = getFetchedList().get(index);
        
        deleteFromTable(element);
        getFetchedList().remove(index);
        
        return element;
    }
    
    // Event
    @Override
    public void listChanged(ListEvent<E> listChanges) {
        if (isInitialized) {
            updates.forwardEvent(listChanges);
        }
    }
    
    static class UniqueIdComparator<E, N extends Comparable<N>> implements Comparator<E> {
        private final Function<? super E, ? extends N> comaprableFunction;
        
        public UniqueIdComparator(Function<? super E, ? extends N> uniqueIdFunction) {
            if (uniqueIdFunction == null)
                throw new IllegalArgumentException("uniqueIdFunction must not be null");
            
            this.comaprableFunction = uniqueIdFunction;
        }

        @Override
        public int compare(E o1, E o2) {
            N val1 = comaprableFunction.evaluate(o1);
            N val2 = comaprableFunction.evaluate(o2);
            
            if (val1 == null) {
                if (val2 == null) {
                    return Integer.valueOf(o1.hashCode()).compareTo(o2.hashCode()); 
                }
                else {
                    return 1;
                }
            }
            else if (val2 == null) {
                return -1;
            }
            else {
                return val1.compareTo(val2);
            }
        }
    }
}
