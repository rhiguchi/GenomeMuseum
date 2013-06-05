package jp.scid.genomemuseum.gui;

import java.awt.FileDialog;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;

import jp.scid.bio.store.ConnectionBuilder;
import jp.scid.bio.store.SequenceLibrary;
import jp.scid.genomemuseum.model.GeneticSequenceCollection;
import jp.scid.genomemuseum.model.GeneticSequenceCollections;
import jp.scid.genomemuseum.model.MuseumTreeSource;
import jp.scid.genomemuseum.view.MainMenuBar;
import jp.scid.genomemuseum.view.MainView;

import org.h2.jdbcx.JdbcConnectionPool;
import org.jdesktop.application.Application;
import org.jdesktop.application.ProxyActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProxyActions({"selectAll", "deselect"})
public class GenomeMuseum extends Application {
    private final static Logger logger = LoggerFactory.getLogger(GenomeMuseum.class);
    private static final String DATABASE_LOCAL_DIRECTORY = "schema";
    private static final String LOCAL_FILES_DIRECTORY_NAME = "Files";
    
    private JdbcConnectionPool connectionPool;
    
    private MainFrameController mainFrameController;
    private GeneticSequenceListController geneticSequenceListController;
    private FolderTreeController folderDirectoryTreeController;
    private FileOpenHandler fileOpenHandler;
    
    private ExecutorService taskExecutor;
    
    // actions
    public GenomeMuseum() {
        getContext().getResourceManager().setResourceFolder(null);
        
        mainFrameController = new MainFrameController();
        geneticSequenceListController = new GeneticSequenceListController();
        folderDirectoryTreeController = new FolderTreeController();
        fileOpenHandler = new FileOpenHandler(geneticSequenceListController);
    }

    private void openConnectionPool() {
        if (connectionPool != null) {
            throw new IllegalArgumentException("connection already open");
        }
        
        ConnectionBuilder connbuilder = new ConnectionBuilder();
        connbuilder.databaseUser("genomemuseum");
        
        // addr
        File databasePath =
                new File(getContext().getLocalStorage().getDirectory(),
                        DATABASE_LOCAL_DIRECTORY);
        databasePath.getParentFile().mkdirs();
        String namespace = databasePath.getPath() + ";AUTO_SERVER=TRUE";
        connbuilder.databaseNamespace(namespace);
        
        connectionPool = connbuilder.build();
        logger.info("database stores at {}", databasePath);
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
        fileOpenHandler.setFileDialog(openFileDialog);
        fileOpenHandler.bindOpenMenu(mainMenuBar.open);
        
        GeneticSequenceListController.Binding listBinding =
                geneticSequenceListController.new Binding();
        listBinding.bindTable(mainView.exhibitListView.dataTable);
        listBinding.bindSearchEngineTextField(mainView.quickSearchField, false);
        listBinding.bindTreeSelectionChange(folderDirectoryTreeController);
        
        folderDirectoryTreeController.bindTree(mainView.sourceList);
        folderDirectoryTreeController.bindBasicFolderAddButton(mainView.addBoxFolder);
        folderDirectoryTreeController.bindGroupFolderAddButton(mainView.addListBox);
        folderDirectoryTreeController.bindFilterFolderAddButton(mainView.addSmartBox);
        folderDirectoryTreeController.bindFolderRemoveButton(mainView.removeBoxButton);
    }

    @Override
    protected void startup() {
        logger.debug("startup");
        
        // sequenceModel
        SequenceLibrary sequenceLibrary = SequenceLibrary.create(getConnection());
        File filesDir = getFilesStoreDirectory();
        sequenceLibrary.setFilesStoreRoot(filesDir);
        logger.info("sequence files store at {}", filesDir);
        
        // tree model
        MuseumTreeSource treeSource = new MuseumTreeSource();
        treeSource.setSequenceLibrary(sequenceLibrary);
        folderDirectoryTreeController.setModel(treeSource);
        
        showMainFrame();
    }

    private Connection getConnection() {
        openConnectionPool();
        Connection connection;
        try {
            connection = connectionPool.getConnection();
        }
        catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return connection;
    }

    File getFilesStoreDirectory() {
        File filesDir = new File(getContext().getLocalStorage().getDirectory(),
                LOCAL_FILES_DIRECTORY_NAME);
        return filesDir;
    }
    
    public void showMainFrame() {
        mainFrameController.show();
    }

    @Override
    protected void shutdown() {
        if (connectionPool != null) {
            connectionPool.dispose();
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


