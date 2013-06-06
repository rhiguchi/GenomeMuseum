package jp.scid.gui.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

public class ValueModelFactory implements PropertyChangeListener {
    private final Map<String, PropertyConnector<?>> propertyModels;
    
    public ValueModelFactory() {
        propertyModels = new HashMap<String, PropertyConnector<?>>();
    }

    public ValueModel<Boolean> createBooleanModel(String propertyName, boolean initialValue) {
        return createPropertyValueModel(propertyName, Boolean.class, initialValue);
    }
    
    private <T> ValueModel<T> createPropertyValueModel(String propertyName, Class<T> valueClass, T initialValue) {
        if (propertyName == null) throw new IllegalArgumentException("propertyName must not be null");
        if (valueClass == null) throw new IllegalArgumentException("valueClass must not be null");
        
        ValueModel<T> valueModel = ValueModels.newValueModel(initialValue);
        PropertyConnector<T> conn = new PropertyConnector<T>(valueModel, valueClass);
        propertyModels.put(propertyName, conn);
        
        return valueModel;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        PropertyConnector<?> model = propertyModels.get(evt.getPropertyName());
        if (model == null) {
            return;
        }
        model.setObjectValue(evt.getNewValue());
    }
    
    private static class PropertyConnector<T> {
        private final Class<T> valueClass;
        private final ValueModel<T> valueModel;
        
        public PropertyConnector(ValueModel<T> valueModel, Class<T> valueClass) {
            this.valueModel = valueModel;
            this.valueClass = valueClass;
        }

        void setObjectValue(Object newValue) {
            valueModel.setValue(valueClass.cast(newValue));
        }
    }
}
