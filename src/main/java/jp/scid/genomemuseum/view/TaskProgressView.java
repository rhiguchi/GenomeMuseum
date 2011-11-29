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
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Spring;
import javax.swing.SpringLayout;

class TaskProgressView extends JPanel {
    private final JLabel statusLabel = new JLabel("status");
    private boolean isInitialized = false;
    
    private final JButton executeButton = new JButton("Download"); {
        executeButton.setMargin(new Insets(0, 0, 0, 0));
    }
    
    private final JProgressBar downloadProgress = new JProgressBar();  {
        downloadProgress.setMaximum(10000);
    }
    
    public TaskProgressView() {
        JComponent parent = this;
        SpringLayout layout = new SpringLayout();
        parent.setLayout(layout);
        
        Spring dlWidth = constant(60);
        
        parent.add(executeButton);
        layout.putConstraint(EAST, executeButton, 0, EAST, parent);
        layout.putConstraint(NORTH, executeButton, 0, NORTH, parent);
        layout.putConstraint(SOUTH, executeButton, 0, SOUTH, parent);
        layout.getConstraints(executeButton).setWidth(dlWidth);
        
        parent.add(downloadProgress);
        layout.putConstraint(EAST, downloadProgress, 0, EAST, parent);
        layout.putConstraint(NORTH, downloadProgress, 0, NORTH, parent);
        layout.putConstraint(SOUTH, downloadProgress, 0, SOUTH, parent);
        layout.getConstraints(downloadProgress).setWidth(dlWidth);
        
        parent.add(statusLabel);
        layout.putConstraint(WEST, statusLabel, 0, WEST, parent);
        layout.putConstraint(NORTH, statusLabel, 0, NORTH, parent);
        layout.putConstraint(SOUTH, statusLabel, 0, SOUTH, parent);
        layout.putConstraint(EAST, statusLabel, -4, WEST, executeButton);
        
        isInitialized = true;
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
        if (!isInitialized) return;
        
        statusLabel.setFont(font);
        executeButton.setFont(font);
    }
    
    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        if (!isInitialized) return;
        
        statusLabel.setForeground(fg);
    }
}