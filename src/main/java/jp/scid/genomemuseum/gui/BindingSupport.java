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

import javax.swing.text.JTextComponent;

import jp.scid.gui.control.ComponentPropertyConnector;
import jp.scid.gui.control.TextComponentTextConnector;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

@Deprecated
class BindingSupport {
    final Object bean;
    
    public BindingSupport(Object bean) {
        this.bean = bean;
    }

    Method getReadMethod(String property) {
        PropertyDescriptor pd;
        try {
            pd = new PropertyDescriptor(property, bean.getClass());
        }
        catch (IntrospectionException e) {
            throw new IllegalStateException(format(
                    "maybe invalid property name '%s'", property), e);
        }
        
        Method readMethod = pd.getReadMethod();
        return readMethod;
    }
    
    public PropertyConnector bind(String property) {
        Method readMethod = getReadMethod(property);
        
        PropertyConnectorImpl conn = new PropertyConnectorImpl(readMethod);
        conn.updateModelValue();
        
        listenTo(bean, conn);
        
        return conn;
    }
    
    public StringPropertyConnector bindText(String property) {
        Method readMethod = getReadMethod(property);
        
        if (!String.class.isAssignableFrom(readMethod.getReturnType())) {
            throw new IllegalArgumentException(format(
                    "%s property is not String return type", property));
        }
        
        StringPropertyConnector conn = new StringPropertyConnector(readMethod);
        
        listenTo(bean, conn);
        
        return conn;
    }
    
    protected static interface PropertyConnector {
        <C extends Component> ComponentPropertyConnector<C, Object> to(C component, String propertyName);
    }
    
    class PropertyConnectorImpl extends AbstractPropertyConnector<Object> implements PropertyConnector, PropertyChangeListener {
        PropertyConnectorImpl(Method readMethod) {
            super(readMethod, Object.class);
        }
    }
    
    abstract class AbstractPropertyConnector<T> implements PropertyChangeListener {
        final ValueModel<T> model = ValueModels.newNullableValueModel();
        final Class<T> valueClass;
        final Method readMethod;
        
        protected AbstractPropertyConnector(Method readMethod, Class<T> valueClass) {
            this.readMethod = readMethod;
            this.valueClass = valueClass;
            
            updateModelValue();
        }

        public void updateModelValue() {
            try {
                Object returnValue = readMethod.invoke(bean, (Object[]) null);
                T newValue = valueClass.cast(returnValue);
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
        
        public <C extends Component> ComponentPropertyConnector<C, T> to(C component, String propertyName) {
            return ComponentPropertyConnector.connect(model, component, propertyName);
        }
    }
    
    public class StringPropertyConnector extends AbstractPropertyConnector<String> {
        private StringPropertyConnector(Method readMethod) {
            super(readMethod, String.class);
        }
        
        public <C extends JTextComponent> TextComponentTextConnector toText(C component) {
            return TextComponentTextConnector.connect(model, component);
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