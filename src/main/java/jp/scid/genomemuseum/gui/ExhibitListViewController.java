package jp.scid.genomemuseum.gui;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jp.scid.genomemuseum.gui.ExhibitDataLoader.LoadResult;
import jp.scid.genomemuseum.gui.ExhibitDataLoader.MuseumExhibitLoadTask;
import jp.scid.genomemuseum.gui.transfer.ExhibitTransferHandler;
import jp.scid.genomemuseum.model.ExhibitLibrary;
import jp.scid.genomemuseum.model.ExhibitListModel;
import jp.scid.genomemuseum.model.FreeExhibitCollectionModel;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumExhibit.FileType;
import jp.scid.genomemuseum.model.MuseumExhibitTableFormat;
import jp.scid.gui.control.ColumnOrderStatementHandler;
import jp.scid.gui.control.SortedListComparator;
import jp.scid.gui.control.TextComponentTextConnector;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor;

public class ExhibitListViewController extends ListController<MuseumExhibit> implements ChangeListener {
    private final static Logger logger = LoggerFactory.getLogger(ExhibitListViewController.class);
    
    private String filterText = "";
    
    final MuseumExhibitTableFormat tableFormat;
    
    final BindingSupport bindings = new BindingSupport(this);
    
    // Models
    protected ExhibitListModel exhibitListModel = null;
    
    protected final SortedListComparator<MuseumExhibit> sortedListComparator;
    
    final TextFilterator<? super MuseumExhibit> textFilterator;
    
    // Controllers
    final ExhibitTransferHandler transferHandler = new ExhibitTransferHandler(this);
    
    protected final ColumnOrderStatementHandler<MuseumExhibit> orderStatementHandler;
    
    private final SearchEngineTextMatcherEditor<MuseumExhibit> textMatcherEditor;

    protected ExhibitDataLoader bioFileLoader = null;

    
    public ExhibitListViewController() {
        tableFormat = new MuseumExhibitTableFormat();
        
        // sorting
        ValueModel<Comparator<? super MuseumExhibit>> comparator = ValueModels.newNullableValueModel();
        sortedListComparator = new SortedListComparator<MuseumExhibit>(sortedList);
        sortedListComparator.setModel(comparator);
        
        // filtering
        textFilterator = new ExhibitTextFilterator();
        textMatcherEditor = new SearchEngineTextMatcherEditor<MuseumExhibit>(textFilterator);
        filterList.setMatcherEditor(textMatcherEditor);
        
        orderStatementHandler = new ColumnOrderStatementHandler<MuseumExhibit>(comparator, tableFormat);
    }
    
    // bioFileLoader
    public ExhibitDataLoader getBioFileLoader() {
        return bioFileLoader;
    }
    
    public void setBioFileLoader(ExhibitDataLoader bioFileLoader) {
        this.bioFileLoader = bioFileLoader;
    }
    
    // exhibitListModel
    public ExhibitListModel getExhibitListModel() {
        return exhibitListModel;
    }
    
    public void setExhibitListModel(ExhibitListModel exhibitListModel) {
        getSource().clear();
        
        this.exhibitListModel = exhibitListModel;
        
        if (exhibitListModel != null) {
            fetch();
        }
    }
    
    // Filter text
    public String getFilterText() {
        return filterText;
    }
    
    public void setFilterText(String newText) {
        textMatcherEditor.refilter(newText);
        firePropertyChange("filterText", this.filterText, this.filterText = newText);
    }
    
    public boolean addExhibitFromFile(File file) {
        FileType fileType;
        try {
            fileType = bioFileLoader.findFileType(file);
        }
        catch (IOException e) {
            logger.info("cannot load file " + file, e);
            fileType = FileType.UNKNOWN;
        }
        
        boolean accepted;
        
        if (fileType != FileType.UNKNOWN) {
            MuseumExhibitLoadTask<?> task = bioFileLoader.executeLoading(file, fileType);
            MuseumExhibit exhibit = task.getMuseumExhibit();
            add(exhibit);
            accepted = true;
        }
        else {
            accepted = false;
        }
        
        return accepted;
    }
    
