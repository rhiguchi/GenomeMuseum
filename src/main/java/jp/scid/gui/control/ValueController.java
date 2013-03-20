package jp.scid.gui.control;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFormattedTextField;
import javax.swing.text.Document;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class ValueController<V> implements PropertyChangeListener {
    private final String valuePropertyName;
    private final ValueModel<V> valueModel;
    
    public ValueController(ValueModel<V> valueModel, String valuePropertyName) {
        if (valueModel == null) throw new IllegalArgumentException("valueModel must not be null");
        
        this.valueModel = valueModel;
        this.valuePropertyName = valuePropertyName;
    }
    
    public ValueController(ValueModel<V> valueModel) {
        this(valueModel, "value");
    }
    
    public ValueController(V initialValue) {
        this(ValueModels.newValueModel(initialValue));
    }
    
    public ValueController() {
        this(ValueModels.<V>newNullableValueModel());
    }

    public V getValue() {
        return getValueModel().getValue();
    }
    
    public void setValue(V newValue) {
        getValueModel().setValue(newValue);
    }
    
    ValueModel<V> getValueModel() {
        return valueModel;
    }

    protected void processValueChange(V newValue) {
        setValue(newValue);
    }
    
    public FormattedTextValueConnector<V> bindFormattedTextField(JFormattedTextField field) {
        FormattedTextValueConnector<V> controller = new FormattedTextValueConnector<V>(field);
        controller.setModel(getValueModel());
        field.addPropertyChangeListener("value", this);
        return controller;
    }
    
    public FormattedTextValueConnector<V> bindFormattedTextField(JFormattedTextField field, Document model) {
        FormattedTextValueConnector<V> controller = bindFormattedTextField(field);
        model.addDocumentListener(controller);
        return controller;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        V value = getPropertyValue(evt.getSource());
        processValueChange(value);
    }

    @SuppressWarnings("unchecked")
    protected V getPropertyValue(Object model) throws ClassCastException {
        Object value = AbstractController.getBeanPropertyValue(model, valuePropertyName);
        return (V) value;
    }
}
