package jp.scid.gui.control;

import javax.swing.AbstractButton;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

/**
 * 値モデルをビューに適用する処理の抽象実装。
 * @author Ryusuke Higuchi
 *
 * @param <T> ビュークラス型
 * @param <V> 値型
 */
public abstract class ViewValueConnector<T, V> extends AbstractController<ValueModel<V>> {
    private final T view;
    
    public ViewValueConnector(T targetView) {
        super();
        if (targetView == null)
            throw new IllegalArgumentException("targetView must not be null");
        this.view = targetView;
    }

    public static <C extends AbstractButton> ViewValueConnector<C, Boolean> connectSelected(ValueModel<Boolean> model, C button) {
        ComponentPropertyConnector<C, Boolean> conn = new ComponentPropertyConnector<C, Boolean>(button, "selected");
        conn.setModel(model);
        return conn;
    }
    
    public T getView() {
        return view;
    }
    
    @Deprecated
    public void setModelValue(V newValue) {
        ValueModel<V> valueModel = getModel();
        if (valueModel == null) {
            valueModel = ValueModels.newValueModel(newValue);
            setModel(valueModel);
        }
        else {
            valueModel.setValue(newValue);
        }
    }

    @Override
    protected void processPropertyChange(ValueModel<V> model, String property) {
        V value = model.getValue();

        updateView(getView(), value);
    }
    
    abstract protected void updateView(T target, V modelValue);
}
