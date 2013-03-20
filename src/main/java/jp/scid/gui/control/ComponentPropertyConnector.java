package jp.scid.gui.control;

import java.awt.Component;
import java.awt.EventQueue;

import jp.scid.gui.model.ValueModel;

public class ComponentPropertyConnector<C extends Component, V> extends ViewValueConnector<C, V> {
    private final String propertyName;
    
    public ComponentPropertyConnector(C targetView, String propertyName) {
        super(targetView);
        if (propertyName == null)
            throw new IllegalArgumentException("propertyName must not be null");
        
        this.propertyName = propertyName;
    }

    public static <C extends Component, V> ComponentPropertyConnector<C, V> connect(
            ValueModel<V> model, C component, String propertyName) {
        ComponentPropertyConnector<C, V> conn = new ComponentPropertyConnector<C, V>(component, propertyName);
        conn.setModel(model);
        return conn;
    }
    
    @Override
    protected void processPropertyChange(ValueModel<V> model, String property) {
        if (property == null || property.equals("value")) {
            final V value = model.getValue();
            
            Runnable updater = new Runnable() {
                public void run() {
                    updateView(getView(), value);
                }
            };
            
            if (EventQueue.isDispatchThread()) {
                updater.run();
            }
            else {
                EventQueue.invokeLater(updater);
            }
        }
    }
    
    @Override
    protected void updateView(C target, V modelValue) {
        execute(target, getSetterName(propertyName), modelValue);
    }
}
