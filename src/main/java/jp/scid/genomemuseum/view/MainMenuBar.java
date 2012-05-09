package jp.scid.genomemuseum.view;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MainMenuBar {
    // File menu
    public final JMenuItem newCollectionBox = new JMenuItem("New Collection Box");
    public final JMenuItem newGroupBox = new JMenuItem("New Group Box");
    public final JMenuItem newSmartBox = new JMenuItem("New Smart Box");
    
    public final JMenuItem open = new JMenuItem("Open");
    public final JMenuItem quit = new JMenuItem("Quit");
    
    public final JMenu fileMenu = createFileMenu(newCollectionBox, newGroupBox, newSmartBox, open, quit);
    
    // Edit
    public final JMenuItem cut = new JMenuItem("Cut");
    public final JMenuItem copy = new JMenuItem("Copy");
    public final JMenuItem paste = new JMenuItem("Paste");
    public final JMenuItem delete = new JMenuItem("Delete");
    
    public final JMenuItem selectAll = new JMenuItem("selectAll");
    public final JMenuItem deselect = new JMenuItem("deselect");
    
    public final JMenu editMenu = createEditMenu(cut, copy, paste, delete, selectAll, deselect); 
    
    // View
    public final JMenuItem viewOption = new JMenuItem("View Option...");
    
    public final JMenu viewMenu = createViewMenu(viewOption); 
    
    // MenuBar
    public final JMenuBar menuBar = createMenuBar(fileMenu, editMenu);
    
    public MainMenuBar() {
    }
    
    public JMenuBar getMenuBar() {
        return menuBar;
    }
    
    static JMenu createFileMenu(
            JMenuItem newCollectionBox, JMenuItem newGroupBox, JMenuItem newSmartBox,
            JMenuItem open, JMenuItem quit) {
        JMenu menu = new JMenu("File");
        
        menu.add(newSmartBox);
        menu.add(newGroupBox);
        menu.addSeparator();
        menu.add(open);
        menu.addSeparator();
        menu.add(quit);
        
        return menu;
    }
    
    static JMenu createEditMenu(JMenuItem cut, JMenuItem copy, JMenuItem paste, JMenuItem delete,
            JMenuItem selectAll, JMenuItem deselect) {
        JMenu menu = new JMenu("Edit");
        
        menu.add(cut);
        menu.add(copy);
        menu.add(paste);
        menu.add(delete);
        menu.addSeparator();
        menu.add(selectAll);
        menu.add(deselect);
        
        return menu;
    }
    
    static JMenu createViewMenu(JMenuItem viewOption) {
        JMenu menu = new JMenu("Edit");
        
        menu.add(viewOption);
        
        return menu;
    }
    
    static JMenuBar createMenuBar(JMenu fileMenu, JMenu editMenu) {
        JMenuBar menuBar = new JMenuBar();
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        
        return menuBar;
    }
}
