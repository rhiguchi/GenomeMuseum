package jp.scid.genomemuseum.view.folder;

import static java.lang.String.*;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class NodeIcons {
    private final static NodeIcons SINGLETON = new NodeIcons();
    private Icon bookIcon;
    private Icon computerIcon;
    private Icon folderIcon;
    
    private NodeIcons() {
    }
    
    public static NodeIcons getInstance() {
        return SINGLETON;
    }
    
    public Icon book() {
        if (bookIcon == null) {
            bookIcon = getResourceIcon("book.png");
        }
        return bookIcon;
    }
    
    public Icon computer() {
        if (computerIcon == null) {
            computerIcon = getResourceIcon("computer.png");
        }
        return computerIcon;
    }
    
    public Icon folder() {
        if (folderIcon == null) {
            folderIcon = getResourceIcon("folder.png");
        }
        return folderIcon;
    }

    private static Icon getResourceIcon(String name) {
        URL url = NodeIcons.class.getResource(name);
        if (url == null) {
            throw new IllegalStateException(format("resource for %s is not found", name));
        }
        return new ImageIcon(url);
    }
}
