package jp.scid.genomemuseum.view;

import static javax.swing.Spring.*;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SpringLayout.Constraints;

public class MainView {
    public final JPanel contentPane = new JPanel();
    public final JTable dataTable = new JTable();
    public final JScrollPane dataTableScroll = new JScrollPane(dataTable,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    public final JTextField quickSearchField = new JTextField();
    
    public MainView() {
        SpringLayout layout = new SpringLayout();
        contentPane.setLayout(layout);
        contentPane.setPreferredSize(new Dimension(600, 400));
        
        final Constraints container = layout.getConstraints(contentPane);
        
        contentPane.add(dataTableScroll);
        contentPane.add(quickSearchField);
        
        // Layout
        {
            Constraints c = layout.getConstraints(dataTableScroll);
            c.setX(container.getX());
            c.setY(sum(container.getY(), constant(64)));
            c.setWidth(container.getWidth());
            c.setConstraint("South", container.getConstraint("South"));
        }
        {
            Constraints c = layout.getConstraints(quickSearchField);
            c.setConstraint("East", sum(container.getConstraint("East"), constant(-16)));
            c.setY(sum(container.getY(), constant(16)));
            c.setWidth(constant(80, 120, 200));
        }
    }
}
