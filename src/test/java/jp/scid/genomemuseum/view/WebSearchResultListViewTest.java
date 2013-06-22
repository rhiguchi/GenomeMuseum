package jp.scid.genomemuseum.view;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import jp.scid.genomemuseum.model.TaskProgressModel;

public class WebSearchResultListViewTest {

    // view test
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new WebSearchResultListViewTest().runSample();
            }
        });
    }
    
    private void runSample() {
        WebSearchResultListView view = new WebSearchResultListView();
        Action action = createSampleDownloadingAction2();
        view.setDownloadButtonAction(action);
        
        DefaultTableModel model = new DefaultTableModel(10, 4) {
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return TaskProgressModel.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        model.setValueAt(new RemoteSourceImpl(), 0, 0);
        
        view.getTable().setModel(model);
        
        
        JFrame frame = new JFrame("test");
        frame.setContentPane(view.getTableContainer());
        frame.pack();
        frame.setVisible(true);
    }
    

    private Action createSampleDownloadingAction2() {
        return new AbstractAction("Download") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTable table = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, (AbstractButton) e.getSource());
                int editingRow = table.getEditingRow();
                
                System.out.println("row: " + editingRow + " editor: " + e.getSource());
                RemoteSourceImpl source = new RemoteSourceImpl();
                table.setValueAt(source, table.getEditingRow(), table.getEditingColumn());
                
                new RemoteSourceDownloadTask(source, (AbstractTableModel) table.getModel()).execute();
            }
        };
    }

    class RemoteSourceDownloadTask extends SwingWorker<Void, Void> {
        final RemoteSourceImpl remoteSource;
        final AbstractTableModel model;

        public RemoteSourceDownloadTask(RemoteSourceImpl remoteSource, AbstractTableModel model) {
            this.remoteSource = remoteSource;
            this.model = model;
        }

        @Override
        protected Void doInBackground() throws Exception {
            remoteSource.state = StateValue.STARTED;

            int count = 0;

            while (count < 500) {
                Thread.sleep(10);
                count++;
                remoteSource.progress = count / 5.0f;
                System.out.println("progress: " + remoteSource.progress);
                publish();
            }
            remoteSource.progress = 100;
            Thread.sleep(1000);

            remoteSource.state = StateValue.DONE;

            return null;
        }

        @Override
        protected void process(List<Void> chunks) {
            model.fireTableDataChanged();
        }
    };

}


class RemoteSourceImpl implements TaskProgressModel {
    boolean available = true;
    float progress = 1;
    String message = "";
    StateValue state = StateValue.PENDING;
    
    @Override
    public StateValue getState() {
        return state;
    }
    
    public float getProgress() {
        return progress;
    }
    
    public String getLabel() {
        return message;
    }
    
    public boolean isAvailable() {
        return available;
    }
}
