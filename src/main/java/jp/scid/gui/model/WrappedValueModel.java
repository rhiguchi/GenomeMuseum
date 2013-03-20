package jp.scid.gui.model;

/**
 * ある主題値を {@link ValueModel} として扱うための接続用抽象クラス。
 * 
 * 主題値の変化は監視され、主題値が変更されるとこの値も変更される。
 * このモデルの値が変更された時は、主題値への適用を試みるが、
 * 実装はその変更を適用しないことも許される。
 * 
 * @author Ryusuke Higuchi
 *
 * @param <S> 主題値。
 * @param <T> 提供する値の型。
 */
public abstract class WrappedValueModel<S, T> extends SimpleValueModel<T> {
    private S subject = null;
    
    WrappedValueModel() {
    }
    
    public WrappedValueModel(S subject) {
        this();
        setSubject(subject);
    }

    /**
     * 主題値を取得する。
     * 
     * @return 主題値。
     */
    public S getSubject() {
        return subject;
    }
    
    @Override
    public void setValue(T newValue) {
        super.setValue(newValue);
        
        updateSubject();
    }
    
    /**
     * 新しい主題値を設定する。
     * 
     * 主題値の変化は監視され、主題値が変更されると、このモデルの変化としてリスナーに通知される。
     * 古い主題値は監視が解除される。
     * 
     * @param newSubject 新しい主題値。
     */
    public void setSubject(S newSubject) {
        if (this.subject != null)
            deafTo(this.subject);
        
        this.subject = newSubject;
        if (newSubject != null)
            listenTo(newSubject);
        
        updateValueFromSubject();
    }
    
    /**
     * このモデルの値を主題値から取得し、更新を行う。
     * 
     * 主題値が {@code null} の時は、新しい値として {@code null} がこのモデルに適用される。
     * それ以外は、実装によって異なる値がこのモデルに適用される。
     * 
     * @see #getSubject()
     * @see #setValue(Object)
     */
    public void updateValueFromSubject() {
        S subj = getSubject();
        if (subj == null)
            return;
        
        final T newValue = getSubjectValue(subj);
        
        // 現在値の更新は updateSubject を呼び出すが、主題値によっては
        // 複数の値変換（delete & insert イベント）を呼び出すことがあるので、
        // 現在の値と主題の値が異なる時のみに、現在の値の更新を行う。
        T currentValue = getValue();
        
        if (currentValue != null && !currentValue.equals(newValue) ||
                currentValue == null && newValue != null) {
            setValue(newValue);
        }
    }
    
    /**
     * 主題値に、このモデルの値を適用する。
     * 
     * このモデルが保持している値が {@code null} の時は、主題値の更新は行われない。
     * 
     * @see #getValue()
     */
    protected void updateSubject() {
        S sub = getSubject();
        T myValue = getValue();
        if (sub != null && myValue != null)
            updateSubjectValue(sub, myValue);
    }
    
    /**
     * 主題値からこのモデルが保持する型の値へ変換する。
     * 
     * 通常、このメソッドに {@code null} が渡されることはない。
     * 
     * @param subject 主題値。
     * @return 変換された値。
     */
    abstract T getSubjectValue(S subject);
    
    /**
     * 主題値を変更する。
     * 
     * 適用する値が主題値の定義にそぐわない時は、実際に変更を行わなくても良い。
     * @param subject 新しい値が適用される主題値。
     * @param newValue 適用する値。
     */
    abstract void updateSubjectValue(S subject, T newValue);
    
    /**
     * 主題値の変化を監視する処理を行う。
     * 
     * @param newSubject あたらしい主題値。
     */
    abstract void listenTo(S newSubject);
    
    /**
     * 主題値の監視をやめる処理を行う。
     * 
     * @param newSubject 監視をやめる主題値。
     */
    abstract void deafTo(S newSubject);
}
