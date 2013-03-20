package jp.scid.gui.control;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrameController {
    
    private FrameController() {
    }
    
    public static class DisposeHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            // TODO Auto-generated method stub
            super.windowClosing(e);
        }
    }
}
