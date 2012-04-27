package jp.scid.genomemuseum.gui;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;

import jp.scid.genomemuseum.model.ListSource;

import org.jdesktop.application.AbstractBean;
import org.jooq.Condition;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.SearchEngineTextFieldMatcherEditor;

class ListController<E> extends AbstractBean implements ChangeListener, ListEventListener<E> {
    protected EventTableModel<E> tableModel;
    
    protected ListSource<E> listSource;
    
    protected final EventList<E> listModel;
    
    protected final SortedList<E> sortedList;
    
    protected final FilterList<E> filterList;

    protected final EventSelectionModel<E> selectionModel;

    protected final AddAction addAction;
    
    protected final RemoveAction removeAction;
    
    protected boolean autoFetch = true;
    
    public ListController(EventList<E> listModel) {
        if (listModel == null) {
            listModel = createModelEventList();
        }
        this.listModel = listModel;

        sortedList = new SortedList<E>(listModel, null);
        sortedList.addListEventListener(this);
        
        filterList = new FilterList<E>(sortedList);
        filterList.addListEventListener(this);
        
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
    
    public void setListSource(ListSource<E> newSource) {
        listModel.clear();
        
        this.listSource = newSource;
        
        if (newSource != null && autoFetch) {
            fetch();
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
    
    @Override
    public void listChanged(ListEvent<E> listChanges) {
        if (listChanges.getSource() == sortedList) {
            if (listChanges.isReordering()) {
                
            }
        }
        else if (listChanges.getSource() == filterList) {
            
        }
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
    
    
    // jOOq
    public void fetch() {
        if (listSource == null) {
            throw new IllegalStateException("need listSource to fetch");
        }
        
        List<E> list = retrieve();
        setSource(list);
    }
    
    protected List<E> retrieve() {
        return listSource.getAllElements();
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
    
    public void add(int index, E element) {
        if (listSource != null) {
            listSource.save(element);
        }
        else {
            listModel.add(index, element);
        }
    }

    protected E createElement() {
        if (listSource != null) {
            return listSource.newElement();
        }
        
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
        E element = listModel.remove(index);
        
        if (listSource != null) {
            listSource.delete(element);
        }
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
    
    public void removeAll() {
        // TODO
    }
    
    protected EventList<E> createModelEventList() {
        return new BasicEventList<E>();
    }
    
    @Override
    public void stateChanged(ChangeEvent e) {
        if (listSource != null && listSource == e.getSource()) {
            fetch();
        }
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
    
//    public static class TextMatcherEditorController<E> implements ChangeListener {
//        protected final SearchEngineTextMatcherEditor<E> matcherEditor;
//
//        private ValueModel<String> filterText;
//        
//        public TextMatcherEditorController(SearchEngineTextMatcherEditor<E> matcherEditor) {
//            super();
//            this.matcherEditor = matcherEditor;
//        }
//        
//        public void setFilterText(ValueModel<String> newModel) {
//            if (this.filterText != null) {
//                this.filterText.removeChangeListener(this);
//            }
//            this.filterText = newModel;
//            
//            if (newModel != null) {
//                newModel.addChangeListener(this);
//            }
//        }
//
//        public void refilter(String inputText) {
//            matcherEditor.refilter(inputText);
//        }
//        
//        protected void filterTextChanged() {
//            String text = filterText.get();
//            refilter(text);
//        }
//        
//        @Override
//        public void stateChanged(ChangeEvent e) {
//            if (e.getSource() == filterText) {
//                filterTextChanged();
//            }
//        }
//
//        public void setFilterator(TextFilterator<? super E> filterator) {
//            matcherEditor.setFilterator(filterator);
//        }
//    }
    
    
    public static class ListArrrangingController<E> {
        protected final EventList<E> sourceList;
        
        protected final SortedList<E> sortedList;
        
        protected final FilterList<E> filterList;
        
        
        
        
        public ListArrrangingController(EventList<E> sourceList) {
            this.sourceList = sourceList;
            
            sortedList = new SortedList<E>(sourceList, null);
            
            filterList = new FilterList<E>(sortedList);
            
        }

        public EventList<E> getSourceList() {
            return sourceList;
        }
        
        public EventList<E> getArrangedList() {
            return filterList;
        }
        
        public Comparator<? super E> getComparator() {
            return sortedList.getComparator();
        }

        public void setComparator(Comparator<? super E> comparator) {
            sortedList.setComparator(comparator);
        }

        protected void setMatcher(Matcher<? super E> matcher) {
            filterList.setMatcher(matcher);
        }

        protected void setMatcherEditor(MatcherEditor<? super E> editor) {
            filterList.setMatcherEditor(editor);
        }
        
        public SearchEngineTextMatcherEditor<E> bindFilterTextField(
                JTextField field, TextFilterator<? super E> textFilterator) {
            return new SearchEngineTextFieldMatcherEditor<E>(field, textFilterator);
        }
    }
}
