package jp.scid.genomemuseum.model;

import javax.swing.SwingWorker.StateValue;
import javax.swing.event.ChangeListener;
/**
 * タスクの進捗状況を表現するモデル。
 * @author higuchi
 *
 */
public interface TaskProgressModel {
    /**
     * タスクの進捗。0 が最小 で 100 が最大。
     * @return 進捗数。
     */
    public float getProgress();
    
    /**
     * このタスクを表す名前。
     * @return 表示名
     */
    public String getLabel();
    
    /**
     * タスクが利用可能であるか。
     * @return 利用可能のときは {@code true} 。
     */
    public boolean isAvailable();
    
    /**
     * タスクが実行中であるかを表す {@link StateValue} の値。
     * @return 未実行の時は {@code PENDING} 、実行中は {@code STARTED}  、終了済みは {@code DONE} 。
     */
    public StateValue getState();
    
    void addProgressChangeListener(ChangeListener l);
    
    void removeProgressChangeListener(ChangeListener l);
}

