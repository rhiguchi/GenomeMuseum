package jp.scid.gui;

import java.text.FieldPosition;
import java.text.Format;

import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;


public class MessageFormatTableCell extends AbstractTableCellComponent {
    private final JLabel cell = new JLabel();
    
    private final Format fromat;
    private final StringBuffer sb = new StringBuffer();
    private final FieldPosition pos = new FieldPosition(0);
    
    protected String nullLabel = "";
    
    public MessageFormatTableCell(Format fromat, TableCellRenderer baseRenderer) {
        super(baseRenderer);
        this.fromat = fromat;
    }
    
    public MessageFormatTableCell(Format fromat) {
        super();
        this.fromat = fromat;
    }
    
    @Override
    public JLabel getRendererView() {
        return cell;
    }

    @Override
    protected void setValueToRendererView(Object value) {
        if (value == null) {
            cell.setText(nullLabel);
            return;
        }
        
        sb.delete(0, sb.length());
        pos.setBeginIndex(0);
        pos.setEndIndex(0);
        
        fromat.format(value, sb, pos);
        cell.setText(sb.toString());
    }
}
