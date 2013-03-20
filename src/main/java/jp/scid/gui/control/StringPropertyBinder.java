package jp.scid.gui.control;

import java.awt.Frame;

import javax.swing.JTextField;
import javax.swing.text.Document;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class StringPropertyBinder extends ValuePropertyBinder<String> {
    public StringPropertyBinder(ValueModel<String> target) {
        super(target, String.class);
    }
    
    public StringPropertyBinder() {
        this(ValueModels.newValueModel(""));
    }
    
    public TextComponentTextConnector bindTextField(JTextField field) {
        TextComponentTextConnector controller = new TextComponentTextConnector(field);
        field.setText(getValueModel().getValue());
        controller.setModel(getValueModel());
        return controller;
    }
    
    public TextComponentTextConnector bindTextField(JTextField field, Document document) {
        TextComponentTextConnector controller = bindTextField(field);
        controller.listenEditingTo(document);
        return controller;
    }
    
    public <C extends Frame> ComponentPropertyConnector<C, String> bindFrameTitle(C frame) {
        ComponentPropertyConnector<C, String> connector = new ComponentPropertyConnector<C, String>(frame, "title");
        connector.setModel(getValueModel());
        return connector;
    }
}
