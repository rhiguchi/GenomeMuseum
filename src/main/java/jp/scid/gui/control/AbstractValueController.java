package jp.scid.gui.control;

import jp.scid.gui.model.ValueModel;

/**
 * 単一モデルをデータモデルとする操作クラス。
 * @author Ryusuke Higuchi
 *
 * @param <V>
 */
public abstract class AbstractValueController<V> extends AbstractController<ValueModel<V>> {
    /**
     * プロパティ名に関係なくモデル内の値を取得して処理を行う。
     * @see #processValueChange(Object)
     */
    @Override
    protected void processPropertyChange(ValueModel<V> model, String property) {
        V newValue = model.getValue();
        processValueChange(newValue);
    }
    
    /**
     * モデルに変化通知が行われたときに、モデルの値の処理を行う。
     * @param newValue
     */
    protected abstract void processValueChange(V newValue);
}
