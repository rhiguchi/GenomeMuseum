package jp.scid.gui.model;

import java.beans.PropertyChangeListener;

/**
 * 単一の値を保持するオブジェクトの表現。
 * 
 * @author Ryusuke Higuchi
 *
 * @param <T> 保持されている値の型。
 */
public interface ValueModel<T> {
    /**
     * このモデルの現在の値を返す。
     * 
     * {@code null} を返すことも可能であるが、これは読み込み禁止を意味し、意味のある値として利用してはならない。
     * この値を利用する操作系はこのメソッドから {@code null} を返された時は、何も動作をしないことが期待される。
     * 
     * @return モデルの値。
     */
    T getValue();
    
    /**
     * このモデルに新しい値を適用する。
     * 
     * 実装は、値が更新されたときに {@link #addPropertyChangeListener(PropertyChangeListener)} によって
     * 追加されたリスナーへ変更を通知する必要がある。この時のイベントオブジェクトの
     * {@code propetyName} は "{@code value}" とする。
     * 
     * 実装が読み込み専用のときは、値を更新せず、また {@code PropertyChangeListener} の
     * 通知も行わないようにする。
     * 
     * @param newValue 適用する値。
     */
    void setValue(T newValue);
    
    /**
     * モデルの値の変化を監視するリスナーを登録する。
     * 
     * このモデルの値の変化は {@code propetyName} を "{@code value}" とするイベントオブジェクトで通知される。
     * 
     * @param listener 追加するリスナー
     */
    void addPropertyChangeListener(PropertyChangeListener listener);
    
    /**
     * リスナーのモデルの値の監視をやめる。
     * 
     * @param listener 監視をやめるリスナー。
     */
    void removePropertyChangeListener(PropertyChangeListener listener);
}
