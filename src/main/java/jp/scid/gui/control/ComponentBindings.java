package jp.scid.gui.control;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;

import javax.swing.AbstractButton;
import javax.swing.text.JTextComponent;

import jp.scid.gui.model.ValueModel;

@Deprecated
public class ComponentBindings {
    
    
    public BooleanModelComponentBinder connectBoolean(ValueModel<Boolean> model) {
        return new BooleanModelComponentBinder(model);
    }
    
    public StringModelComponentBinder connectString(ValueModel<String> model) {
        return new StringModelComponentBinder(model);
    }
    
    protected <T extends Component, V> BeanPropertyConnection<T, V> connectProperty(
            T component, ValueModel<V> model, String propertyName) {
        BeanPropertyConnection<T, V> connector =
            new BeanPropertyConnection<T, V>(component, propertyName);
        connector.setModel(model);
        connector.updateBeanProperty();
        
        return connector;
    }

    abstract class ValueModelComponentBinder<V> {
        private final ValueModel<V> model;

        ValueModelComponentBinder(ValueModel<V> model) {
            if (model == null)
                throw new IllegalArgumentException("model must not be null");
            this.model = model;
        }

        <T extends Component> BeanPropertyConnection<T, V> connectProperty(T component, String propertyName) {
            return ComponentBindings.this.connectProperty(component, model, propertyName);
        }
    }
    
    public class BooleanModelComponentBinder extends ValueModelComponentBinder<Boolean> {
        BooleanModelComponentBinder(ValueModel<Boolean> model) {
            super(model);
        }

        public <C extends Component> BeanPropertyConnection<C, Boolean> toVisibleOf(C component) {
            return connectProperty(component, "visible");
        }
        
        public <C extends Component> BeanPropertyConnection<C, Boolean> toEnabledOf(C component) {
            return connectProperty(component, "enabled");
        }
    }
    
    public class StringModelComponentBinder extends ValueModelComponentBinder<String> {
        StringModelComponentBinder(ValueModel<String> model) {
            super(model);
        }
        
        public <C extends JTextComponent> BeanPropertyConnection<C, String> toTextOf(C component) {
            return connectProperty(component, "text");
        }
        
        public <C extends AbstractButton> BeanPropertyConnection<C, String> toTextOf(C component) {
            return connectProperty(component, "text");
        }
        
        public <C extends Frame> BeanPropertyConnection<C, String> toTitleOf(C component) {
            return connectProperty(component, "title");
        }
        
        public <C extends Dialog> BeanPropertyConnection<C, String> toTitleOf(C component) {
            return connectProperty(component, "title");
        }
    }
}

