package jp.scid.genomemuseum.gui;

import java.awt.FileDialog;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JMenuItem;

import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBoxService;
import jp.scid.genomemuseum.model.ExhibitFileManager;
import jp.scid.genomemuseum.model.MuseumDataSchema;
import jp.scid.genomemuseum.model.MuseumExhibitLibrary;
import jp.scid.genomemuseum.model.MuseumSourceModel;
import jp.scid.genomemuseum.model.SchemaBuilder;
import jp.scid.genomemuseum.view.MainMenuBar;
import jp.scid.genomemuseum.view.MainView;

import org.h2.jdbcx.JdbcConnectionPool;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.ProxyActions;

@ProxyActions({"selectAll", "deselect"})
public class GenomeMuseum extends Application {
    private static final String DATABASE_LOCAL_DIRECTORY = "Library";
    private static final String LOCAL_FILES_DIRECTORY_NAME = "Files";

    public enum ProgramArgument {
        USE_LOCAL_LIBRARY() {
            @Override
            public int apply(GenomeMuseum application, String[] args) {
                if (!args[0].equals("--LocalLibrary"))
                    return 0;
                
                application.useLocalLibrary = true; 
                return 1;
            }
        },
        ;
        
        public abstract int apply(GenomeMuseum application, String[] args);
    }
    // property
    private String databaseAddr = "jdbc:h2:mem:test";
    
    private final static String databaseUser = "genomemuseum";
    
    private final static String databasePassword = "";
    
    private boolean useLocalLibrary = false;
    
    MainView mainView;
    
    MainMenuBar mainMenuBar;
    
    JFrame mainFrame;
    
    FileDialog openFileDialog;
    
    // models
    private JdbcConnectionPool connectionPool = null;
    
    private MuseumDataSchema dataSchema = null;
    
    // Controllers
    private ExhibitDataLoader fileLoader;
    
    MainFrameController mainFrameController = null;
    
    public GenomeMuseum() {
    }
    
    @Override
    protected void initialize(String[] args) {
        ProgramArgument[] argumentFunctions = ProgramArgument.values();
        
        while (args.length > 0) {
            for (ProgramArgument arg: argumentFunctions) {
                int usedArg = arg.apply(this, args);
                
                if (usedArg > 0) {
                    int nextLength = args.length - usedArg;
                    if (nextLength <= 0)
                        break;
                    
                    String[] newArgs = new String[args.length - usedArg];
                    System.arraycopy(arg, usedArg, newArgs, 0, newArgs.length);
                    args = newArgs;
                }
            }
        }
    }
    
    @Override
    protected void startup() {
        // Model
        if (useLocalLibrary) {
            File databaseDir =
                    new File(getContext().getLocalStorage().getDirectory(),
                            DATABASE_LOCAL_DIRECTORY);
            
            databaseDir.mkdirs();
            
            File databaseNamespace = new File(databaseDir, "store");
            
            databaseAddr = "jdbc:h2:file:" + databaseNamespace.getPath();
        }
        try {
            initDataSchema();
        }
        catch (SQLException e) {
            throw new IllegalStateException("schema initialization failure", e);
        }
        
        // file library
        if (useLocalLibrary) {
            File filesDir = new File(getContext().getLocalStorage().getDirectory(),
                        LOCAL_FILES_DIRECTORY_NAME);
            ExhibitFileManager fileManager = new ExhibitFileManager(); 
            fileManager.setDirectory(filesDir);
            
            getFileLoader().setFileManager(fileManager);
        }
        
        CollectionBoxService boxService = dataSchema.getCollectionBoxService();
        boxService.addRootItem(BoxType.FREE);
        
        // test source
        MuseumExhibitLibrary exhibitService = getExhibitLibrary();
        exhibitService.save(exhibitService.newElement());
        exhibitService.save(exhibitService.newElement());
        exhibitService.save(exhibitService.newElement());
        
        // Controller
        MainFrameController mainFrameController = getMainFrameController();
        mainFrameController.bindFrame(getMainFrame());
        mainFrameController.bindMainView(getMainView());
        
        // add local library
        getMuseumSourceModel().setLocalLibrarySource(getExhibitLibrary());
        
        mainFrameController.showFrame();
    }
    
