package jp.scid.genomemuseum.view.folder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.folder.FoldersContainer;

import com.explodingpixels.macwidgets.MacWidgetFactory;
import com.explodingpixels.macwidgets.SourceListBadgeContentProvider;
import com.explodingpixels.macwidgets.SourceListColorScheme;
import com.explodingpixels.macwidgets.SourceListCountBadgeRenderer;
import com.explodingpixels.macwidgets.SourceListStandardColorScheme;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class FolderTreeCellRenderer extends SourceListTreeCellRenderer {
    private DefaultTreeCellRenderer delegate;
    
    public FolderTreeCellRenderer() {
        this.delegate = new DefaultTreeCellRenderer();
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        String text = getText(value);
        if (text == null) {
            text = tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        }
        Icon icon = getIcon(value);
        setIcon(icon);
        
        return super.getTreeCellRendererComponent(tree, text, selected, expanded, leaf, row, hasFocus);
    }
    
    private Icon getIcon(Object object) {
        if (object instanceof DefaultMutableTreeNode) {
            return getIcon(((DefaultMutableTreeNode) object).getUserObject());
        }
        else if (object instanceof SequenceLibrary) {
            return NodeIcons.getInstance().computer();
        }
        else if (object instanceof FoldersContainer) {
            return NodeIcons.getInstance().folder();
        }
        else if (object instanceof Folder) {
            return NodeIcons.getInstance().book();
        }
        return null;
    }
    
    private String getText(Object object) {
        if (object instanceof DefaultMutableTreeNode) {
            return getText(((DefaultMutableTreeNode) object).getUserObject());
        }
        
        return null;
    }
}


abstract class SourceListTreeCellRenderer implements TreeCellRenderer {
    private final Font CATEGORY_FONT =
            UIManager.getFont("Label.font").deriveFont(Font.BOLD, 11.0f);
    private final Font ITEM_FONT = UIManager.getFont("Label.font").deriveFont(11.0f);
    private final Font ITEM_SELECTED_FONT = ITEM_FONT.deriveFont(Font.BOLD);
    
    private final SourceListColorScheme fColorScheme;

    private final CategoryTreeCellRenderer iCategoryRenderer;

    private final ItemTreeCellRenderer iItemRenderer;

    private Icon icon;
    
    protected SourceListTreeCellRenderer() {
        fColorScheme = new SourceListStandardColorScheme();
        
        iCategoryRenderer = new CategoryTreeCellRenderer(new JLabel());
        
        iItemRenderer = new ItemTreeCellRenderer(new JLabel());
    }
    
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {

        boolean isCategoryRow =
                tree.getPathForRow(row) != null && tree.getPathForRow(row).getPathCount() <= 2;
        
        TreeCellRenderer render = isCategoryRow ? iCategoryRenderer : iItemRenderer;
        JLabel label = (JLabel) render.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);
        label.setIcon(icon);
        return label;
    }

    protected void setIcon(Icon icon) {
        this.icon = icon;
    }
    
    private class CategoryTreeCellRenderer implements TreeCellRenderer {
        private final JLabel label;
        
        public CategoryTreeCellRenderer(JLabel label) {
            this.label = label;
        }
        
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            MacWidgetFactory.makeEmphasizedLabel(new JLabel(),
                    fColorScheme.getCategoryTextColor(),
                    fColorScheme.getCategoryTextColor(),
                    fColorScheme.getCategoryTextShadowColor());
            label.setFont(CATEGORY_FONT);
            
            String text = String.valueOf(value).toUpperCase();
            label.setText(text);
            return label;
        }
    }
    
    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
    
    private class ItemTreeCellRenderer implements TreeCellRenderer {
        private final JLabel label;
        
        private ItemTreeCellRenderer(JLabel label) {
            this.label = label;
        }
        
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            if (selected) {
                MacWidgetFactory.makeEmphasizedLabel(label,
                        fColorScheme.getSelectedItemTextColor(),
                        fColorScheme.getSelectedItemTextColor(),
                        fColorScheme.getSelectedItemFontShadowColor());
                label.setFont(ITEM_SELECTED_FONT);
            }
            else {
                MacWidgetFactory.makeEmphasizedLabel(label,
                        fColorScheme.getUnselectedItemTextColor(),
                        fColorScheme.getUnselectedItemTextColor(),
                        TRANSPARENT_COLOR);
                label.setFont(ITEM_FONT);
            }
            label.setText(String.valueOf(value));
            return label;
        }
    }
    
    static class WhiteIcon implements Icon {
        private final Icon base;
        
        public WhiteIcon(Icon base) {
            this.base = base;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            
            BufferedImage baseImage = gc.createCompatibleImage(getIconWidth(), getIconHeight(), Transparency.TRANSLUCENT);
            
            Graphics2D baseImageGraphics = baseImage.createGraphics();
            base.paintIcon(c, baseImageGraphics, 0, 0);
            
            float scaleFactor = 0.3f;
            float offset = 255f;
            RescaleOp op = new RescaleOp(new float[]{scaleFactor, scaleFactor, scaleFactor, 1f},
                    new float[]{offset, offset, offset, 0}, null);
            BufferedImage filtered = op.filter(baseImage, null);
            
            g.drawImage(filtered, x, y, c);
        }

        @Override
        public int getIconWidth() {
            return base.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return base.getIconHeight();
        }
        
    }
}

