package jp.scid.gui.control;

import static java.lang.String.*;

import java.beans.Expression;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.text.Document;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class ValuePropertyBinder<M> extends AbstractValueController<M> {
    private final Class<M> valueClass;
    
    private final ValueModel<M> valueModel;
    
    public ValuePropertyBinder(ValueModel<M> valueModel, Class<M> valueClass) {
        this.valueModel = valueModel;
        this.valueClass = valueClass;
    }
    
    public ValuePropertyBinder(Class<M> valueClass) {
        this(ValueModels.<M>newNullableValueModel(), valueClass);
    }

    public ValueModel<M> getValueModel() {
        return valueModel;
    }
    
    public FormattedTextValueConnector<M> bindFormattedTextField(JFormattedTextField field) {
        FormattedTextValueConnector<M> controller = new FormattedTextValueConnector<M>(field, valueClass);
        field.setValue(getValueModel().getValue());
        controller.setModel(getValueModel());
        return controller;
    }
    
    public FormattedTextValueConnector<M> bindFormattedTextField(JFormattedTextField field, Document document) {
        FormattedTextValueConnector<M> controller = bindFormattedTextField(field);
        controller.listenEditingTo(document);
        return controller;
    }
    
    public <C extends JComponent> ComponentPropertyConnector<C, M> bindComponentProperty(
            C component, String propertyName) {
        ComponentPropertyConnector<C, M> connector = new ComponentPropertyConnector<C, M>(component, propertyName);
        connector.setModel(getValueModel());
        return connector;
    }
    
    @Override
    protected void processValueChange(M newValue) {
        getValueModel().setValue(newValue);
    }
    
    protected M getPropertyValue(Object model, String property) {
        Object value = getBeanPropertyValue(model, property);
        return valueClass.cast(value);
    }
}
