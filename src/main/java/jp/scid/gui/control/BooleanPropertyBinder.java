package jp.scid.gui.control;

import java.awt.Component;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class BooleanPropertyBinder extends ValuePropertyBinder<Boolean> {
    public BooleanPropertyBinder(ValueModel<Boolean> valueModel) {
        super(valueModel, Boolean.class);
    }
    
    public BooleanPropertyBinder() {
        this(ValueModels.newBooleanModel(false));
    }

    public ButtonSelectionConnector bindButtonSelection(AbstractButton button) {
        button.setSelected(getValueModel().getValue());
        ButtonSelectionConnector conn = new ButtonSelectionConnector(button);
        conn.setModel(getValueModel());
        return conn;
    }
    
    public ButtonSelectionConnector bindButtonSelection(AbstractButton button, ButtonModel model) {
        ButtonSelectionConnector conn = bindButtonSelection(button);
        conn.listenEditingTo(model);
        return conn;
    }
    
    public <C extends Component> ComponentPropertyConnector<C, Boolean> bindVisible(C component) {
        ComponentPropertyConnector<C, Boolean> connector =
                new ComponentPropertyConnector<C, Boolean>(component, "visible");
        connector.setModel(getValueModel());
        return connector;
    }
}
