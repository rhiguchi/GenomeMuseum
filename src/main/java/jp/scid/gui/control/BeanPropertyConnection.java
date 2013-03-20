package jp.scid.gui.control;

import static java.lang.String.*;

import java.beans.Statement;

@Deprecated
public class BeanPropertyConnection<T, V> extends ValueHandler<V> {
    private final T target;
    private final String propertyName;
    
    protected BeanPropertyConnection(T target, String propertyName) {
        this.target = target;
        this.propertyName = propertyName;
    }
    
    public T getTarget() {
        return target;
    }
    
    public String getPropertyName() {
        return propertyName;
    }
    
    public void updateBeanProperty() {
        handleValue(getModelValue());
    }

    @Override
    protected void handleValue(V newValue) {
        String setterMethodName = getSetterName();
        Statement updateStatement = new Statement(target, setterMethodName, new Object[]{newValue});
        
        try {
            updateStatement.execute();
        }
        catch (Exception e) {
            throw new IllegalStateException(format(
                    "invokation failure for '%s' method on '%s'", setterMethodName, target), e);
        }
    }
    
    String getSetterName() {
        return "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }
}