package jp.scid.genomemuseum.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jp.scid.genomemuseum.gui.ExhibitDataLoader.LoadResult;
import jp.scid.genomemuseum.model.ExhibitCollectionModel;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
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
    
    boolean isModelAppendable() {
        return getExhibitListModel() instanceof FreeExhibitCollectionModel
                || getExhibitListModel() instanceof ExhibitLibrary;
    }
    
    MuseumExhibit createExhibit(File file) {
        MuseumExhibit exhibit = getBioFileLoader().newMuseumExhibit(file.toURI());
        try {
            getBioFileLoader().updateFileFormat(exhibit);
        }
        catch (IOException e) {
            logger.info("", e);
        }
        
        return exhibit;
    }
    
    Future<?> loadExhibit(MuseumExhibit exhibit) {
        ExhibitListModel model = getExhibitListModel();
        model.storeExhibit(exhibit);
        
        SwingWorker<LoadResult, ?> task = getBioFileLoader().executeReload(exhibit);
        
        LoadedDataUpdateHandler updateHandler = new LoadedDataUpdateHandler(task, model);
        updateHandler.execute();
        
        return updateHandler;
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
        
        if (table.getParent() instanceof JComponent) {
            ((JComponent) table.getParent()).setTransferHandler(transferHandler);
        }
        
        if (table.getTableHeader() != null) {
            orderStatementHandler.bindTableHeader(table.getTableHeader());
        }
    }
    
    public TextComponentTextConnector bindFilterTextField(JTextField field) {
        TextComponentTextConnector connector = bindings.bindText("filterText").toText(field);
        return connector;
    }
    
    @Override
    public TableFormat<MuseumExhibit> getTableFormat() {
        return tableFormat;
    }
    
    static class ExhibitTransferHandler extends TransferHandler {
        ExhibitListViewController controller;
        
        public ExhibitTransferHandler(ExhibitListViewController controller) {
            this.controller = controller;
        }
        
        @Override
        public boolean canImport(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return controller.isModelAppendable();
            }
            return false;
        }
        
        @Override
        public boolean importData(TransferSupport support) {
            final boolean result;
            
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> files = listFiles(getImportFileList(support));
                int loadFileCount = 0;
                
                for (File file: files) {
                    MuseumExhibit exhibit = controller.createExhibit(file);
                    if (exhibit.getFileType() != FileType.UNKNOWN) {
                        controller.add(exhibit);
                        controller.loadExhibit(exhibit);
                        loadFileCount++;
                    }
                }
                
                result = loadFileCount > 0;
            }
            else {
                result = false;
            }
            
            return result;
        }

        @SuppressWarnings("unchecked")
        List<File> getImportFileList(TransferSupport support) {
            List<File> files = Collections.emptyList();
            try {
                files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            }
            catch (UnsupportedFlavorException e) {
                logger.warn("cannot import file", e);
            }
            catch (IOException e) {
                logger.warn("cannot import file", e);
            }
            return files;
        }
        
        List<File> listFiles(List<File> base) {
            List<File> list = new LinkedList<File>();
            
            for (File file: base) {
                if (file.isDirectory()) {
                    Collection<File> files =
                            FileUtils.listFiles(file, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE);
                    list.addAll(files);
                }
                else {
                    list.add(file);
                }
            }
            
            return list;
        }
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