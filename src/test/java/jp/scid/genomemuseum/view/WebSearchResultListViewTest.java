package jp.scid.genomemuseum.view;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import jp.scid.genomemuseum.model.SimpleTaskProgressModel;
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
        
        DefaultTableModel model = new DefaultTableModel(4, 2) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return TaskProgressModel.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        model.setValueAt(new SimpleTaskProgressModel(), 0, 0);
        model.setValueAt(new SimpleTaskProgressModel(), 1, 0);
        model.setValueAt(new SimpleTaskProgressModel(), 2, 0);
        model.setValueAt(new SimpleTaskProgressModel(), 3, 0);
        
        view.getTable().setModel(model);
        view.getTable().getTableHeader().getColumnModel().getColumn(0).setPreferredWidth(200);
        
        
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
                int row = table.getEditingRow();
                int column = table.getEditingColumn();
                
                System.out.println("row: " + row + " editor: " + e.getSource());
                SimpleTaskProgressModel source = (SimpleTaskProgressModel) table.getValueAt(row, column);
                new RemoteSourceDownloadTask((JComponent)((AbstractButton) e.getSource()).getParent(), source, (AbstractTableModel) table.getModel(), row, column).execute();
            }
        };
    }

    static class RemoteSourceDownloadTask extends SwingWorker<Void, Void> {
        final SimpleTaskProgressModel remoteSource;
        final AbstractTableModel model;
        int row;
        int column;
        JComponent table;
        int progress = 0;
        
        public RemoteSourceDownloadTask(JComponent table, SimpleTaskProgressModel remoteSource, AbstractTableModel model, int row, int column) {
            this.remoteSource = remoteSource;
            this.model = model;
            this.row = row;
            this.column = column;
            this.table = table; 
            
            remoteSource.setTaskSize(100);
        }

        @Override
        protected Void doInBackground() throws Exception {
            while (progress < 100) {
                publish();
                Thread.sleep(30);
                progress++;
            }
            progress = 100;
            publish();
            
            Thread.sleep(1000);

            return null;
        }

        @Override
        protected void process(List<Void> chunks) {
            remoteSource.setTaskState(getState());
            remoteSource.setTaskProgress(progress);
            
            System.out.println("progress: " + remoteSource.getTaskProgress());
            model.setValueAt(remoteSource, row, column);
        }
        @Override
        protected void done() {
            remoteSource.setTaskProgress(progress);
            remoteSource.setTaskState(getState());
            model.setValueAt(remoteSource, row, column);
        }
    }
}
