package jp.scid.genomemuseum.gui;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTree;

import jp.scid.genomemuseum.model.CollectionBox;
import jp.scid.genomemuseum.model.MuseumSourceModel;
import jp.scid.genomemuseum.view.MainMenuBar;
import jp.scid.genomemuseum.view.MainView;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Application.ExitListener;
import org.jooq.impl.Factory;

public class MainFrameController extends AbstractBean
        implements ExitListener, PropertyChangeListener {
    static final String PROPERTY_VISIBLE = "visible";

    // Properties
//    boolean visible = false;
    
    // Controllers
    final JFrame frame;
    
//    final MuseumExhibitController exhibitListController;
    
//    final MuseumSourceListController sourceListController;
    
    // actions
    final Action openAction = new AbstractAction("open") {
        @Override
        public void actionPerformed(ActionEvent e) {
            open();
        }
    };
    
//    MainFrameController(JFrame frame, MuseumExhibitController exhibitListController,
//            MuseumSourceListController sourceListController) {
//        this.frame = frame;
////        this.exhibitListController = exhibitListController;
////        this.sourceListController = sourceListController;
//        
//        sourceListController.addPropertyChangeListener(this);
//    }
    
    public MainFrameController(JFrame frame) {
//        this(frame, new MuseumExhibitController(new FileDialog(frame, "")), new MuseumSourceListController());
        this.frame = frame;
    }
    
    public MainFrameController() {
        this(new JFrame());
    }
    
    public void setFactory(Factory factory) {
//        exhibitListController.setFactory(factory);
    }
    
    // Controllers
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        if (sourceListController == evt.getSource()
//                && MuseumSourceListController.PROPETY_SELECTED_ROOM.equals(evt.getPropertyName())) {
//            // TODO updating
//        }
    }
    
    // Actions
    public void show() {
        frame.setVisible(true);
    }

    public void open() {
//        exhibitListController.open();
    }
    
    public CollectionBox getSelectedSource() {
        // TODO
        return null;
    }

    // Bindings
    @Deprecated
    public void bindFrame(JFrame frame) {
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setLocationRelativeTo(null);
    }
    
    public void bindMainView(MainView mainView) {
        bindSourceList(mainView.sourceList);
        bindSourceListControlls(
                mainView.addListBox, mainView.addBoxFolder, mainView.addSmartBox,
                mainView.removeBoxButton);
    }
    
    public void bindMainMenuBar(MainMenuBar mainMenuBar) {
        // TOOD
    }
    
    void bindSourceList(JTree tree) {
//        sourceListController.bindTree(tree);
    }
    
    void bindSourceListControlls(AbstractButton addFreeBoxButton,
            AbstractButton addGroupBoxButton, AbstractButton addSmartBoxButton,
            AbstractButton removeBoxButton) {
//        sourceListController.bindAddFreeBox(addFreeBoxButton);
//        sourceListController.bindAddGroupBox(addGroupBoxButton);
//        sourceListController.bindAddSmartBox(addSmartBoxButton);
//        sourceListController.bindRemoveBox(removeBoxButton);
    }
    
    MuseumSourceModel getMuseumSourceModel() {
        return null; //sourceListController.sourceModel;
    }
    
    @Override
    public boolean canExit(EventObject event) {
        return true;
    }

    @Override
    public void willExit(EventObject event) {
    }
    
    public class Builder {
        public Builder() {
        }
        
//        public MainFrameController build() {
//            
//        }
        
        public void bindToMenus(MainMenuBar mainMenuBar) {
            mainMenuBar.open.setAction(openAction);
        }
        
        public void bindToMainView(MainView mainView) {
//            exhibitListController.bind(mainView.exhibitListView);
//            exhibitListController.bindFilterTextField(mainView.quickSearchField);
            
            bindSourceList(mainView.sourceList);
        }
    }
}