    @Override
    protected void shutdown() {
        if (connectionPool != null) {
            connectionPool.dispose();
        }
    }
    
    @Action
    public void open() {
        // TODO
    }

    MainFrameController getMainFrameController() {
        if (mainFrameController == null) {
            mainFrameController = new MainFrameController();
            mainFrameController.setDataSchema(getDataSchema());
            mainFrameController.setBioFileLoader(getFileLoader());
            
            addExitListener(mainFrameController);
        }
        return mainFrameController;
    }
    
    MuseumSourceModel getMuseumSourceModel() {
        return getMainFrameController().sourceListController.sourceModel;
    }
    
    // Data Models
    public Connection getConnection() throws SQLException {
        if (connectionPool == null) {
            try {
                Class.forName("org.h2.Driver");
            }
            catch (ClassNotFoundException e) {
                throw new IllegalStateException("need h2 database driber", e);
            }
            
            connectionPool = JdbcConnectionPool.create(databaseAddr, databaseUser, databasePassword);
        }
        return connectionPool.getConnection();
    }
    
    public void initDataSchema() throws SQLException {
        SchemaBuilder builder = new SchemaBuilder(getConnection());
        dataSchema = builder.getDataSchema();
    }
    
    public MuseumDataSchema getDataSchema() {
        if (dataSchema == null)
            throw new IllegalStateException("schema needs initialize before using");
        return dataSchema;
    }
    
    public MuseumExhibitLibrary getExhibitLibrary() {
        return dataSchema.getMuseumExhibitLibrary();
    }
    
    // Application Views
    public JFrame getMainFrame() {
        if (mainFrame == null){
            mainFrame = createMainFrame(getMainView());
            mainFrame.setJMenuBar(getMainMenuBar().getMenuBar());
        }
        
        return mainFrame;
    }
    
    public MainView getMainView() {
        if (mainView == null) {
            mainView = new MainView();
        }
        return mainView;
    }
    
    public MainMenuBar getMainMenuBar() {
        if (mainMenuBar == null) {
            mainMenuBar = new MainMenuBar();
            bindEditMenuItems(
                    mainMenuBar.cut, mainMenuBar.copy, mainMenuBar.paste, mainMenuBar.selectAll,
                    mainMenuBar.deselect, mainMenuBar.delete);
            bindFileMenuItems(mainMenuBar.open, mainMenuBar.quit);
        }
        return mainMenuBar;
    }
    
    public FileDialog getOpenFileDialog() {
        if (openFileDialog == null) {
            openFileDialog = new FileDialog(getMainFrame(), "", FileDialog.LOAD);
        }
        return openFileDialog;
    }
    
    // Application controllers
    public ExhibitDataLoader getFileLoader() {
        if (fileLoader == null) {
            ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
            fileLoader = new ExhibitDataLoader(taskExecutor, getExhibitLibrary());
        }
        return fileLoader;
    }
    
    // Bindings
    public void bindEditMenuItems(
            JMenuItem cut, JMenuItem copy, JMenuItem paste, JMenuItem selectAll,
            JMenuItem deselect, JMenuItem delete) {
        ApplicationActionMap actionMap = getContext().getActionMap(this);
        cut.setAction(actionMap.get("cut"));
        copy.setAction(actionMap.get("copy"));
        paste.setAction(actionMap.get("paste"));
        selectAll.setAction(actionMap.get("selectAll"));
        deselect.setAction(actionMap.get("deselect"));
        delete.setAction(actionMap.get("delete"));
    }

    public void bindFileMenuItems(
            JMenuItem open, JMenuItem quit) {
        ApplicationActionMap actionMap = getContext().getActionMap(this);
        open.setAction(actionMap.get("open"));
        quit.setAction(actionMap.get("quit"));
    }
    static JFrame createMainFrame(MainView mainView) {
        JFrame frame = new JFrame();
        frame.setContentPane(mainView.getContentPane());
        
        return frame;
    }
}
