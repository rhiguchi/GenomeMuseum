package jp.scid.genomemuseum.view;

import static java.lang.String.*;
import static javax.swing.Spring.*;
import static javax.swing.SpringLayout.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.text.DecimalFormat;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;
import javax.swing.SwingWorker.StateValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jp.scid.genomemuseum.model.TaskProgressModel;

public class TaskProgressView extends JComponent implements ChangeListener {
    private final static String DEFAULT_PROGRESS_MESSAGE_FORMAT = "%s / %s...";
    private final static String DEFAULT_SUCCESS_MESSAGE_FORMAT = "%s Done";
    private final static String DEFAULT_CANCELLED_MESSAGE_FORMAT = "Cancelled";
    
    private final JLabel statusLabel = new JLabel("status");
    private final JButton executeButton;
    private final JProgressBar downloadProgress;
    
    private final JComponent component;
    
    private TaskProgressModel model;
    
    public TaskProgressView() {
        component = this;
        
        executeButton = new JButton("Download");
        executeButton.setMargin(new Insets(0, 0, 0, 0));
        
        downloadProgress = new JProgressBar(0, 10000);
        downloadProgress.setIndeterminate(true);
        
        JComponent parent = component;
        SpringLayout layout = new SpringLayout();
        parent.setLayout(layout);
        
        parent.add(executeButton);
        layout.putConstraint(EAST, executeButton, 0, EAST, parent);
        layout.putConstraint(NORTH, executeButton, 0, NORTH, parent);
        layout.putConstraint(SOUTH, executeButton, 0, SOUTH, parent);
        
        parent.add(downloadProgress);
        layout.putConstraint(EAST, downloadProgress, 0, EAST, parent);
        layout.putConstraint(NORTH, downloadProgress, 0, NORTH, parent);
        layout.putConstraint(SOUTH, downloadProgress, 0, SOUTH, parent);
        layout.getConstraints(downloadProgress).setConstraint(WEST,
                max(sum(layout.getConstraint(EAST, downloadProgress), constant(-100)),
                        layout.getConstraint(EAST, statusLabel)));
        
        parent.add(statusLabel);
        layout.putConstraint(WEST, statusLabel, 4, WEST, parent);
        layout.putConstraint(NORTH, statusLabel, 0, NORTH, parent);
        layout.putConstraint(SOUTH, statusLabel, 0, SOUTH, parent);
    }

    public JComponent getComponent() {
        return component;
    }
    
    public void setExecuteButtonAction(Action action) {
        executeButton.addActionListener(action);
    }
    
    void setLabelText(StateValue state, long maxSize, long currentSize) {
        final String message;
        if (state == StateValue.STARTED) {
            String max = readableFileSize(maxSize);
            String current = readableFileSize(currentSize);
            message = format(DEFAULT_PROGRESS_MESSAGE_FORMAT, current, max);
            
        }
        else if (state == StateValue.DONE) {
            if (maxSize <= currentSize) {
                String max = readableFileSize(maxSize);
                message = format(DEFAULT_SUCCESS_MESSAGE_FORMAT, max);
            }
            else {
                message = format(DEFAULT_CANCELLED_MESSAGE_FORMAT);
            }
        }
        else {
            message = "";
        }
        statusLabel.setText(message);
    }
    
    public void setProgressVisible(boolean b) {
        downloadProgress.setVisible(b);
    }

    void setExecuteButtonEnabled(boolean isEnabled) {
        executeButton.setEnabled(isEnabled);
    }
    
    void setExecuteButtonVisible(boolean b) {
        executeButton.setVisible(b);
    }

    public void setProgress(long maxSize, long currentSize) {
        final int progress = (int) (currentSize * 10000.0 / maxSize);
        
        downloadProgress.setValue(progress);
        downloadProgress.setIndeterminate(currentSize < 0 || maxSize <= currentSize);
    }
    
    @Override
    public void setFont(Font font) {
        super.setFont(font);
        
        statusLabel.setFont(font);
        executeButton.setFont(font);
    }
    
    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        
        statusLabel.setForeground(fg);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        updateComponentValues();
    }
    
    private void updateComponentValues() {
        setExecuteButtonEnabled(model.getTaskState() == StateValue.PENDING);
        setLabelText(model.getTaskState(), model.getTaskSize(), model.getTaskProgress());
        
        boolean inProgress = model.getTaskState() == StateValue.STARTED;
        setProgressVisible(inProgress);
        setExecuteButtonVisible(!inProgress);
        
        setProgress(model.getTaskSize(), model.getTaskProgress());
    }
    
    public TaskProgressModel getModel() {
        return model;
    }

    public void setModel(TaskProgressModel model) {
        if (this.model != null) {
            this.model.removeTaskStateChangeListener(this);
        }
        this.model = model;
        
        if (model != null) {
            model.addTaskStateChangeListener(this);
            updateComponentValues();
        }
    }
    
    static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) ( Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}