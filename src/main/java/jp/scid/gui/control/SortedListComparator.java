package jp.scid.gui.control;

import java.util.Comparator;

import ca.odell.glazedlists.SortedList;


public class SortedListComparator<E> extends AbstractValueController<Comparator<? super E>> {
    private final SortedList<E> sortedList;
    
    public SortedListComparator(SortedList<E> sortedList) {
        super();
        this.sortedList = sortedList;
    }

    public SortedList<E> getSortedList() {
        return sortedList;
    }
    
    @Override
    protected void processValueChange(Comparator<? super E> newValue) {
        sortedList.setComparator(newValue);
    }
}
