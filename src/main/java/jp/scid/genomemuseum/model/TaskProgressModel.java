package jp.scid.genomemuseum.model;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingWorker.StateValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
/**
 * タスクの進捗状況を表現するモデル。
 * @author higuchi
 *
 */
public interface TaskProgressModel {
    /**
     * タスクの大きさを返します。
     * 
     * @return タスクの大きさ。0 の場合もある。
     */
    long getTaskSize();
    
    /**
     * タスクの進捗を返します
     * 
     * 0 が最小 で {@link #getTaskSize()} が最大です。
     * 
     * @return 進捗数
     */
    public long getTaskProgress();
    
    /**
     * タスクが実行中であるかを表す {@link StateValue} の値。
     * @return 未実行の時は {@code PENDING} 、実行中は {@code STARTED}  、終了済みは {@code DONE} 。
     */
    public StateValue getTaskState();
    
    URI sourceUri();
    
    void addTaskStateChangeListener(ChangeListener l);
    
    void removeTaskStateChangeListener(ChangeListener l);
}

abstract class AbstractTaskProgressModel implements TaskProgressModel {
    private final List<ChangeListener> listeners = new LinkedList<ChangeListener>();

    public void addTaskStateChangeListener(ChangeListener l) {
        listeners.add(l);
    }
    
    public void removeTaskStateChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    protected void fireTaskStateChange() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l: listeners) {
            l.stateChanged(e);
        }
    }
}