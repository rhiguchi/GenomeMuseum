package jp.scid.gui.control;

import static java.lang.String.*;

import java.beans.Expression;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.Statement;

import jp.scid.gui.model.ValueModel;

/**
 * 操作クラスを定義する抽象実装。
 * データモデルのプロパティ変化を監視し、変化が起きた際にはその処理を実行する。
 * @author Ryusuke Higuchi
 *
 * @param <M> モデル型
 */
public abstract class AbstractController<M> {
    final PropertyChangeListener modelPropertyChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getProperty() == null || getProperty().equals(evt.getPropertyName()))
                processPropertyChange(getModel(), evt.getPropertyName());
        }
    };

    private M model = null;
    
    @Deprecated
    private String property = null;

    /**
     * モデルの変化監視を解除し、内部のモデル保持も解除する。
     */
    public void release() {
        if (model != null)
            deafTo(model);
        
        model = null;
    }
    
    /**
     * 値変化の処理を行う
     * @param model 変化したモデル。現在のモデル。
     * @param property 変化したプロパティ名。
     */
    abstract protected void processPropertyChange(M model, String property);
    
    /**
     * 現在設定されているモデルを取得する。
     * @return 現在のモデル。
     */
    public M getModel() {
        return model;
    }

    /**
     * モデルを設定する。全てのプロパティの変化の通知を行う。
     * 
     * @param newModel 新しいデータモデル。
     */
    public void setModel(M newModel) {
        if (model != null)
            deafTo(model);
        
        model = newModel;
        
        if (newModel != null)
            listenTo(newModel);
        
        if (model != null) {
            modelChange(model);
        }
    }

    /**
     * モデルを設定する。{@code property} に指定したプロパティのみ変化の通知する。
     * 
     * {@code property} が {@code null} の時は、全てのプロパティの変化の通知を行う。 
     * @param newModel
     * @param property 監視するプロパティ名。
     */
    @Deprecated
    public void setModel(M newModel, String property) {
        this.property = property;
        setModel(newModel);
    }
    
    /**
     * モデルが変化したときに処理を実行する。
     */
    protected void modelChange(M model) {
        processPropertyChange(model, null);
    }
    
    @Deprecated
    public String getProperty() {
        return property;
    }

    /**
     * モデルの変化監視を開始する。
     * @param model 変化を監視するモデル。
     */
    protected void listenTo(M model) {
        if (model == null)
            return;
        if (model instanceof ValueModel) {
            ((ValueModel<?>) model).addPropertyChangeListener(modelPropertyChangeListener);
        }
        else {
            execute(model, "addPropertyChangeListener", modelPropertyChangeListener);
        }
    }
    
    /**
     * モデルの変化監視を解除する。
     * @param model 変化の監視を解除するモデル。
     */
    protected void deafTo(M model) {
        if (model == null)
            return;
        
        if (model instanceof ValueModel) {
            ((ValueModel<?>) model).removePropertyChangeListener(modelPropertyChangeListener);
        }
        else {
            execute(model, "removePropertyChangeListener", modelPropertyChangeListener);
        }
    }
    
    /**
     * メソッドをリフレクションを用いて実行する。
     * @param target メソッドを呼び出すオブジェクト。
     * @param methodName メソッド名
     * @param arguments メソッド引数
     * @throws IllegalStateException 実行時に例外が発生したとき
     */
    static void execute(Object target, String methodName, Object... arguments) throws IllegalStateException {
        Statement statement = new Statement(target, methodName, arguments);
        try {
            statement.execute();
        }
        catch (Exception e) {
            throw new IllegalStateException(format(
                    "Cannot execute %s on an object for %s", methodName, target.getClass()), e);
        }
    }

    static Object getBeanPropertyValue(Object model, String property) {
        String methodName = AbstractController.getGetterName(property);
        Expression statement = new Expression(model, methodName, null);
        
        try {
            return statement.getValue();
        }
        catch (Exception e) {
            throw new IllegalStateException(format(
                    "Cannot execute %s to an object for %s", methodName, model.getClass()), e);
        }
    }
    
    static String getSetterName(String propertyName) {
        return "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }
    
    static String getGetterName(String propertyName) {
        return "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }

}
