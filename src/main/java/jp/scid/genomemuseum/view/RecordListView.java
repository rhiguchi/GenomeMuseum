package jp.scid.genomemuseum.view;

import java.awt.Dimension;
import java.text.DecimalFormat;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import com.explodingpixels.macwidgets.plaf.ITunesTableUI;

import jp.scid.gui.MessageFormatTableCell;

public abstract class RecordListView {
    final JTable table;
    final JScrollPane tableContainer;
    final JTextField searchField;
    
    public RecordListView() {
        table = createTable();
        
        tableContainer = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(200, 28));
    }
    
    public JTable getTable() {
        return table;
    }
    
    public JScrollPane getTableContainer() {
        return tableContainer;
    }
    
    public JTextField getSearchField() {
        return searchField;
    }
    
    protected JTable createTable() {
        JTable table = new JTable();
        
        table.setUI(new ITunesTableUI());
        TableCellRenderer defaultRenderer = table.getDefaultRenderer(Object.class);
        
        MessageFormatTableCell intValueCell =
                new MessageFormatTableCell(new DecimalFormat("#,##0"), defaultRenderer);
        intValueCell.getRendererView().setHorizontalAlignment(SwingConstants.RIGHT);
        table.setDefaultRenderer(Integer.class, intValueCell);
        table.getTableHeader().setReorderingAllowed(true);
        
        return table;
    }
}
