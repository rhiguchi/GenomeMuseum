package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.ObservableElementList.Connector;
import ca.odell.glazedlists.PluggableList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.impl.filter.StringTextFilterator;
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

public abstract class ListController<E> {
    private final static Logger logger = LoggerFactory.getLogger(ListController.class);
    
    /** source */
    protected final PluggableList<E> base;
    /** sorter */
    private final SortedList<E> sortedList;
    // filtering
    /** matcher editor */
    private SearchEngineTextMatcherEditor<E> matcherEditor;
    private final FilterList<E> filterList;
    
    /** selection model */
    protected final EventSelectionModel<E> selectionModel;
    
    // properties
    private boolean canRemove = false;
    // actions
    /** remove */
    protected final Action addAction;
    protected final Action removeAction;
    
    public ListController(EventList<E> source) {
        this.base = new PluggableList<E>(source);
        this.sortedList = new SortedList<E>(this.base, null);
        
        matcherEditor = createTextMatcherEditor();
        filterList = new FilterList<E>(sortedList, matcherEditor);
        
        selectionModel = createSelectionModel(getViewList());
        
        addAction = createAddAction();
        removeAction = createRemoveAction();
        
        updateCanRemoveBySelection();
    }

    @SuppressWarnings("unused")
    private Connector<E> createObserveConnector() {
        return new EmptyConnector<E>();
    }

    public EventList<E> getViewList() {
        return filterList;
    }
    
    // adding methods
    public boolean canAdd() {
        return false;
    }

    public void add(E newElement) {
        int index = getInsertIndex();
        add(index, newElement);
    }
    
    public void addAll(Collection<E> elements) {
        int index = getInsertIndex();
        getViewList().addAll(index, elements);
    }

    public void add(int index, E newElement) {
        getViewList().add(index, newElement);
    }
    
    public E add() {
        int index = getInsertIndex();

        return add(index);
    }

    private int getInsertIndex() {
        return selectionModel.getMaxSelectionIndex() + 1;
    }

    public E add(int index) {
        E newElement = createElement();
        
        add(index, newElement);
        return newElement;
    }

    protected E createElement() {
        throw new UnsupportedOperationException("to create element must be implemented");
    }
    
    /**
     * @param file 
     * @throws IOException  
     */
    public boolean importFile(File file) throws IOException {
        logger.error("importFromFile must be implemented");
        return false;
    }
    
    // change
    public void elementChange(int index) {
        EventList<E> list = getViewList();
        list.set(index, list.get(index));
    }
 
    public void elementChanged(E exhibit) {
        EventList<E> list = getViewList();
        int index = 0;
        for (Iterator<E> ite = list.iterator(); ite.hasNext(); index++) {
            E element = ite.next();
             
            if (exhibit.equals(element)) {
                list.set(index, exhibit);
            }
        }
    }
    
    // removing methods
    public void clear() {
        base.clear();
    }

    public boolean canRemove() {
        return canRemove;
    }
    
    public void setCanRemove(boolean newValue) {
        canRemove = newValue;
        removeAction.setEnabled(newValue);
    }
    
    private void updateCanRemoveBySelection() {
        setCanRemove(!isSelectionEmpty());
    }

    // sorting
    public Comparator<? super E> getComparator() {
        return sortedList.getComparator();
    }

    public void setComparator(Comparator<? super E> comparator) {
        sortedList.setComparator(comparator);
    }
    
    // Remove
    public E removeAt(int index) {
        return getViewList().remove(index);
    }

    public boolean remove(E element) {
        int index = getViewList().indexOf(element);
        return removeAt(index) != null;
    }
    
