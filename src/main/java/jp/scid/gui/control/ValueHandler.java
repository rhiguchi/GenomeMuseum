package jp.scid.gui.control;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

/**
 * 値モデルが変化した時に処理を実行する抽象クラス。
 * 
 * @author Ryusuke Higuchi
 *
 * @param <V> 値モデルの型
 */
@Deprecated
abstract public class ValueHandler<V> {
    private final PropertyChangeListener changeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            processModelValueChange(evt.getPropertyName());
        }
    };
    
    private ValueModel<V> model;
    
    public ValueHandler() {
        this(ValueModels.<V>newNullableValueModel());
    }
    
    public ValueHandler(ValueModel<V> subjectModel) {
        bindTo(subjectModel);
    }
    
    /**
     * モデルの変化監視を停止し、接続を解除する。
     */
    public void release() {
        if (model != null)
            model.removePropertyChangeListener(changeListener);
        model = null;
    }
    
    /**
     * 現在接続しているモデルを取得する。
     * @return 接続中のモデル。
     */
    public ValueModel<V> getModel() {
        return model;
    }
    
    /**
     * モデルの変化を監視して、変化の際には処理を実行するよう接続する。
     * 
     * 現在接続されているモデルは、接続が解除される。
     * @param model
     */
    public void setModel(ValueModel<V> model) {
        release();
        bindTo(model);
    }

    Set<String> getObservingProperties() {
        return Collections.singleton("value");
    }
    
    /**
     * @return 現在のモデルの値。
     */
    V getModelValue() {
        ValueModel<V> m = getModel();
        return m == null ? null : m.getValue();
    }

    void processModelValueChange() {
        V modelValue = getModelValue();
        if (modelValue != null)
            handleValue(modelValue);
    }
    
    void processModelValueChange(String propertyName) {
        processModelValueChange();
    }
    
    abstract protected void handleValue(V newValue);

    /**
     * 値の変化を監視して処理を行う対象モデルを指定する。
     * 
     * @param subjectModel 主題モデル。
     */
    void bindTo(ValueModel<V> subjectModel) {
        if (subjectModel != null)
            subjectModel.addPropertyChangeListener(changeListener);
        this.model = subjectModel;
    }
}
