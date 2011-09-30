package jp.scid.genomemuseum.view;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MainViewMenuBar {
    public final JMenuBar menuBar = new JMenuBar();
    
    public final JMenu fileMenu = new JMenu("file");
    public final JMenu editMenu = new JMenu("edit");
    
    // for fileMenu
    public final JMenuItem open = new JMenuItem("open");
    public final JMenuItem quit = new JMenuItem("quit");
    
    // for editMenu
    public final JMenuItem undo = new JMenuItem("undo");
    public final JMenuItem cut = new JMenuItem("cut");
    public final JMenuItem copy = new JMenuItem("copy");
    public final JMenuItem paste = new JMenuItem("paste");
    public final JMenuItem delete = new JMenuItem("delete");
    
    
    public MainViewMenuBar() {
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        
        fileMenu.add(open);
        fileMenu.addSeparator();
        fileMenu.add(quit);
        
        editMenu.add(undo);
        editMenu.addSeparator();
        editMenu.add(cut);
        editMenu.add(copy);
        editMenu.add(paste);
        editMenu.addSeparator();
        editMenu.add(delete);
    }
}