    public void fetch() {
        List<MuseumExhibit> list = getExhibitListModel().fetchExhibits();
        
        setSource(list);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (getExhibitListModel() != null) {
            fetch();
        }
    }
    
    @Override
    public boolean canAdd() {
        return getExhibitListModel() instanceof FreeExhibitCollectionModel
                || getExhibitListModel() instanceof ExhibitLibrary;
    }
    
    @Override
    public void add(int index, MuseumExhibit exhibit) {
        int count = listModel.size();
        super.add(count, exhibit);
        
        boolean executed = exhibitListModel.storeExhibit(exhibit);
        if (!executed) {
            logger.warn("element is not persisted: {}", exhibit);
        }

        if (exhibitListModel instanceof FreeExhibitCollectionModel) {
            move(new int[]{count}, index);
            // TODO move index
            ((FreeExhibitCollectionModel) exhibitListModel).addExhibit(index, exhibit);
        }
    }
    
    @Override
    public void move(int[] indices, int dest) {
        if (exhibitListModel instanceof FreeExhibitCollectionModel) {
            super.move(indices, dest);
            
            // TODO
        }
    }
    
    @Override
    public void removeAt(int index) {
        MuseumExhibit exhibit = getTransformedElements().remove(index);
        
        if (exhibitListModel instanceof ExhibitLibrary) {
            // TODO alert
            ((ExhibitLibrary) exhibitListModel).deleteExhibit(exhibit);
        }
        else if (exhibitListModel instanceof FreeExhibitCollectionModel) {
            // TODO alert
            ((FreeExhibitCollectionModel) exhibitListModel).deleteExhibit(exhibit);
        }
        else {
            logger.warn("element is not removed");
        }
    }
    
    @Override
    public void bindTable(JTable table) {
        super.bindTable(table);
        
        table.setFocusable(true);
        table.getInputMap().put(KeyStroke.getKeyStroke('+'), "add");
        table.getInputMap().put(KeyStroke.getKeyStroke('-'), "delete");
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete");
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(transferHandler);
        table.setDragEnabled(true);
        
        if (table.getParent() instanceof JComponent) {
            ((JComponent) table.getParent()).setTransferHandler(transferHandler);
        }
        
        if (table.getTableHeader() != null) {
            orderStatementHandler.bindTableHeader(table.getTableHeader());
        }
    }
    
    public TextComponentTextConnector bindFilterTextField(JTextField field) {
        TextComponentTextConnector connector = bindings.bindText("filterText").toText(field);
        connector.listenEditingTo(field.getDocument());
        return connector;
    }
    
    @Override
    public TableFormat<MuseumExhibit> getTableFormat() {
        return tableFormat;
    }
    
    class LoadedDataUpdateHandler extends SwingWorker<MuseumExhibit, Void> {
        private final SwingWorker<LoadResult, ?> task;
        private final ExhibitListModel model;
        
        public LoadedDataUpdateHandler(SwingWorker<LoadResult, ?> task, ExhibitListModel model) {
            this.task = task;
            this.model = model;
        }
        
        @Override
        protected MuseumExhibit doInBackground() throws Exception {
            return task.get().exhibit();
        }
        
        @Override
        protected void done() {
            if (isCancelled())
                return;
            
            MuseumExhibit exhibit;
            try {
                exhibit = get();
            }
            catch (InterruptedException e) {
                logger.warn("loading failure", e);
                return;
            }
            catch (ExecutionException e) {
                logger.warn("loading failure", e);
                return;
            }
            
            model.storeExhibit(exhibit);
            elementChanged(exhibit);
        }
    }
    
    static class ExhibitTextFilterator implements TextFilterator<MuseumExhibit> {
        @Override
        public void getFilterStrings(List<String> baseList, MuseumExhibit element) {
            baseList.add(element.getAccession());
            baseList.add(element.getDefinition());
            baseList.add(element.getName());
            baseList.add(element.getNamespace());
            baseList.add(element.getOrganism());
            baseList.add(element.getSourceText());
        }
    }
}