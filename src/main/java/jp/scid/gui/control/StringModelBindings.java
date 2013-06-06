package jp.scid.gui.control;

import javax.swing.JLabel;

import jp.scid.gui.control.BooleanModelBindings.ModelConnector;
import jp.scid.gui.model.ValueModel;

public class StringModelBindings extends AbstractValueModelBindigs<String> {

    public StringModelBindings(ValueModel<String> model) {
        super(model);
    }
    
    public ModelConnector bindToLabelText(JLabel label) {
        return installModel(new LabelTextConnector(label));
    }

    private static class LabelTextConnector extends AbstractPropertyConnector<String> {
        private final JLabel label;
        
        public LabelTextConnector(JLabel label) {
            this.label = label;
        }
        
        @Override
        protected void valueChanged(String newValue) {
            label.setText(newValue);
        }
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
}