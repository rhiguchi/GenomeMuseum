package jp.scid.genomemuseum.view;

import static javax.swing.SpringLayout.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;

public class TaskProgressView {
    private final JLabel statusLabel = new JLabel("status");
    
    private final JButton executeButton = new JButton("Download"); {
        executeButton.setMargin(new Insets(0, 0, 0, 0));
    }
    
    private final JProgressBar downloadProgress = new JProgressBar();  {
        downloadProgress.setMaximum(10000);
    }
    
    private final JPanel component;
    
    public TaskProgressView() {
        component = new JPanel();
        component.setOpaque(false);
        
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
        layout.putConstraint(WEST, downloadProgress, 4, EAST, statusLabel);
        
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
    
    public void setFont(Font font) {
        component.setFont(font);
        
        statusLabel.setFont(font);
        executeButton.setFont(font);
    }
    
    public void setForeground(Color fg) {
        component.setForeground(fg);
        
        statusLabel.setForeground(fg);
    }
}