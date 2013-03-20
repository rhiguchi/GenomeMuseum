package jp.scid.gui.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * 単一値モデルの抽象実装
 * @author Ryusuke Higuchi
 *
 * @param <T> 保持する値の型
 */
abstract public class AbstractValueModel<T> implements ValueModel<T> {
    private PropertyChangeSupport propertyChangeSupport;
    
    public AbstractValueModel() {
    }
    
    /**
     * {@inheritDoc}
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().addPropertyChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().removePropertyChangeListener(listener);
    }

    /**
     * 値変化の {@code PropertyChange} イベントを発行する。{@code value} がプロパティ名となる。
     * @param oldValue 古い値
     * @param newValue 新しい値
     */
    protected void firePropertyChange(T oldValue, T newValue) {
        getPropertyChangeSupport().firePropertyChange("value", oldValue, newValue);
    }
    
    PropertyChangeSupport getPropertyChangeSupport() {
        if (propertyChangeSupport == null)
            propertyChangeSupport = createPropertyChangeSupport(this);
        return propertyChangeSupport;
    }
    
    /**
     * プロパティ変化イベントの管理委譲オブジェクトの作成。
     * @param self  {@code PropertyChangeSupport} のコンストラクタに渡す引数。
     * @return イベントの管理委譲オブジェクト
     * @see PropertyChangeSupport#PropertyChangeSupport(Object)
     */
    protected PropertyChangeSupport createPropertyChangeSupport(Object self) {
        return new PropertyChangeSupport(self);
    }
    
    /**
     * Scala 用アクセサ
     * @return {@link #getValue()} の値
     */
    public T apply() {
        return getValue();
    }
    
    /**
     * Scala 用アクセサ
     * @param newValue 新しい値
     */
    public void $colon$eq(T newValue) {
        setValue(newValue);
    }
}
