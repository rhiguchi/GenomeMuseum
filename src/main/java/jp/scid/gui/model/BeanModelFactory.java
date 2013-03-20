package jp.scid.gui.model;

import static java.lang.String.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.Statement;

import ca.odell.glazedlists.impl.beans.BeanProperty;

public class BeanModelFactory {
    private final Object bean;

    public BeanModelFactory(Object bean) {
        if (bean == null) throw new IllegalArgumentException("bean must not be null");
        
        this.bean = bean;
    }
    
    public ValueModel<?> createModel(String propertyName) {
        if (propertyName == null)
            throw new IllegalArgumentException("propertyName must not be null");
        
        @SuppressWarnings("unchecked")
        BeanProperty<Object> property = new BeanProperty<Object>((Class<Object>) bean.getClass(), propertyName, true, false);
        BeanPropertyModel model = new BeanPropertyModel(bean, property);
        model.listenToBeanPropertyChange();
        return model;
    }
    
    static void execute(Object target, String methodName, Object... arguments) throws IllegalStateException {
        Statement statement = new Statement(target, methodName, arguments);
        try {
            statement.execute();
        }
        catch (Exception e) {
            throw new IllegalStateException(format(
                    "Cannot execute %s on an object for %s", methodName, target.getClass()), e);
        }
    }
    
    static class BeanPropertyModel extends AbstractValueModel<Object> implements PropertyChangeListener {
        private final Object bean;
        private final BeanProperty<Object> property;
        
        @SuppressWarnings("unchecked")
        public <B> BeanPropertyModel(B bean, BeanProperty<B> property) {
            super();
            
            this.bean = bean;
            this.property = (BeanProperty<Object>) property;
        }
        
        public void listenToBeanPropertyChange() {
            execute(bean, "addPropertyChangeListener", this);
        }
        
        public void deafToBeanPropertyChange() {
            execute(bean, "removePropertyChangeListener", this);
        }
        
        @Override
        public Object getValue() {
            return property.get(bean);
        }

        @Override
        public void setValue(Object newValue) {
            property.set(bean, newValue);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            firePropertyChange(evt.getOldValue(), evt.getNewValue());
        }
    }
}
