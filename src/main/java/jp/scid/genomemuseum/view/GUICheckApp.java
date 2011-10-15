package jp.scid.genomemuseum.view;

import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jdesktop.application.Application;
import org.jdesktop.application.FrameView;

class GUICheckApp extends Application {
    private JComponent contentPane = new JPanel();
    private FrameView view = new FrameView(this);
    
    GUICheckApp() {
        getContext().getResourceManager().setResourceFolder("");
    }
    
    @Override
    protected void initialize(String[] args) {
        List<String> argList = Arrays.asList(args);
        if (argList.contains("ColumnVisibilitySetting")) {
            ColumnVisibilitySetting pane = new ColumnVisibilitySetting();
            contentPane = pane.contentPane;
        }
    }
    @Override
    protected void startup() {
        view.setComponent(contentPane);
    }
    
    @Override
    protected void ready() {
        view.getFrame().setLocationRelativeTo(null);
        view.getFrame().pack();
        view.getFrame().setVisible(true);
    }
}