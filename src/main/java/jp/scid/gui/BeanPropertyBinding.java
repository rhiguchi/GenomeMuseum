package jp.scid.gui;

import ca.odell.glazedlists.impl.beans.BeanProperty;
import jp.scid.gui.control.ValueChangeHandler;
import jp.scid.gui.model.ValueModel;

public class BeanPropertyBinding<T> extends ValueChangeHandler<T> {
    private final Object bean;
    private final BeanProperty<Object> property;
    
    public BeanPropertyBinding(Object bean, String propertyName) {
        if (bean == null) throw new IllegalArgumentException("bean must not be null");
        
        this.bean = bean;
        
        @SuppressWarnings("unchecked")
        Class<Object> beanClass = (Class<Object>) bean.getClass();
        this.property = new BeanProperty<Object>(beanClass, propertyName, false, true);
    }
    
    public static <T> BeanPropertyBinding<T> bind(Object bean, String propertyName, ValueModel<T> model) {
        BeanPropertyBinding<T> connector = new BeanPropertyBinding<T>(bean, propertyName);
        connector.setModel(model);
        return connector;
    }

    @Override
    protected void valueChanged(T newValue) {
        property.set(bean, newValue);
    }
}
