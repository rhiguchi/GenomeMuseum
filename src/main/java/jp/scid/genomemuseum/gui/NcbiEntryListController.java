package jp.scid.genomemuseum.gui;

import static java.lang.String.*;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.Action;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import jp.scid.bio.store.remote.RemoteSource;
import jp.scid.bio.store.remote.RemoteSource.RemoteEntry;
import jp.scid.gui.control.ActionManager;
import jp.scid.gui.control.BooleanModelBindings;
import jp.scid.gui.control.StringModelBindings;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

public class NcbiEntryListController extends ListController<NcbiEntry> {
    private final static String TASK_MESSAGE_TOTAL = "%d Records";
    private final static String TASK_MESSAGE_PROGRESS = "Searching... %d / ";
    
    private final ValueModel<String> searchQuery;
    
    private final ValueModel<Boolean> isSearching;
    private final ValueModel<Boolean> isIndeterminate;
    
    private final ValueModel<String> taskMessage;
    
    private final WebServiceResultTableFormat tableFormat;
    private final DefaultEventTableModel<NcbiEntry> tableModel;
    
    private final DefaultBoundedRangeModel progressModel;
    
    private final RemoteSource remoteSource;
    
    private Future<?> runningTask;
    
    // Actions
    private final Action searchAction;
    private final Action stopAction;
    
    public NcbiEntryListController() {
        super();
        
        remoteSource = new RemoteSource();
        
        searchQuery = ValueModels.newValueModel("");
        isSearching = ValueModels.newBooleanModel(false);
        isIndeterminate = ValueModels.newBooleanModel(true);
        taskMessage = ValueModels.newValueModel("");
        progressModel = new DefaultBoundedRangeModel(0, 0, 0, 0);
        
        tableFormat = new WebServiceResultTableFormat();
        tableModel = new DefaultEventTableModel<NcbiEntry>(getViewList(), tableFormat);
        
        // Actions
        ActionManager actions = new ActionManager(this);
        searchAction = actions.getAction("search");
        stopAction = actions.getAction("stop");
    }
    
    public RemoteSource getSource() {
        return remoteSource;
    }

    public String searchQuery() {
        return searchQuery.getValue();
    }
    
    private boolean isSearching() {
        return isSearching.getValue();
    }
    
    int resultCount() {
        return progressModel.getMaximum();
    }
    
    void setResultCount(int newValue) {
        progressModel.setMaximum(newValue);
        updateIndeterminate();
    }
    
    public void search() {
        SearchTask task = new SearchTask(searchQuery());
        execute(task);
    }

