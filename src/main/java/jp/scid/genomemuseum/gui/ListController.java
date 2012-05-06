package jp.scid.genomemuseum.gui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.jdesktop.application.AbstractBean;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;

class ListController<E> extends AbstractBean {
    protected EventTableModel<E> tableModel;
    
    protected final EventList<E> listModel;
    
    protected final SortedList<E> sortedList;
    
    protected final FilterList<E> filterList;

    protected final EventSelectionModel<E> selectionModel;
    
    // Actions
    protected final AddAction addAction;
    
    protected final RemoveAction removeAction;
    
    public ListController(EventList<E> listModel) {
        if (listModel == null) {
            listModel = createModelEventList();
        }
        this.listModel = listModel;

        sortedList = new SortedList<E>(listModel, null);
        
        filterList = new FilterList<E>(sortedList);
        
        selectionModel = new EventSelectionModel<E>(filterList);
        
        addAction = createAddAction();
        addAction.setEnabled(canAdd());
        
        removeAction = createRemoveAction();
    }

    public ListController() {
        this(null);
    }

    public List<E> getSource() {
        return listModel;
    }
    
    public List<E> getTransformedElements() {
        return filterList;
    }

    public void setSource(List<E> newSource) {
        EventList<E> list = getModel();
        
        list.getReadWriteLock().writeLock().lock();
        try {
            if (newSource == null || newSource.isEmpty()) {
                list.clear();
            }
            else {
                GlazedLists.replaceAll(list, newSource, true);
            }
        }
        finally {
            list.getReadWriteLock().writeLock().unlock();
        }
    }
    
    EventList<E> getModel() {
        return listModel;
    }
    
    public void replaceSource(List<E> newSource, boolean updates) {
        GlazedLists.replaceAll(getModel(), newSource, updates);
    }
    
    public EventSelectionModel<E> getSelectionModel() {
        return selectionModel;
    }
    
    // selection
    public E getSelection() {
        List<E> list = getSelections();
        if (list.isEmpty()) {
            return null;
        }
        else {
            return list.get(0);
        }
    }
    
    public void setSelection(E newSelection) {
        if (newSelection == null) {
            setSelections(Collections.<E>emptyList());
        }
        else {
            setSelections(Collections.singletonList(newSelection));
        }
    }
    
    // selections
    public List<E> getSelections() {
        ArrayList<E> selections = new ArrayList<E>(selectionModel.getSelected());
        return selections;
    }
    
    public void setSelections(List<E> selections) {
        selectionModel.getTogglingSelected().addAll(selections);
    }
    
    public void bindTable(JTable table) {
        table.setModel(getTableModel());
        table.setSelectionModel(selectionModel);
        
        table.getActionMap().put("delete", removeAction);
        table.getActionMap().put("add", addAction);
    }

    protected TableFormat<E> getTableFormat() {
        throw new UnsupportedOperationException("method must implement to use tableformat");
    }
    
    public TableModel getTableModel() {
        if (tableModel == null) {
            tableModel = new EventTableModel<E>(getModel(), getTableFormat());
        }
        return tableModel;
    }
    
    // Add
    public boolean canAdd() {
        return addAction.isEnabled();
    }
    
    protected void setCanAdd(boolean newValue) {
        addAction.setEnabled(newValue);
    }
    
    public void add() {
        int index = selectionModel.getMaxSelectionIndex() + 1;

        add(index);
    }

    public void add(int index) {
        E newElement = createElement();
        
        add(index, newElement);
    }
    
    public void add(E element) {
        int index = selectionModel.getMaxSelectionIndex() + 1;
        
        add(index, element);
    }
    
    public void add(int index, E element) {
        getTransformedElements().add(index, element);
    }

    protected E createElement() {
        throw new UnsupportedOperationException("must implement to create element");
    }
    
    // Move
    public void move(int[] indices, int dest) {
        // TODO
        for (int i = indices.length - 1; i >= 0; i--) {
            int sourceIndex = indices[i];
            
            List<E> subList = listModel.subList(dest, sourceIndex + 1);
            Collections.rotate(subList, 1);
        }
    }
    
    // Remove
    public void removeAt(int index) {
        E element = getTransformedElements().remove(index);
    }
    
    public void remove() {
        int minSelectionIndex = selectionModel.getMinSelectionIndex();
        int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
        
        if (minSelectionIndex < 0 || maxSelectionIndex < 0)
            return;
        
        int[] indices = new int[maxSelectionIndex - minSelectionIndex + 1];
        int count = 0;
        for (int index = minSelectionIndex; index <= maxSelectionIndex; index++) {
            if (selectionModel.isSelectedIndex(index)) {
                indices[count++] = index;
            }
        }
        
        int[] selectedIndices = Arrays.copyOf(indices, count);
        removeAt(selectedIndices);
    }
    
    public void removeAt(int[] indices) {
        for (int i = indices.length - 1; i >= 0; i--) {
            int index = indices[i];
            removeAt(index);
        }
    }
    
    public void elementChange(int index) {
        getTransformedElements().set(index, getTransformedElements().get(index));
    }

    public void elementChanged(E exhibit) {
        int index = 0;
        for (Iterator<E> ite = listModel.iterator(); ite.hasNext(); index++) {
            E element = ite.next();
            
            if (exhibit.equals(element)) {
                listModel.set(index, exhibit);
            }
        }
    }
    
    protected EventList<E> createModelEventList() {
        return new BasicEventList<E>();
    }
    
    // Action factories
    protected AddAction createAddAction() {
        return new AddAction("Add");
    }
    
    protected RemoveAction createRemoveAction() {
        return new RemoveAction("Remove");
    }
    
    // Actions
    protected class AddAction extends AbstractAction {
        public AddAction(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            add();
        }
    }
    
    protected class RemoveAction extends AbstractAction {
        public RemoveAction(String name) {
            super(name);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            remove();
        }
    }
}
