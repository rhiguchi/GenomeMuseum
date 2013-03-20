package jp.scid.gui.control;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.impl.filter.StringTextFilterator;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;

public class EventListController<E, M extends ValueModel<? extends List<E>>> extends AbstractController<M> {
    /** source list */
//    private final EventList<E> source;
    
    /** filter list */
    private final FilterList<E> filteredSource;
    
    private final SearchEngineTextMatcherEditor<E> textMatcherEditor =
            new SearchEngineTextMatcherEditor<E>(new StringTextFilterator<E>());
    
    private final TextMatcherEditorRefilterator<E> refilterator =
            new TextMatcherEditorRefilterator<E>(textMatcherEditor);
    
    private final StringPropertyBinder searchText = new StringPropertyBinder();
    
    /** sorted list */
    private final SortedList<E> sortedSource;
    
    private final SortedListComparator<E> elementComparator;

    private final EventSelectionModel<E> selectionModel;
    
    private final ValueModel<Comparator<? super E>> comparator = ValueModels.newNullableValueModel();
    
    private EventTableModel<E> tableModel = null;
    
    // controller
    private TransferHandler transferHandler = null;
    
    // Action
    private final RemoveAction removeAction = new RemoveAction("Remove");
    
    // model sync
    private EventListSync<E> syncManager = null;

    public EventListController(EventList<E> baseList) {
        if (baseList == null) throw new IllegalArgumentException("sourceList must not be null");
        
        syncManager = new EventListSync<E>(baseList);
        
        sortedSource = new SortedList<E>(baseList, null);
        
        elementComparator = new SortedListComparator<E>(sortedSource);
        elementComparator.setModel(comparator);
        
        filteredSource = new FilterList<E>(sortedSource, textMatcherEditor);
        refilterator.setModel(searchText.getValueModel());
        
        selectionModel = new EventSelectionModel<E>(filteredSource);
        selectionModel.addListSelectionListener(removeAction);
    }
    
    public EventListController() {
        this(new BasicEventList<E>());
    }

    public EventList<E> getSourceList() {
        return syncManager.getModelList();
    }
    
    public EventList<E> getArrangedList() {
        return filteredSource;
    }

    public EventSelectionModel<E> getSelectionModel() {
        return selectionModel;
    }

    public EventList<E> getSelectedElements() {
        return getSelectionModel().getSelected();
    }
    
    public void setFilterator(TextFilterator<? super E> filterator) {
        textMatcherEditor.setFilterator(filterator);
    }
    
    public TableFormat<? super E> createTableFormat() {
        return GlazedLists.<E>tableFormat(new String[]{"class"}, new String[]{"class"});
    }
    
    @SuppressWarnings("unchecked")
    public void setTableFormat(TableFormat<? super E> tableFormat) {
        getTableModel().setTableFormat((TableFormat<E>) tableFormat);
    }
    
    public EventTableModel<E> getTableModel() {
        if (tableModel == null) {
            tableModel = createTableModel();
        }
        return tableModel;
    }
    
    public void remove() {
        selectionModel.getSelected().clear();
    }
    
    public void removeAtTransformedListIndex(int index) {
        getArrangedList().remove(index);
    }
    
    protected EventTableModel<E> createTableModel() {
        EventTableModel<E> tableModel = new EventTableModel<E>(getArrangedList(), createTableFormat());
        return tableModel;
    }

    @Override
    protected void processPropertyChange(M model, String property) {
        if (property == null || property.equals("value")) {
            final EventList<E> list;
            syncManager.deafSyncEventList();
            
            if (model.getValue() instanceof EventList) {
                list = (EventList<E>) model.getValue();
            }
            else {
                list = GlazedLists.eventList(model.getValue());
            }
            syncManager.setSyncEventList(list);
        }
    }
    
    protected void setMatcher(Matcher<? super E> matcher) {
        filteredSource.setMatcher(matcher);
    }

    protected void setMatcherEditor(MatcherEditor<? super E> editor) {
        filteredSource.setMatcherEditor(editor);
    }
    
    
    /**
     * @deprecated use {@link TextMatcherEditorRefilterator#TextMatcherEditorRefilterator(TextFilterator)}
     */
    @Deprecated
    public void bindSearchTextField(JTextField field) {
        searchText.bindTextField(field, field.getDocument());
    }
    
    public void bindTable(JTable table) {
        EventTableModel<E> eventTableModel = getTableModel();
        table.setModel(eventTableModel);
        table.setSelectionModel(getSelectionModel());
        table.setTransferHandler(getTransferHandler());
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.getActionMap().put("delete", removeAction);
        
        if (table.getParent() instanceof JComponent) {
            ((JComponent) table.getParent()).setTransferHandler(getTransferHandler());
        }
        
        if (table.getTableHeader() != null && eventTableModel.getTableFormat() instanceof AdvancedTableFormat) {
            bindTableHeader(table.getTableHeader(), (AdvancedTableFormat<? super E>) eventTableModel.getTableFormat());
        }
    }
    
