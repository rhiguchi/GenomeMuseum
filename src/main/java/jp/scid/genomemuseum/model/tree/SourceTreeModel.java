package jp.scid.genomemuseum.model.tree;

import java.util.List;

import javax.swing.event.ListDataListener;

public interface SourceTreeModel<E> {
    void addChildrenListener(E parent, ListDataListener l);
    
    void removeChildrenListener(E parent, ListDataListener l);
    
    List<E> getChildren(E element);
    
    boolean isLeaf(E element);
}