    public void remove() {
        int minSelectionIndex = selectionModel.getMinSelectionIndex();
        int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
        
        if (minSelectionIndex < 0 || maxSelectionIndex < 0) {
            return;
        }
        
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

    public void removeAt(int... indices) {
        for (int i = indices.length - 1; i >= 0; i--) {
            int index = indices[i];
            E removed = removeAt(index);
        }
    }
    
    // selections
    protected boolean isSelectionEmpty() {
        return selectionModel.isSelectionEmpty();
    }
    
    public List<E> getSelections() {
        return new ArrayList<E>(selectionModel.getSelected());
    }

    public void setSelections(List<E> selections) {
        EventList<E> selected = selectionModel.getTogglingSelected();
        selected.clear();
        selected.addAll(selections);
    }

    public E getSelection() {
        List<E> list = getSelections();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
    
    public void setSelection(E newSelection) {
        if (newSelection == null) {
            clearSelection();
        }
        else {
            setSelections(Collections.singletonList(newSelection));
        }
    }
    
    public void clearSelection() {
        selectionModel.clearSelection();
    }
    
    // filtering
    public void refilter(String text) {
        matcherEditor.refilter(text);
    }
    
    public void setMatcherEditor(SearchEngineTextMatcherEditor<E> matcherEditor) {
        this.matcherEditor = matcherEditor;
        filterList.setMatcherEditor(matcherEditor);
    }
    
    public void setTextFilterator(TextFilterator<? super E> filterator) {
        matcherEditor.setFilterator(filterator);
    }
    
    // transferring
    protected Transferable createTransferData() {
        if (isSelectionEmpty()) {
            return null;
        }
        
        List<E> selections = getSelections();
        ListTransferData<E> data = new ListTransferData<E>(selections);
        
        List<File> files = new LinkedList<File>();
        for (E e: selections) {
            File file = getFile(e);
            if (file != null) {
                files.add(file);
            }
        }
        data.setFiles(files);
        
        return data;
    }
    
    protected URI getUri(E element) {
        logger.warn("must be implemented to get file of %s", element);
        return null;
    }
    
    protected File getFile(E element) {
        URI uri = getUri(element);
        
        if (uri == null || !"file".equals(uri.getScheme())) {
            return null;
        }
        File file = new File(uri);
        return file;
    }
    
    // factories
    protected EventSelectionModel<E> createSelectionModel(EventList<E> viewList) {
        EventSelectionModel<E> selectionModel = new EventSelectionModel<E>(viewList);
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateCanRemoveBySelection();
            }
        });
        return selectionModel;
    }
    
    SearchEngineTextMatcherEditor<E> createTextMatcherEditor() {
        return new SearchEngineTextMatcherEditor<E>(createTextFilterator());
    }

    protected TextFilterator<E> createTextFilterator() {
        return new StringTextFilterator<E>();
    }

    // Action factories
    protected Action createAddAction() {
        return new AbstractAction("Add") {
            @Override
            public void actionPerformed(ActionEvent e) {
                add();
            }
        };
    }
    
    protected Action createRemoveAction() {
        return new AbstractAction("Remove") {
            @Override
            public void actionPerformed(ActionEvent e) {
                remove();
            }
        };
    }
    
    protected TransferHandler getTransferHandler() {
        throw new UnsupportedOperationException("must be implemented for use");
    }
    
    private static class EmptyConnector<E> implements ObservableElementList.Connector<E> {
        public EmptyConnector() {
        }
        @Override
        public EventListener installListener(E element) {
            return null;
        }

        @Override
        public void uninstallListener(E element, EventListener listener) {
            // Do nothing
        }

        @Override
        public void setObservableElementList(ObservableElementList<? extends E> list) {
            // Do nothing
        }
    }

    public class Binding {
        public Binding() {
        }

        public void bindTable(JTable table, TableFormat<? super E> tableFormat) {
            DefaultEventTableModel<?> tableModel =
                    new DefaultEventTableModel<E>(getViewList(), tableFormat);
            table.setModel(tableModel);
            table.setSelectionModel(selectionModel);
            table.putClientProperty("Binding.controller", ListController.this);
        }
        
        public void bindTableTransferHandler(JTable table) {
            TransferHandler transferHandler = getTransferHandler();
            
            table.setTransferHandler(transferHandler);
            table.setDropMode(DropMode.INSERT_ROWS);
            table.setDragEnabled(true);
            table.setFocusable(true);
            
            table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
            table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete");

            table.getActionMap().put("add", addAction);
            table.getActionMap().put("delete", removeAction);
            
            if (table.getParent() instanceof JComponent) {
                ((JComponent) table.getParent()).setTransferHandler(transferHandler);
            }
        }

        public TableHeaderClickHandler.Binder bindSortableTableHeader(
                JTableHeader header, AdvancedTableFormat<? super E> tableFormat) {
            TableHeaderClickHandler.Binder binder =
                    TableHeaderClickHandler.installTo(header, sortedList, tableFormat);
            

//            orderStatementHandler = new ColumnOrderStatementHandler<MuseumExhibit>(comparator, tableFormat);
            return binder;
        }
        
        public void bindSearchEngineTextField(JTextField field, boolean incrementalSearch) {
            FilterHandler filterHandler = new FilterHandler(field);
            filterHandler.refilter();
            
            field.addActionListener(filterHandler);
            
            if (incrementalSearch) {
                field.getDocument().addDocumentListener(filterHandler);
            }
        }
        
        private class FilterHandler implements ActionListener, DocumentListener {
            private final JTextField textField;
            
            public FilterHandler(JTextField textField) {
                this.textField = textField;
            }
            
            
            public void refilter() {
                ListController.this.refilter(textField.getText());
            }
            
            public void insertUpdate(DocumentEvent e) { refilter(); }
            public void removeUpdate(DocumentEvent e) { refilter(); }
            public void changedUpdate(DocumentEvent e) { refilter(); }
            public void actionPerformed(ActionEvent e) { refilter(); }
        }
    }
}
