package jp.scid.gui.model;

/**
 * 未定義 ({@code null}) 以外の値を保持するモデル。
 * 
 * このモデルの {@link #getValue()} は {@code null} を返さないことが保証されている。
 * また {@link #setValue(Object)} に {@code null} を渡すことは、初期値に戻すことを表す。
 * 
 * @author Ryusuke Higuchi
 *
 * @param <T> 保持する値の型。
 */
public class NonNullValueModel<T> extends SimpleValueModel<T> {
    /**
     * {@code null} を設定しようとする時の振る舞い。
     * @author Ryusuke Higuchi
     *
     */
    public static enum NullValueSettingStrategy {
        /** モデルの値の更新を行わない */
        IGNORE {
            @Override
            <T> void updateValue(NonNullValueModel<T> model) {
                // Do nothing
            }
        },
        /** {@code null} 用の値が設定される。 */
        REPLACE_INITIAL_VALUE {
            @Override
            <T> void updateValue(NonNullValueModel<T> model) {
                model.setValue(model.valueForNull);
            }
        },
        /** 例外を送出する。 */
        THROWS_EXCEPTION {
            @Override
            <T> void updateValue(NonNullValueModel<T> model) {
                throw new IllegalArgumentException("newValue must not be null");
            }
        },
        ;
        
        abstract <T> void updateValue(NonNullValueModel<T> model);
    }
    private T valueForNull;
    private NullValueSettingStrategy nullValueSettingStrategy = NullValueSettingStrategy.IGNORE;

    /**
     * モデルを構築。
     * @param valueForNull 初期値、および未定義値の代替値。
     * @throws IllegalArgumentException 初期値が {@code null} のとき。
     */
    public NonNullValueModel(T valueForNull) throws IllegalArgumentException {
        if (valueForNull == null)
            throw new IllegalArgumentException("Need a non null value");
        
        setValue(valueForNull);
        this.valueForNull = valueForNull;
    }
    
    /**
     * モデルを構築。
     * @param valueForNull 初期値、および未定義値の代替値。
     * @throws IllegalArgumentException 初期値が {@code null} のとき。
     */
    public NonNullValueModel(T valueForNull, NullValueSettingStrategy nullValueSettingStrategy)
            throws IllegalArgumentException {
        this(valueForNull);
        
        setNullValueSettingStrategy(nullValueSettingStrategy);
    }
    
    /**
     * 現在の {@code null} 値設定のときの振る舞いを取得する。
     * @return 現在の振る舞い。
     */
    final public NullValueSettingStrategy getNullValueSettingStrategy() {
        return nullValueSettingStrategy;
    }
    
    /**
     * {@code null} 値設定のときの振る舞いを設定する。
     * @param strategy 新しい振る舞い
     */
    public void setNullValueSettingStrategy(NullValueSettingStrategy strategy) {
        if (strategy == null)
            throw new IllegalArgumentException("stragtegy must not be null");
        
        this.nullValueSettingStrategy = strategy;
    }
    
    @Override
    final public T getValue() {
        return super.getValue();
    }
    
    /**
     * 値を設定する。
     * {@code newValue} が {@code null} の時は、 {@code nullValueSettingStrategy} によって
     * 異なる処理が行われる。
     * 
     * @throws IllegalArgumentException {@code newValue} が {@code null} でかつ {@code nullValueSettingStrategy} が
     * {@link NullValueSettingStrategy#THROWS_EXCEPTION} である時。
     */
    @Override
    public void setValue(T newValue) {
        if (newValue == null) {
            nullValueSettingStrategy.updateValue(this);
        }
        else {
            super.setValue(newValue);
        }
    }
    
    public void setValueForNull(T valueForNull) {
        if (valueForNull == null)
            throw new IllegalArgumentException("valueForNull must not be null");
        
        this.valueForNull = valueForNull;
    }

    @Override
    public String toString() {
        return "NonNullValueModel [value=" + getValue()
            + ", strategy=" + nullValueSettingStrategy + "]";
    }
}