    /**
     * 
     * @param table
     * @param tableFormat
     * @deprecated use {@link #bindTable(JTable)} and {@link #setTableFormat(TableFormat)} 
     */
    @Deprecated
    public void bindTable(JTable table, TableFormat<? super E> tableFormat) {
        setTableFormat(tableFormat);
        bindTable(table);
    }
    
    public void bindTableHeader(JTableHeader tableHeader, AdvancedTableFormat<? super E> tableFormat) {
        ColumnOrderStatementHandler<?> handler =
                new ColumnOrderStatementHandler<E>(comparator, tableFormat);
        handler.bindTableHeader(tableHeader);
    }
    
    // Transferring
    public synchronized TransferHandler getTransferHandler() {
        if (transferHandler == null) {
            transferHandler = new ObjectControllerTransferHandler();
        }
        
        return transferHandler;
    }
    
    // Transfer
    public boolean addElement(E element) {
        return addElements(Collections.singleton(element));
    }
    
    public boolean addElements(Collection<E> elements) {
        EventList<E> list = getArrangedList();
        list.getReadWriteLock().readLock().lock();
        try {
            list.addAll(elements);
        }
        finally {
            list.getReadWriteLock().readLock().unlock();
        }
        
        return !elements.isEmpty();
    }
    
    public boolean addElementsFromFiles(List<File> files) {
        return false;
    }

    class RemoveAction extends AbstractAction implements ListSelectionListener {
        public RemoveAction(String name) {
            super(name);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            remove();
        }

        public void listenTo(ListSelectionModel model) {
            model.addListSelectionListener(this);
            updateEnability(model);
        }
        
        void updateEnability(ListSelectionModel model) {
            setEnabled(!model.isSelectionEmpty());
        }
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel model = (ListSelectionModel) e.getSource();
            updateEnability(model);
        }
    }
}

class EventListSync<E> implements ListEventListener<E> {
    private final EventList<E> list;
    
    private EventList<E> syncEventList = null;
    
    private ListEventListener<E> syncEventListener = null;
    
    private boolean sourceChange = false;;
    
    private boolean modelChange = false;;
    
    public EventListSync(EventList<E> list) {
        if (list == null) throw new IllegalArgumentException("list must not be null");
        this.list = list;
        list.addListEventListener(this);
    }
    
    public EventList<E> getModelList() {
        return syncEventList != null ? syncEventList : list;
    }
    
    public void setSyncEventList(EventList<E> source) {
        deafSyncEventList();
        
        ListEventListener<E> baseListener = GlazedLists.syncEventListToList(source, list);
        source.removeListEventListener(baseListener);
        
        syncEventListener = new PropagationProxy(baseListener);
        source.addListEventListener(syncEventListener);
        
        syncEventList = source;
    }
    
    void deafSyncEventList() {
        if (syncEventList != null) {
            syncEventList.removeListEventListener(syncEventListener);
        }
        syncEventList = null;
        syncEventListener = null;
    }
    
    @Override
    public void listChanged(ListEvent<E> listChanges) {
        if (sourceChange || syncEventList == null)
            return;
        
        syncEventList.getReadWriteLock().writeLock().lock();
        modelChange = true;
        
        try {
            while (listChanges.next()) {
                int index = listChanges.getIndex();
                int type = listChanges.getType();
                
                if (type == ListEvent.INSERT) {
                    E inserted = listChanges.getSourceList().get(index);
                    syncEventList.add(index, inserted);
                }
                else if (type == ListEvent.DELETE) {
                    syncEventList.remove(index);
                }
                else if (type == ListEvent.UPDATE) {                       
                    E updated = listChanges.getSourceList().get(index);
                    syncEventList.set(index, updated);
                }
            }
        }
        finally {
            modelChange = false;
            syncEventList.getReadWriteLock().writeLock().unlock();
        }
    }
    
    class PropagationProxy implements ListEventListener<E> {
        private final ListEventListener<E> adaptee;
        
        public PropagationProxy(ListEventListener<E> adaptee) {
            this.adaptee = adaptee;
        }
        
        @Override
        public void listChanged(ListEvent<E> listChanges) {
            if (modelChange)
                return;
            
            sourceChange = true;
            try {
                adaptee.listChanged(listChanges);
            }
            finally {
                sourceChange = false;
            }
        }
    }
}

