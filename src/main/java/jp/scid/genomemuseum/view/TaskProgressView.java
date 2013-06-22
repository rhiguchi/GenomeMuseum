package jp.scid.genomemuseum.view;

import static javax.swing.Spring.*;
import static javax.swing.SpringLayout.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

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
        executeButton.setAction(action);
    }
    
    public void setDownloadButtonEnabled(boolean isEnabled) {
        executeButton.setEnabled(isEnabled);
    }
    
    public void setLabelAvailable(boolean isEnabled) {
        statusLabel.setEnabled(isEnabled);
    }
    
    public void setText(String statusText) {
        statusLabel.setText(statusText);
    }
    
    public void setProgressVisible(boolean b) {
        downloadProgress.setVisible(b);
        executeButton.setVisible(!b);
    }

    public void setProgress(float value) {
        final int progress = (int) value * 100;
        downloadProgress.setValue(progress);
        downloadProgress.setIndeterminate(value < 0
                || downloadProgress.getMaximum() <= progress);
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
        setLabelAvailable(model.isAvailable());
        setDownloadButtonEnabled(model.isAvailable() && model.getState() == StateValue.PENDING);
        setProgressVisible(model.getState() == StateValue.STARTED);
        setProgress(model.getProgress());
        setText(model.getLabel());
        
        repaint();
    }
    
    public TaskProgressModel getModel() {
        return model;
    }

    public void setModel(TaskProgressModel model) {
        if (this.model != null) {
            this.model.removeProgressChangeListener(this);
        }
        this.model = model;
        
        if (model != null) {
            model.addProgressChangeListener(this);
            updateComponentValues();
        }
    }
}