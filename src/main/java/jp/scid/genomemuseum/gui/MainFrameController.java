package jp.scid.genomemuseum.gui;

import java.util.EventObject;

import javax.swing.JFrame;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Application.ExitListener;

public class MainFrameController extends AbstractBean implements ExitListener {
    private JFrame view;
    
    // Properties
    public MainFrameController() {
    }
    
    public JFrame getView() {
        if (view == null) {
            view = new JFrame();
        }
        
        return view;
    }
    
    public void setView(JFrame view) {
        this.view = view;
    }
    
    // Actions
    public void show() {
        getView().setVisible(true);
    }
    
    @Override
    public boolean canExit(EventObject event) {
        return true;
    }

    @Override
    public void willExit(EventObject event) {
        // do nothing
    }
}