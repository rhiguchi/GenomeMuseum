package jp.scid.gui.control;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingWorker;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class TaskProgressController implements PropertyChangeListener {
    protected final ValueModel<Boolean> running;
    protected final ValueModel<Integer> progress;
    
    public TaskProgressController(ValueModel<Boolean> running, ValueModel<Integer> progress) {
        this.running = running;
        this.progress = progress;
    }
    
    public TaskProgressController() {
        this(ValueModels.newBooleanModel(false), ValueModels.newIntegerModel(0));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SwingWorker<?, ?> task = (SwingWorker<?, ?>) evt.getSource();
        
        if ("state".equals(evt)) {
            updateRunning(task);
        }
        else if ("progress".equals(evt)) {
            updateProgress(task);
        }
    }
    
    protected void updateRunning(SwingWorker<?, ?> task) {
        switch (task.getState()) {
        case STARTED:
            setRunning(true);
            break;
        case DONE:
            setRunning(false);
            break;
        }
    }
    
    protected void updateProgress(SwingWorker<?, ?> task) {
        int value = task.getProgress();
        
        progress.setValue(value);
    }
    
    public boolean isRunning() {
        return running.getValue();
    }
    
    public void setRunning(boolean newValue) {
        running.setValue(newValue);
    }
    
    public void listenTo(SwingWorker<?, ?> task) {
        task.addPropertyChangeListener(this);
    }
    
    public void deafTo(SwingWorker<?, ?> task) {
        task.removePropertyChangeListener(this);
    }
}