    public void stop() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }
    
    private void updateProgressMessage() {
        StringBuilder message = new StringBuilder();
        
        if(isSearching()) {
            message.append(format(TASK_MESSAGE_PROGRESS, sourceCount()));
        }
        
        message.append(format(TASK_MESSAGE_TOTAL, resultCount()));
        taskMessage.setValue(message.toString());
    }

    private void updateIndeterminate() {
        boolean newValue = resultCount() == this.sourceCount();
        isIndeterminate.setValue(newValue);
    }
    
    private void execute(SwingWorker<?, ?> newTask) {
        stop();
        clear();
        
        runningTask = newTask;
        isSearching.setValue(true);
        updateProgressMessage();

        newTask.execute();
    }

    public void addResults(List<RemoteEntry> entries) {
        for (RemoteEntry e: entries) {
            NcbiEntry ncbiEntry = NcbiEntry.fromRemoteSource(e);
            add(ncbiEntry);
        }
        
        updateProgressMessage();
        updateIndeterminate();
    }
    
    private void searchStopped() {
        runningTask = null;
        isSearching.setValue(false);
        
        updateProgressMessage();
    }
    
    class SearchTask extends SwingWorker<Void, Command> {
        private final int chunkSize = 10;
        private final String queryString;
        
        public SearchTask(String queryString) {
            this.queryString = queryString;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (queryString.isEmpty()) {
                return null;
            }
            
            int count = remoteSource.retrieveCount(queryString);
            publish(new UpdateCount(count));
            
            for (int offset = 1; offset <= count; offset += chunkSize) {
                List<RemoteEntry> list = remoteSource.searchEntry(queryString, offset, chunkSize);
                
                publish(new AppendResult(list));
            }
            return null;
        }
        
        @Override
        protected void process(List<Command> chunks) {
            if (isCancelled()) {
                return;
            }
            
            for (Command command: chunks) {
                command.run();
            }
        }
        
        @Override
        protected void done() {
            searchStopped();
        }
    }
    
    private abstract class Command implements Runnable {}
    
    private final class AppendResult extends Command {
        List<RemoteEntry> entries;

        public AppendResult(List<RemoteEntry> entries) {
            this.entries = entries;
        }
        
        @Override
        public void run() {
            addResults(entries);
        }
    }
    
    private final class UpdateCount extends Command {
        private final int countValue;

        public UpdateCount(int newValue) {
            this.countValue = newValue;
        }
        
        @Override
        public void run() {
            setResultCount(countValue);
        }
    }
    
    // Binding
    public class Binding extends ListController<NcbiEntry>.Binding {
        public void bindTable(JTable table) {
            table.setModel(tableModel);
            table.setSelectionModel(selectionModel);
        }
        
        public void bindSearchField(JTextField field) {
            field.setAction(searchAction);
            new StringModelBindings(searchQuery).bindToTextField(field);
        }
        
        public void bindProgressMessageLabel(JLabel label) {
            new StringModelBindings(taskMessage).bindToLabelText(label);
        }
        
        public void bindStopButton(JButton button) {
            new BooleanModelBindings(isSearching).bindToComponentVisibled(button);
            button.setAction(stopAction);
        }
        
        public void bindProgressBar(JProgressBar progressBar) {
            progressBar.setModel(progressModel);
            new BooleanModelBindings(isSearching).bindToComponentVisibled(progressBar);
            new BooleanModelBindings(isIndeterminate).bindToProgressBarIndeterminate(progressBar);
        }
    }
}

class NcbiEntry {
    private final RemoteEntry entry;
    
    private NcbiEntry(RemoteEntry entry) {
        this.entry = entry;
    }
    
    public static NcbiEntry fromRemoteSource(RemoteEntry e) {
        return new NcbiEntry(e);
    }

    public String identifier() {
        return entry.identifier();
    }

    public String accession() {
        return entry.accession();
    }

    public int sequenceLength() {
        return entry.sequenceLength();
    }

    public String definition() {
        return entry.definition();
    }

    public String taxonomy() {
        return entry.taxonomy();
    }
    
    @Override
    public String toString() {
        return identifier();
    }
}


class WebServiceResultTableFormat implements AdvancedTableFormat<NcbiEntry> {
    enum Column implements Comparator<NcbiEntry> {
        IDENTIFIER(String.class) {
            Object getColumnValue(NcbiEntry e) {
                return e.identifier();
            }
        },
        ACCESSION(String.class) {
            Object getColumnValue(NcbiEntry e) {
                return e.accession();
            }
        },
        SEQUENCE_LENGTH(String.class) {
            Object getColumnValue(NcbiEntry e) {
                return e.sequenceLength();
            }
        },
        DEFINITION(String.class) {
            Object getColumnValue(NcbiEntry e) {
                return e.definition();
            }
        },
        TAXONOMY(String.class) {
            Object getColumnValue(NcbiEntry e) {
                return e.taxonomy();
            }
        },
        ;
        private final Class<?> dataClass;
        
        private Column(Class<?> dataClass) {
            this.dataClass = dataClass;
        }
        
        public Comparator<NcbiEntry> comparator() {
            return Comparable.class.isAssignableFrom(dataClass) ? this : null;
        }
        
        abstract Object getColumnValue(NcbiEntry e);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public int compare(NcbiEntry o1, NcbiEntry o2) {
            Comparable val1 = (Comparable<?>) getColumnValue(o1);
            Comparable val2 = (Comparable<?>) getColumnValue(o2);
            return val1.compareTo(val2);
        }
    }
    
    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return Column.values()[column].name();
    }

    @Override
    public Object getColumnValue(NcbiEntry baseObject, int column) {
        return Column.values()[column].getColumnValue(baseObject);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return Column.values()[column].dataClass;
    }

    @Override
    public Comparator<NcbiEntry> getColumnComparator(int columnNumber) {
        return Column.values()[columnNumber].comparator();
    }
}
