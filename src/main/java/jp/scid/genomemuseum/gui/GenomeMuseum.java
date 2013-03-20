package jp.scid.genomemuseum.gui;

import java.awt.FileDialog;
import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;

import jp.scid.bio.store.FileLibrary;
import jp.scid.bio.store.LibrarySchemaManager;
import jp.scid.bio.store.SequenceLibrary;
import jp.scid.genomemuseum.gui.FolderDirectoryTreeController.GenomeMuseumTreeSource;
import jp.scid.genomemuseum.model.PersistentGeneticSequenceLibrary;
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
    
    private final LibrarySchemaManager schemaManager;
    
    private MainFrameController mainFrameController;
    private GeneticSequenceListController geneticSequenceListController;
    private FolderDirectoryTreeController folderDirectoryTreeController;
    
    private ExecutorService taskExecutor;
    
    // actions
    public GenomeMuseum() {
        getContext().getResourceManager().setResourceFolder(null);
        
        schemaManager = new LibrarySchemaManager();

        mainFrameController = new MainFrameController();
        geneticSequenceListController = new GeneticSequenceListController();
        folderDirectoryTreeController = new FolderDirectoryTreeController();
    }

    private void openSchema() throws SQLException {
        LibrarySchemaManager manager = schemaManager;
        
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
    }
    
    @Override
    protected void initialize(String[] args) {
        logger.debug("initialize with args: {}", Arrays.asList(args));
        
        taskExecutor = Executors.newSingleThreadExecutor();
        
        MainView mainView = new MainView();
        MainMenuBar mainMenuBar = createMainMenuBar();
        
        // main frame
        JFrame mainFrame = createMainFrame(mainView.getContentPane(), mainMenuBar.getMenuBar());
        mainFrameController.setView(mainFrame);
        
        FileDialog openFileDialog = createOpenFileDialog(mainFrame);
        geneticSequenceListController.setFileDialog(openFileDialog);
        
        GeneticSequenceListController.Binding listBinding =
                new GeneticSequenceListController.Binding(geneticSequenceListController);
        listBinding.bindTable(mainView.exhibitListView.dataTable);
        listBinding.bindSearchEngineTextField(mainView.quickSearchField, false);
        
        FolderDirectoryTreeController.Binding treeBinding =
                new FolderDirectoryTreeController.Binding(folderDirectoryTreeController);
        treeBinding.bindTree(mainView.sourceList);
    }

    @Override
    protected void startup() {
        java.util.logging.Logger.getLogger(getClass().toString()).fine("test");
        logger.debug("startup");
        
        try {
            openSchema();
        }
        catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        
        // sequenceModel
        SequenceLibrary sequenceLibrary = schemaManager.createSequenceLibrary();
        PersistentGeneticSequenceLibrary senquenceModel =
                new PersistentGeneticSequenceLibrary(sequenceLibrary);
        senquenceModel.setFileLibrary(createFileLibrary());
        
        geneticSequenceListController.setModel(senquenceModel);
        
        // tree model
        GenomeMuseumTreeSource treeSource = new GenomeMuseumTreeSource(sequenceLibrary);
        folderDirectoryTreeController.setTreeSource(treeSource);
        
        showMainFrame();
    }

    protected FileLibrary createFileLibrary() {
        File filesDir = new File(getContext().getLocalStorage().getDirectory(),
                LOCAL_FILES_DIRECTORY_NAME);
        FileLibrary fileLibrary = FileLibrary.newFileLibrary(filesDir);
        return fileLibrary;
    }
    
    public void showMainFrame() {
        mainFrameController.show();
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

    FileDialog createOpenFileDialog(JFrame parent) {
        FileDialog dialog = new FileDialog(parent, "");
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
        
//        bindButtonAction(mainMenuBar.open, "open");
        bindButtonAction(mainMenuBar.quit, "quit");
        
        return mainMenuBar;
    }
    
    void bindButtonAction(AbstractButton menuItem, String actionName) {
        Action buttonAction = getContext().getActionMap(this).get(actionName);
        menuItem.setAction(buttonAction);
    }
}


