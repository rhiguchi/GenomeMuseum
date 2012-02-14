package jp.scid.genomemuseum.view;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import jp.scid.genomemuseum.model.ExhibitRoom;

public class ExhibitRoomListTreeCellRendererProxy implements TreeCellRenderer {
    private TreeCellRenderer adaptee;
    
    public ExhibitRoomListTreeCellRendererProxy(TreeCellRenderer adaptee) {
        this.adaptee = adaptee;
    }
    
    public ExhibitRoomListTreeCellRendererProxy() {
        this(new DefaultTreeCellRenderer());
    }
    
    public void setRenderer(TreeCellRenderer adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        if (value instanceof ExhibitRoom) {
            ExhibitRoom room = (ExhibitRoom) value;
            value = room.name();
        }
        
        Component cell = adaptee.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        return cell;
    }
}
