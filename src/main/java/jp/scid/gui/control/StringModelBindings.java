package jp.scid.gui.control;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTextField;

import jp.scid.gui.control.BooleanModelBindings.ModelConnector;
import jp.scid.gui.model.ValueModel;

public class StringModelBindings extends AbstractValueModelBindigs<String> {

    public StringModelBindings(ValueModel<String> model) {
        super(model);
    }
    
    public ModelConnector bindToLabelText(final JLabel label) {
        return installModel(new AbstractPropertyConnector<String>() {
            protected void valueChanged(String newValue) {
                label.setText(newValue);
            }
        });
    }

    public ModelConnector bindToTextField(final JTextField field) {
        return installModel(new AbstractPropertyConnector<String>() {
            protected void valueChanged(String newValue) {
                field.setText(newValue);
            }
        });
    }
}

abstract class AbstractValueModelBindigs<T> {
    protected final ValueModel<T> model;

    public AbstractValueModelBindigs(ValueModel<T> model) {
        if (model == null) throw new IllegalArgumentException("model must not be null");
        this.model = model;
    }
    
    <C extends AbstractPropertyConnector<T>> C installModel(C connector) {
        connector.setModel(model);
        return connector;
    }
    
    abstract static class AbstractPropertyConnector<T> extends ValueChangeHandler<T> implements ModelConnector {
        @Override
        public void dispose() {
            setModel(null);
        }
    }
    
    abstract static class ComponentPropertyConnector<T, C extends Component> extends AbstractPropertyConnector<T> {
        private final C component;
        
        protected ComponentPropertyConnector(C component) {
            super();
            if (component == null)
                throw new IllegalArgumentException("component must not be null");
            this.component = component;
        }
        
        abstract protected void applyValue(C component, T newValue);
        
        @Override
        protected void valueChanged(T newValue) {
            applyValue(component, newValue);
        }
    }
}