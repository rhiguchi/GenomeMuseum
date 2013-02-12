package jp.scid.genomemuseum.gui;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;

import jp.scid.bio.store.FileLibrary;
import jp.scid.bio.store.LibrarySchemaManager;
import jp.scid.bio.store.SequenceLibrary;
import jp.scid.genomemuseum.gui.GeneticSequenceListController.PersistentGeneticSequenceLibrary;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;
import jp.scid.genomemuseum.view.MainMenuBar;
import jp.scid.genomemuseum.view.MainView;

import org.jdesktop.application.Application;
import org.jdesktop.application.ProxyActions;
import org.jooq.Field;
import org.jooq.UpdatableTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.gui.AdvancedTableFormat;

@ProxyActions({"selectAll", "deselect"})
public class GenomeMuseum extends Application {
    private final static Logger logger = LoggerFactory.getLogger(GenomeMuseum.class);
    private static final String DATABASE_LOCAL_DIRECTORY = "schema";
    private static final String LOCAL_FILES_DIRECTORY_NAME = "Files";
    
    private LibrarySchemaManager schemaManager = null;
    FileLibrary fileLibrary;
    
    private FileDialog openFileDialog;
    
    private JFrame mainFrame;
    private GeneticSequenceListController geneticSequenceListController;
    private MuseumSourceListController sourceListController;
    
    private ExecutorService taskExecutor;
    
    // actions
    final Action openAction = new AbstractAction("open") {
        @Override
        public void actionPerformed(ActionEvent e) {
            open();
        }
    };
    
    public GenomeMuseum() {
        getContext().getResourceManager().setResourceFolder(null);
    }

    private LibrarySchemaManager openSchema() throws SQLException {
        LibrarySchemaManager manager = new LibrarySchemaManager();
        
        manager.setDatabaseUser("genomemuseum");
        // addr
        File databasePath =
                new File(getContext().getLocalStorage().getDirectory(),
                        DATABASE_LOCAL_DIRECTORY);
        databasePath.getParentFile().mkdirs();
        String namespace = databasePath.getPath() + ";AUTO_SERVER=TRUE";
        manager.setDatabaseNamespace(namespace);
        
        logger.info("try to open library schema from {}", namespace);
        manager.open();
        if (!manager.isSchemaReady())  {
            logger.info("schema setup");
            manager.setUpSchema();
        }
        
        return manager;
    }
    
    @Override
    protected void initialize(String[] args) {
        logger.debug("initialize with args: {}", Arrays.asList(args));
        
        taskExecutor = Executors.newSingleThreadExecutor();
        initViews();
    }
    
    protected void initViews() {
        openFileDialog = createOpenFileDialog();
        MainView mainView = new MainView();
        MainMenuBar mainMenuBar = createMainMenuBar();
        mainFrame = createMainFrame(mainView.getContentPane(), mainMenuBar.getMenuBar());
        
        // elements
        
        geneticSequenceListController = new GeneticSequenceListController();
        GeneticSequenceListController.Binding listBinding =
                new GeneticSequenceListController.Binding(geneticSequenceListController);
        listBinding.bindTable(mainView.exhibitListView.dataTable);
        listBinding.bindSearchEngineTextField(mainView.quickSearchField, false);
        
        sourceListController = new MuseumSourceListController();
        sourceListController.bindTree(mainView.sourceList);
    }

    @Override
    protected void startup() {
        java.util.logging.Logger.getLogger(getClass().toString()).fine("test");
        logger.debug("startup");
        
        try {
            schemaManager = openSchema();
        }
        catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        
        // sequenceModel
        SequenceLibrary sequenceLibrary = schemaManager.createSequenceLibrary();
        PersistentGeneticSequenceLibrary senquenceModel = new PersistentGeneticSequenceLibrary(sequenceLibrary);
        
        // file lib
        File filesDir = new File(getContext().getLocalStorage().getDirectory(),
                LOCAL_FILES_DIRECTORY_NAME);
        fileLibrary = FileLibrary.newFileLibrary(filesDir);
        
        geneticSequenceListController.setModel(senquenceModel);
        geneticSequenceListController.setFileLibrary(fileLibrary);
        
        // bindings
        
        
        showMainFrame();
    }
    
    public void showMainFrame() {
        mainFrame.setVisible(true);
    }

    public void open() {
        openFileDialog.setVisible(true);
        
        if (openFileDialog.getFile() == null) {
            return;
        }
        
        File file = new File(openFileDialog.getDirectory(), openFileDialog.getFile());
        geneticSequenceListController.importFromFile(file);
    }

    @Override
    protected void shutdown() {
        if (schemaManager != null) {
            schemaManager.close();
        }
        
        if (taskExecutor != null) {
            taskExecutor.shutdownNow();
        }
    }
    
    JFrame createMainFrame(JComponent contentPane, JMenuBar menuBar) {
        JFrame mainFrame = new JFrame();
        mainFrame.setContentPane(contentPane);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.pack();
        mainFrame.setLocationByPlatform(true);
        mainFrame.setLocationRelativeTo(null);
        return mainFrame;
    }

    FileDialog createOpenFileDialog() {
        FileDialog dialog = new FileDialog(mainFrame, "");
        dialog.setModal(true);
        dialog.setMode(FileDialog.LOAD);
        
        return dialog;
    }

    private MainMenuBar createMainMenuBar() {
        MainMenuBar mainMenuBar = new MainMenuBar();
        bindButtonAction(mainMenuBar.cut, "cut");
        bindButtonAction(mainMenuBar.copy, "copy");
        bindButtonAction(mainMenuBar.paste, "paste");
        bindButtonAction(mainMenuBar.selectAll, "selectAll");
        bindButtonAction(mainMenuBar.deselect, "deselect");
        bindButtonAction(mainMenuBar.delete, "delete");
        
        bindButtonAction(mainMenuBar.open, "open");
        bindButtonAction(mainMenuBar.quit, "quit");
        
        return mainMenuBar;
    }
    
    void bindButtonAction(AbstractButton menuItem, String actionName) {
        Action buttonAction = getContext().getActionMap(this).get(actionName);
        menuItem.setAction(buttonAction);
    }
    

    static class ExhibitTableFormat implements AdvancedTableFormat<MuseumExhibitRecord> {
        protected final UpdatableTable<MuseumExhibitRecord> table;
        
        public ExhibitTableFormat(UpdatableTable<MuseumExhibitRecord> table) {
            super();
            this.table = table;
        }

        @Override
        public int getColumnCount() {
            return getFields().size();
        }

        List<Field<?>> getFields() {
            return table.getFields();
        }

        @Override
        public String getColumnName(int column) {
            return getField(column).getName();
        }

        Field<?> getField(int column) {
            return getFields().get(column);
        }

        @Override
        public Object getColumnValue(MuseumExhibitRecord baseObject, int column) {
            return baseObject.getValue(getField(column), Object.class);
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return getField(column).getType();
        }

        @Override
        public Comparator<?> getColumnComparator(int column) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}


