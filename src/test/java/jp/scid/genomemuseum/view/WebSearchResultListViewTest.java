package jp.scid.genomemuseum.view;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

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
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return TaskProgressModel.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        model.setValueAt(new RemoteSourceImpl(), 0, 0);
        model.setValueAt(new RemoteSourceImpl(), 1, 0);
        model.setValueAt(new RemoteSourceImpl(), 2, 0);
        model.setValueAt(new RemoteSourceImpl(), 3, 0);
        
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
                RemoteSourceImpl source = (RemoteSourceImpl) table.getValueAt(row, column);
//                table.getCellEditor(row, column).stopCellEditing();
                new RemoteSourceDownloadTask((JComponent)((AbstractButton) e.getSource()).getParent(), source, (AbstractTableModel) table.getModel(), row, column).execute();
            }
        };
    }

    class RemoteSourceDownloadTask extends SwingWorker<Void, Void> {
        final RemoteSourceImpl remoteSource;
        final AbstractTableModel model;
        int row;
        int column;
        JComponent table;
        
        public RemoteSourceDownloadTask(JComponent table, RemoteSourceImpl remoteSource, AbstractTableModel model, int row, int column) {
            this.remoteSource = remoteSource;
            this.model = model;
            this.row = row;
            this.column = column;
            this.table = table; 
        }

        @Override
        protected Void doInBackground() throws Exception {
            remoteSource.state = StateValue.STARTED;

            int count = 0;

            while (count < 300) {
                Thread.sleep(10);
                count++;
                remoteSource.progress = count / 3f;
                publish();
            }
            remoteSource.progress = 100;
            Thread.sleep(1000);

            remoteSource.state = StateValue.DONE;

            return null;
        }

        @Override
        protected void process(List<Void> chunks) {
            System.out.println("progress: " + remoteSource.progress);
            model.setValueAt(remoteSource, row, column);
            remoteSource.fireProgressChange();
        }
        @Override
        protected void done() {
            model.setValueAt(remoteSource, row, column);
            remoteSource.fireProgressChange();
        }
    };

}


class RemoteSourceImpl implements TaskProgressModel {
    private final List<ChangeListener> listeners = new LinkedList<ChangeListener>();
    
    boolean available = true;
    float progress = 0;
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
    

    public void addProgressChangeListener(ChangeListener l) {
        listeners.add(l);
    }
    
    public void removeProgressChangeListener(ChangeListener l) {
        listeners.remove(l);
    }
    
    protected void fireProgressChange() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l: listeners) {
            l.stateChanged(e);
        }
    }
}
