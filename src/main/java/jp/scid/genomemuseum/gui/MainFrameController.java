package jp.scid.genomemuseum.gui;

import static java.lang.String.*;

import java.awt.Component;
import java.beans.Expression;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.Statement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EventObject;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import jp.scid.genomemuseum.model.CollectionBox;
import jp.scid.genomemuseum.model.GMExhibit;
import jp.scid.genomemuseum.model.ListSource;
import jp.scid.genomemuseum.model.MuseumDataSchema;
import jp.scid.genomemuseum.view.ExhibitListView;
import jp.scid.genomemuseum.view.MainView;
import jp.scid.gui.control.ComponentPropertyConnector;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Application.ExitListener;


public class MainFrameController extends AbstractBean
        implements ExitListener, TreeSelectionListener, PropertyChangeListener {
    public static final String PROPKEY_FRAME_VISIBLED = "frameVisibled";

    boolean frameVisibled = false;
    
    protected MuseumDataSchema schema = null;
    
    // Controllers
    final MuseumSourceListController sourceListController;
    
    final ExhibitListViewController exhibitListViewController;
    
    final BindingSupport bindings = new BindingSupport(this);
    
    public MainFrameController() {
        sourceListController = new MuseumSourceListController();
        sourceListController.addPropertyChangeListener(this);
        
        sourceListController.getSelectionModel().addTreeSelectionListener(this);
        
        exhibitListViewController = new ExhibitListViewController();
        exhibitListViewController.addPropertyChangeListener(this);
    }
    
    // frameVisibled
    public boolean isFrameVisibled() {
        return frameVisibled;
    }

    public void setFrameVisibled(boolean newValue) {
        firePropertyChange(PROPKEY_FRAME_VISIBLED, this.frameVisibled, this.frameVisibled = newValue);
    }
    
    public void showFrame() {
        setFrameVisibled(true);
    }
    
    public void setDataSchema(MuseumDataSchema newSchema) {
        setExhibitListSource(null);
        sourceListController.setCollectionBoxSource(null);
        
        schema = newSchema;
        
        if (newSchema != null) {
//          setExhibitListSource(newSchema.getMuseumExhibitLibrary());
            sourceListController.setCollectionBoxSource(newSchema.getCollectionBoxService());
        }
    }
    
    public CollectionBox getSelectedSource() {
        // TODO
        return null;
    }
    
    public GMExhibit getSelectedExhibit() {
        // TODO
        return null;
    }

    // Action methods
    public void reloadExhibitDetailsView() {
        GMExhibit selectedExhibit = getSelectedExhibit();
        
        // TODO Auto-generated method stub
        
    }

    public void reloadExhibitList() {
        // TODO Auto-generated method stub
        
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
        bindSourceListControlls(mainView.addListBox);
        
        bindExhibitListView(mainView.exhibitListView);
    }
    
    void bindSourceList(JTree tree) {
        sourceListController.bindTree(tree);
    }
    
    void bindSourceListControlls(AbstractButton addFreeBoxButton) {
        sourceListController.bindAddFreeBox(addFreeBoxButton);
    }
    
    void bindExhibitListView(ExhibitListView exhibitListView) {
        exhibitListViewController.bindExhibitListView(exhibitListView);
    }

    @Override
    public boolean canExit(EventObject event) {
        return true;
    }

    @Override
    public void willExit(EventObject event) {
    }

    public void setExhibitListSource(ListSource<GMExhibit> listSource) {
        exhibitListViewController.setListSource(listSource);
    }
    
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        TreePath path = e.getPath();
        if (path != null) {
            Object selectedNode = path.getLastPathComponent();
            
            
        }
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        String propertyName = evt.getPropertyName();
        
        if (source == sourceListController) {
            if ("selection".equals(propertyName)) {
                reloadExhibitList();
            }
        }
        else if (source == exhibitListViewController) {
            if ("selection".equals(propertyName)) {
                reloadExhibitDetailsView();
            }
        }
    }
}

class BindingSupport {
    final Object bean;
    
    public BindingSupport(Object bean) {
        this.bean = bean;
    }
    
    public PropertyConnector bind(String property) {
        PropertyDescriptor pd;
        try {
            pd = new PropertyDescriptor(property, bean.getClass());
        }
        catch (IntrospectionException e) {
            throw new IllegalStateException(format(
                    "maybe invalid property name ''", property), e);
        }
        
        Method readMethod = pd.getReadMethod();
        
        PropertyConnectorImpl conn = new PropertyConnectorImpl(readMethod);
        conn.updateModelValue();
        
        listenTo(bean, conn);
        
        return conn;
    }
    
    protected static interface PropertyConnector {
        <C extends Component> ComponentPropertyConnector<C, Object> to(C component, String propertyName);
    }
    
    class PropertyConnectorImpl implements PropertyConnector, PropertyChangeListener {
        final ValueModel<Object> model = ValueModels.newNullableValueModel();
        final Method readMethod;
        
        public PropertyConnectorImpl(Method readMethod) {
            this.readMethod = readMethod;
            
            updateModelValue();
        }
        
        public void updateModelValue() {
            try {
                Object newValue = readMethod.invoke(bean, (Object[]) null);
                model.setValue(newValue);
            }
            catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateModelValue();
        }

        @Override
        public <C extends Component> ComponentPropertyConnector<C, Object> to(C component, String propertyName) {
            return ComponentPropertyConnector.connect(model, component, propertyName);
        }
    }
    
    protected Object getControllerValue(String getterMethodName) {
        Expression statement = new Expression(bean, getterMethodName, null);
        
        try {
            return statement.getValue();
        }
        catch (Exception e) {
            throw new IllegalStateException(format(
                    "Cannot execute %s with %s", getterMethodName, bean.getClass()), e);
        }
    }
    
    void listenTo(Object controller, PropertyChangeListener listener) {
        
        Statement statement = new Statement(controller, "addPropertyChangeListener", new Object[]{listener});
        try {
            statement.execute();
        }
        catch (Exception e) {
            throw new IllegalStateException(format(
                    "Cannot execute %s with %s", "addPropertyChangeListener", controller.getClass()), e);
        }
    }
}