package jp.scid.gui.model;

/**
 * 単一の値を持つモデル。
 * @author Ryusuke Higuchi
 *
 * @param <T> 値の型。
 */
public class SimpleValueModel<T> extends AbstractValueModel<T> {
    private T value = null;
    
    /**
     * 定義された値を持たないモデルを構成する。 
     */
    public SimpleValueModel() {
    }
    
    /**
     * 値を持つモデルを構成する。
     * @param initialValue このモデルの初期値。
     */
    public SimpleValueModel(T initialValue) {
        this.value = initialValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T newValue) {
        firePropertyChange(value, value = newValue);
    }

    @Override
    public String toString() {
        return "ValueModel [value=" + value + "]";
    }
}
