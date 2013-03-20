package jp.scid.gui.control;

import static org.junit.Assert.*;

import javax.swing.JOptionPane;

import org.junit.Test;

public class OptionPaneControllerTest {

    OptionPaneController ctrl = null;
    
    OptionPaneController createController() {
        return new OptionPaneController();
    }
    
    public static void main(String[] args) {
        final OptionPaneControllerTest test = new OptionPaneControllerTest();
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                test.createController().showMessage("test asdfasasdffsd asdvasd asdf afasdff oaisdf;oi aoisjdf asfdj as as;ldfj w e;fl jwe w wefjwefpjaw  apw:eofj p:ejf:aw f:", "desc");
            }
        });
    }
}
