package jp.scid.genomemuseum.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import jp.scid.genomemuseum.model.CollectionBox;
import jp.scid.genomemuseum.model.ExhibitListModel;
import jp.scid.genomemuseum.model.MuseumDataSchema;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumSourceModel;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionBoxNode;
import jp.scid.genomemuseum.view.ExhibitListView;
import jp.scid.genomemuseum.view.MainMenuBar;
import jp.scid.genomemuseum.view.MainView;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Application.ExitListener;


public class MainFrameController extends AbstractBean
        implements ExitListener, TreeSelectionListener, PropertyChangeListener, ListSelectionListener {
    public static final String PROPKEY_FRAME_VISIBLED = "frameVisibled";

    boolean frameVisibled = false;
    
    protected MuseumDataSchema schema = null;
    
    // Properties
    
    // Controllers
    final MuseumSourceListController sourceListController;
    
    final ExhibitListViewController exhibitListViewController;
    
    final MuseumExhibitContentViewer museumExhibitContentViewer;

    final BindingSupport bindings = new BindingSupport(this);
    
    public MainFrameController() {
        sourceListController = new MuseumSourceListController();
        sourceListController.addPropertyChangeListener(this);
        
        sourceListController.getSelectionModel().addTreeSelectionListener(this);
        
        exhibitListViewController = new ExhibitListViewController();
        exhibitListViewController.getSelectionModel().addListSelectionListener(this);
        exhibitListViewController.addPropertyChangeListener(this);
        
        museumExhibitContentViewer = new MuseumExhibitContentViewer();
    }
    
    // frameVisibled
    public boolean isFrameVisibled() {
        return frameVisibled;
    }

    public void setFrameVisibled(boolean newValue) {
        firePropertyChange(PROPKEY_FRAME_VISIBLED, this.frameVisibled, this.frameVisibled = newValue);
    }
    
    // Controllers
    public ExhibitDataLoader getBioFileLoader() {
        return exhibitListViewController.getBioFileLoader();
    }

    public void setBioFileLoader(ExhibitDataLoader bioFileLoader) {
        exhibitListViewController.setBioFileLoader(bioFileLoader);
    }

    public void showFrame() {
        setFrameVisibled(true);
    }

    public void setDataSchema(MuseumDataSchema newSchema) {
        sourceListController.setCollectionBoxSource(null);
//        exhibitListViewController.setMuseumSchema(newSchema);
        
        schema = newSchema;
        
        if (newSchema != null) {
            sourceListController.setCollectionBoxSource(newSchema.getCollectionBoxService());
            sourceListController.setLocalLibrarySource(newSchema.getMuseumExhibitLibrary());
            museumExhibitContentViewer.setLibrary(newSchema.getMuseumExhibitLibrary());
        }
        
    }

    public void setExhibitListSource(ExhibitListModel newModel) {
        exhibitListViewController.setExhibitListModel(newModel);
    }
    
    public CollectionBox getSelectedSource() {
        // TODO
        return null;
    }
    
    public MuseumExhibit getSelectedExhibit() {
        return exhibitListViewController.getSelection();
    }

    // Action methods
    public void reloadExhibitDetailsView() {
        MuseumExhibit exhibit = getSelectedExhibit();
        museumExhibitContentViewer.setExhibit(exhibit);
        museumExhibitContentViewer.reload();
    }

    // Bindings
    public void bindFrame(JFrame frame) {
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setLocationRelativeTo(null);
        
        bindings.bind(PROPKEY_FRAME_VISIBLED).to(frame, "visible");
    }
    
    public void bindMainView(MainView mainView) {
        bindSourceList(mainView.sourceList);
        bindSourceListControlls(
                mainView.addListBox, mainView.addBoxFolder, mainView.addSmartBox,
                mainView.removeBoxButton);

        exhibitListViewController.bindTable(mainView.exhibitListView.dataTable);
        exhibitListViewController.bindFilterTextField(mainView.quickSearchField);
        bindExhibitContentView(mainView.exhibitListView);
    }
    
    public void bindMainMenuBar(MainMenuBar mainMenuBar) {
        // TOOD
    }
    
    void bindSourceList(JTree tree) {
        sourceListController.bindTree(tree);
    }
    
    void bindSourceListControlls(AbstractButton addFreeBoxButton,
            AbstractButton addGroupBoxButton, AbstractButton addSmartBoxButton,
            AbstractButton removeBoxButton) {
        sourceListController.bindAddFreeBox(addFreeBoxButton);
        sourceListController.bindAddGroupBox(addGroupBoxButton);
        sourceListController.bindAddSmartBox(addSmartBoxButton);
        sourceListController.bindRemoveBox(removeBoxButton);
    }
    
    void bindExhibitContentView(ExhibitListView view) {
        museumExhibitContentViewer.bindFileContentView(view.fileContentView);
        museumExhibitContentViewer.bindOverviewMotifView(view.overviewMotifView);
    }
    
    MuseumSourceModel getMuseumSourceModel() {
        return sourceListController.sourceModel;
    }
    
    @Override
    public boolean canExit(EventObject event) {
        return true;
    }

    @Override
    public void willExit(EventObject event) {
    }
    
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        TreePath path = e.getPath();
        if (path != null) {
            MutableTreeNode node = (MutableTreeNode) path.getLastPathComponent();
            
            if (node instanceof CollectionBoxNode) {
                ExhibitListModel model = ((CollectionBoxNode) node).getCollectionBox(); 
                setExhibitListSource(model);
            }
            else if (node instanceof DefaultMutableTreeNode &&
                    ((DefaultMutableTreeNode) node).getUserObject() instanceof ExhibitListModel) {
                ExhibitListModel model = (ExhibitListModel) ((DefaultMutableTreeNode) node).getUserObject();
                setExhibitListSource(model);
            }
        }
    }
    
    @Override
    public void valueChanged(ListSelectionEvent e) {
        reloadExhibitDetailsView();
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        String propertyName = evt.getPropertyName();
        
    }
}