package jp.scid.gui.model;

/**
 * {@link ValueModel} の変換接続をおこなう。
 * @author Ryusuke Higuchi
 *
 * @param <S> 主題値の型
 * @param <T> 変換された値の型。
 */
public class TransformValueModel<S, T> extends AbstractProxyValueModel<S, T> {
    private final Transformer<? super S, ? extends T> transformer;
    private final Transformer<? super T, ? extends S> reverseTransformer;
    
    /**
     * 主題値とこのモデル値を相互に変換できるモデルを作成する。
     * @param transformer
     * @param reverseTransformer
     */
    public TransformValueModel(
            Transformer<? super S, ? extends T> transformer,
            Transformer<? super T, ? extends S> reverseTransformer) {
        super();
        if (transformer == null)
            throw new IllegalArgumentException("transformer must not be null");
        
        this.transformer = transformer;
        this.reverseTransformer = reverseTransformer;
    }
    
    public TransformValueModel(
            ValueModel<S> subject,
            Transformer<? super S, ? extends T> transformer,
            Transformer<? super T, ? extends S> reverseTransformer) {
        this(transformer, reverseTransformer);
        
        setSubject(subject);
    }
    
    /**
     * 主題値からこのモデルへの値の変換のみを許すモデルを作成する。
     * @param transformer
     */
    public TransformValueModel(Transformer<? super S, ? extends T> transformer) {
        this(transformer, null);
    }
    
    public TransformValueModel(ValueModel<S> subject,
            Transformer<? super S, ? extends T> transformer) {
        this(transformer, null);
        
        setSubject(subject);
    }
    
    /**
     * 主題値からモデル値への変換器を取得する。
     * @return 主題値からモデル値への変換器。
     */
    public Transformer<? super S, ? extends T> getTransformer() {
        return transformer;
    }
    
    /**
     * モデル値から主題値への変換器を取得する。
     * 
     * 主題値からモデル値への変換の一方向のみが可能な時は {@code null} が返される。
     * @return モデル値から主題値への変換器。
     */
    public Transformer<? super T, ? extends S> getReverseTransformer() {
        return reverseTransformer;
    }

    /**
     * 変換オブジェクトから主題値を変換して返す。
     * {@inheritDoc}
     */
    @Override
    T getSubjectValue(ValueModel<S> subject) {
        return getTransformer().apply(subject.getValue());
    }

    /**
     * 逆変換オブジェクトから、主題値を作成して適用する。
     * 
     * 逆変換ができない時はなにもしない。
     */
    @Override
    void updateSubjectValue(ValueModel<S> subject, T newValue) {
        final Transformer<? super T, ? extends S> tf = getReverseTransformer();
        
        if (tf == null)
            return;
        
        S subjectValue = tf.apply(newValue);
        subject.setValue(subjectValue);
    }
    
    /**
     * 値の変換を行う構造定義。
     * @author Ryusuke Higuchi
     *
     * @param <S> 主題値の型。
     * @param <T> 変換値の型。
     */
    public static interface Transformer<S, T> {
        /**
         * 値の変換を行う。
         * @param subject 変換元
         * @return 変換後の値。
         */
        T apply(S subject);
    }
}
