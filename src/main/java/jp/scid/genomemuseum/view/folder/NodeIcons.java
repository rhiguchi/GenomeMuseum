package jp.scid.genomemuseum.view.folder;

import static java.lang.String.*;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class NodeIcons {
    private final static NodeIcons SINGLETON = new NodeIcons();
    
    private NodeIcons() {
    }
    
    public static NodeIcons getInstance() {
        return SINGLETON;
    }
    
    public Icon book() {
        return getResourceIcon("book.png");
    }
    
    public Icon computer() {
        return getResourceIcon("computer.png");
    }
    
    public Icon folder() {
        return getResourceIcon("folder.png");
    }

    private static Icon getResourceIcon(String name) {
        URL url = NodeIcons.class.getResource(name);
        if (url == null) {
            throw new IllegalStateException(format("resource for %s is not found", name));
        }
        return new ImageIcon(url);
    }
}
