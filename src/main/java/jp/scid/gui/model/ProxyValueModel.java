package jp.scid.gui.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * {@link ValueModel} の接続を行う。
 * @author Ryusuke Higuchi
 *
 * @param <T> 値の型。
 */
public class ProxyValueModel<T> extends AbstractProxyValueModel<T, T> {
    /**
     * 何もモデルを監視していない状態のオブジェクトを作成する。
     */
    public ProxyValueModel() {
        super();
    }
    
    /**
     * 主題モデルを指定して、オブジェクトを作成する。
     * @param subjectModel 主題値モデル。
     * @see #setSubject(Object)
     */
    public ProxyValueModel(ValueModel<T> subjectModel) {
        this();
        setSubject(subjectModel);
    }

    /** {@inheritDoc} */
    @Override
    T getSubjectValue(ValueModel<T> subject) {
        return subject.getValue();
    }
    
    /** {@inheritDoc} */
    @Override
    void updateSubjectValue(ValueModel<T> subject, T newValue) {
        subject.setValue(newValue);
    }
}

/**
 * {@link ValueModel} を別の型の {@link ValueModel} に変換する抽象定義。
 * 
 * @author Ryusuke Higuchi
 *
 * @param <S> 主題値の型。
 * @param <T> 変換後の値の型。
 */
abstract class AbstractProxyValueModel<S, T> extends WrappedValueModel<ValueModel<S>, T> {
    private final PropertyChangeListener changeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("value".equals(evt.getPropertyName())) {
                updateValueFromSubject();
            }
        }
    };
    
    /**
     * {@link ValueModel} の {@code value} 値の変化を監視する。
     */
    @Override
    void listenTo(ValueModel<S> newSubject) {
        newSubject.addPropertyChangeListener(changeListener);
    }
    
    /**
     * この {@link ValueModel} の変化監視を停止する。
     */
    @Override
    void deafTo(ValueModel<S> newSubject) {
        newSubject.removePropertyChangeListener(changeListener);
    }
}
