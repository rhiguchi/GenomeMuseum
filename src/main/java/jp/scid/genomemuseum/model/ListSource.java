package jp.scid.genomemuseum.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.event.ChangeListener;

import org.jooq.Condition;

public interface ListSource<E> {
    List<E> getAllElements();
    
    List<E> fetchElementsWith(Collection<Condition> condition);
    
    E newElement();
    
    boolean save(E element);
    
    boolean delete(E element);
    
    Comparator<E> getIdentifierComparator();
    
    void addChangeListener(ChangeListener listener);
    
    void removeChangeListener(ChangeListener listener);
}
