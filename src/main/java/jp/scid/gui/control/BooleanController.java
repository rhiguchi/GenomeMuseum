package jp.scid.gui.control;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class BooleanController extends ValueController<Boolean> implements ItemListener {

    public BooleanController(ValueModel<Boolean> valueModel) {
        super(valueModel);
    }

    public BooleanController(boolean initialValue) {
        this(ValueModels.newBooleanModel(initialValue));
    }
    
    public BooleanController() {
        this(false);
    }

    public ButtonSelectionConnector bindButtonSelected(AbstractButton button) {
        ButtonSelectionConnector conn = new ButtonSelectionConnector(button);
        conn.setModel(getValueModel());
        return conn;
    }
    
    public ButtonSelectionConnector bindButtonSelected(AbstractButton button, ButtonModel model) {
        ButtonSelectionConnector conn = bindButtonSelected(button);
        model.addItemListener(this);
        return conn;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        switch (e.getStateChange()) {
        case ItemEvent.SELECTED:
            setValue(true);
            break;
        case ItemEvent.DESELECTED:
            setValue(false);
            break;
        } 
    }
}
