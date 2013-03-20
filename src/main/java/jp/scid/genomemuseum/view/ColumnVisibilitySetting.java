package jp.scid.genomemuseum.view;

import static java.lang.String.*;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder2;

public class ColumnVisibilitySetting implements GenomeMuseumView {
    private final static List<String> propertyNames = Arrays.asList("sequenceLength",
            "accession", "identifier", "namespace", "version", "definition",
            "source", "organism", "date", "sequenceUnit", "moleculeType");
    
    private final static int columnCount = 3; 
    private final static int rowCount = (propertyNames.size() - 1) / columnCount + 1; 
    
    public final JPanel contentPane = new JPanel();
    private final Map<String, JCheckBox> checkBoxes = new HashMap<String, JCheckBox>(); 
    public final JButton okButton = new JButton("OK");
    public final JButton cancelButton = new JButton("Cancel");
    
    public ColumnVisibilitySetting() {
        final JPanel chekchBoxesPane = new JPanel(); 
        {
            GroupLayout layout = new GroupLayout(chekchBoxesPane);
            chekchBoxesPane.setLayout(layout);

            ArrayList<ParallelGroup> columns = new ArrayList<ParallelGroup>();
            ArrayList<ParallelGroup> rows = new ArrayList<ParallelGroup>();
            SequentialGroup columnGroup = layout.createSequentialGroup();
            SequentialGroup rowGroup = layout.createSequentialGroup();

            for (int i = 0; i < columnCount; i++) {
                ParallelGroup column = layout.createParallelGroup();
                columns.add(column);
                columnGroup.addGroup(column);
            }         

            for (int i = 0; i < rowCount; i++) {
                ParallelGroup row = layout.createParallelGroup();
                rows.add(row);
                rowGroup.addGroup(row);
            }

            for (int index = 0; index < propertyNames.size(); index++) {
                String propertyName = propertyNames.get(index);
                JCheckBox checkBox = new JCheckBox(propertyName);
                checkBox.setName(propertyName);
                checkBoxes.put(propertyName, checkBox);
                contentPane.add(checkBox);

                ParallelGroup column = columns.get(index / rowCount);
                ParallelGroup row = rows.get(index % rowCount);

                column.addComponent(checkBox);
                row.addComponent(checkBox);
            }

            layout.setHorizontalGroup(columnGroup);
            layout.setVerticalGroup(rowGroup);
        }
        
        final JPanel buttonBar = new JPanel();
        {
            ButtonBarBuilder2 buttonBarBuilder = new ButtonBarBuilder2(buttonBar);
            buttonBarBuilder.addGlue();
            buttonBarBuilder.addButton(cancelButton);
            buttonBarBuilder.addRelatedGap();
            buttonBarBuilder.addButton(okButton);
        }
        
        contentPane.setLayout(new BorderLayout(0, 8));
        contentPane.add(chekchBoxesPane, "Center");
        contentPane.add(buttonBar, "South");
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        reloadResources();
    }
    
    public JPanel getContentPane() {
        return contentPane;
    }
    
    public JCheckBox getCheckBox(String columnName) {
        JCheckBox cb = checkBoxes.get(columnName);
        if (cb == null)
            throw new IllegalArgumentException(format("'%s' checkbox is not found.", columnName));
        return cb;
    }
    
    public List<JCheckBox> getAllCheckBoxes() {
        List<JCheckBox> list = new ArrayList<JCheckBox>(propertyNames.size());
        for (String propertyName: propertyNames) {
            list.add(getCheckBox(propertyName));
        }
        return list;
    }
    
    public void reloadResources() {
//        Application.getInstance().getContext().getResourceMap(getClass()).injectComponents(contentPane);
        
    }
}
