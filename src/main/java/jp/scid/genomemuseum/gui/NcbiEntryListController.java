package jp.scid.genomemuseum.gui;

import static java.lang.String.*;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import jp.scid.bio.store.remote.RemoteSource;
import jp.scid.bio.store.remote.RemoteSource.RemoteEntry;
import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.genomemuseum.model.NcbiEntry;
import jp.scid.genomemuseum.model.WebServiceResultTableFormat;
import jp.scid.genomemuseum.view.TaskProgressView;
import jp.scid.genomemuseum.view.WebSearchResultListView;
import jp.scid.gui.control.ActionManager;
import jp.scid.gui.control.BooleanModelBindings;
import jp.scid.gui.control.StringModelBindings;
import jp.scid.gui.model.MutableValueModel;
import jp.scid.gui.model.ValueModels;
import jp.scid.gui.model.connector.DocumentTextConnector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

public class NcbiEntryListController extends ListController<NcbiEntry> {
    private final static Logger logger = LoggerFactory.getLogger(NcbiEntryListController.class);
    
    private final static String TASK_MESSAGE_TOTAL = "%d Records";
    private final static String TASK_MESSAGE_PROGRESS = "Searching... %d / ";
    
    private final MutableValueModel<String> searchQuery;
    
    private final MutableValueModel<Boolean> isSearching;
    private final MutableValueModel<Boolean> isIndeterminate;
    
    private final MutableValueModel<String> taskMessage;
    
    private final WebServiceResultTableFormat tableFormat;
    private final DefaultEventTableModel<NcbiEntry> tableModel;
    
    private final DefaultBoundedRangeModel progressModel;
    
    private final ExecutorService taskExecutor;
    private final RemoteSource remoteSource;
    
    private Future<?> runningTask;
    
    private SequenceFileImportable fileImporter = null;
    
    // Actions
    private final Action searchAction;
    private final Action stopAction;
    
    private final HttpClient httpClient;
    
    public NcbiEntryListController() {
        super();
        taskExecutor = Executors.newFixedThreadPool(10);
        
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
        
        // download client
        httpClient = new DefaultHttpClient();
    }
    
    public RemoteSource getSource() {
        return remoteSource;
    }

    public String searchQuery() {
        return searchQuery.get();
    }
    
    private boolean isSearching() {
        return isSearching.get();
    }
    
    int resultCount() {
        return progressModel.getMaximum();
    }
    
    void setResultCount(int newValue) {
        progressModel.setMaximum(newValue);
        updateIndeterminate();
        updateProgressMessage();
    }
    
    public void search() {
        String searchQuery = searchQuery();
        logger.debug("search from NCBI with {}", searchQuery);
        
        SearchTask task = new SearchTask(searchQuery);
        executeSearchTask(task);
    }

    public void stop() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }
    
    public void setFileImporter(SequenceFileImportable fileImporter) {
        this.fileImporter = fileImporter;
    }
    
    private void updateProgressMessage() {
        StringBuilder message = new StringBuilder();
        
        if(isSearching()) {
            message.append(format(TASK_MESSAGE_PROGRESS, sourceCount()));
        }
        
        message.append(format(TASK_MESSAGE_TOTAL, resultCount()));
        taskMessage.set(message.toString());
    }

    private void updateIndeterminate() {
        boolean newValue = resultCount() == this.sourceCount();
        isIndeterminate.set(newValue);
    }
    
    private void executeSearchTask(SwingWorker<?, ?> newTask) {
        stop();
        clear();
        setResultCount(0);
        
        runningTask = newTask;
        isSearching.set(true);
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
        isSearching.set(false);
        
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
            try {
                if (!isCancelled()) {
                    get();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            catch (ExecutionException e) {
                e.printStackTrace();
            }
            finally {
                searchStopped();
            }
        }
    }
    
    private abstract class Command implements Runnable { /* empty */ }
    
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
    
    void execute(RemoteSourceDownloadTask task) {
        taskExecutor.execute(task);
    }
    
    // Downloading
    class TableCellEditorDownloadAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            TaskProgressView editor = (TaskProgressView) SwingUtilities.getAncestorOfClass(TaskProgressView.class, (Component) e.getSource());
            NcbiEntry source = (NcbiEntry) editor.getModel();
            DownloadTaskConnector connector = new DownloadTaskConnector(source);
            
            DonloadFileImportTask task = new DonloadFileImportTask(new DefaultHttpClient(), source.sourceUri(), fileImporter);
            connector.installTo(task);
            
            execute(task);
        }
    }

    private static class DownloadTaskConnector implements PropertyChangeListener {
        private final NcbiEntry source;
        
        public DownloadTaskConnector(NcbiEntry source) {
            super();
            this.source = source;
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if ("contentLength".equals(e.getPropertyName())) {
                source.setTaskSize((Long) e.getNewValue());
            }
            else if ("downloadedLegth".equals(e.getPropertyName())) {
                source.setTaskProgress((Long) e.getNewValue());
            }
            else if ("state".equals(e.getPropertyName())) {
                source.setTaskState((StateValue) e.getNewValue()); 
                
                if (e.getNewValue() == StateValue.DONE) {
                    ((SwingWorker<?, ?>) e.getSource()).removePropertyChangeListener(this);
                }
            }
        }
        
        public void installTo(RemoteSourceDownloadTask task) {
            task.addPropertyChangeListener(this);
        }
    }
    
    static class DonloadFileImportTask extends RemoteSourceDownloadTask {
        private final SequenceFileImportable fileImporter;
        
        public DonloadFileImportTask(HttpClient httpClient, URI sourceUrl, SequenceFileImportable fileImporter) {
            super(httpClient, sourceUrl);
            if (fileImporter == null)
                throw new IllegalArgumentException("fileImporter must not be null");
            this.fileImporter = fileImporter;
        }

        @Override
        protected File doInBackground() throws Exception {
            File file = super.doInBackground();
            
            Future<GeneticSequence> sequenceFuture = fileImporter.executeImportingSequenceFile(file);
            sequenceFuture.get();
            
            return file;
        }
        
        @Override
        protected void done() {
            try {
                get();
            }
            catch (InterruptedException ignore) {
                // ignore
            }
            catch (ExecutionException e) {
                e.printStackTrace();
            }
            // TODO Auto-generated method stub
            super.done();
        }
    }
    
    static class RemoteSourceDownloadTask extends SwingWorker<File, Void> {
        private final HttpClient httpClient;
        private final URI sourceUrl;
        
        private final int bufferSize = 8196;
        private long contentLength = 0;
        private long position = 0;
        
        public RemoteSourceDownloadTask(HttpClient httpClient, URI sourceUrl) {
            if (sourceUrl == null)
                throw new IllegalArgumentException("sourceUrl must not be null");
            this.httpClient = httpClient;
            if (httpClient == null)
                throw new IllegalArgumentException("httpClient must not be null");
            this.sourceUrl = sourceUrl;
        }

        @Override
        protected File doInBackground() throws Exception {
            File destFile = File.createTempFile("GenomeMuseumRemoteDownload", ".txt");
            @SuppressWarnings("resource")
            FileChannel dest = new FileOutputStream(destFile).getChannel();
            
            System.out.println("Download source: " + sourceUrl);
            System.out.println("Download dest: " + destFile);
            try {
                download(sourceUrl, dest);
            }
            finally {
                dest.close();
            }
            
            return destFile;
        }

        private void download(URI sourceUrl, FileChannel dest)
                throws IOException, ClientProtocolException {
            HttpGet request = new HttpGet(sourceUrl);
            HttpResponse response = httpClient.execute(request);
            
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new IOException("cannot find entity");
            }
            
            setContentLength(entity.getContentLength());
            ReadableByteChannel source = Channels.newChannel(entity.getContent());
            try {
                long read;
                while ((read = dest.transferFrom(source, position, bufferSize)) > 0) {
                    appendPosition(read);
                }
                
                source.close();
            }
            finally {
                request.abort();
            }
        }

        public long getContentLength() {
            return contentLength;
        }
        
        private void setContentLength(long contentLength) {
            firePropertyChange("contentLength", this.contentLength, this.contentLength = contentLength);
        }
        
        public long getDownloadedLegth() {
            return this.position;
        }
        
        private void appendPosition(long read) {
            firePropertyChange("downloadedLegth", this.position, this.position += read);
        }
    }

    
    // Binding
    public class Binding extends ListController<NcbiEntry>.Binding {
        public void bindTable(JTable table) {
            table.setModel(tableModel);
            table.setSelectionModel(selectionModel);
        }
        
        public void bindWebSearchResultListView(WebSearchResultListView view) {
            view.setDownloadButtonAction(new TableCellEditorDownloadAction());
        }
        
        public void bindSearchField(JTextField field) {
            field.setAction(searchAction);
            new StringModelBindings(searchQuery).bindToTextField(field);
            DocumentTextConnector connector = new DocumentTextConnector(searchQuery);
            connector.setSource(field.getDocument());
        }
        
        public void bindProgressMessageLabel(JLabel label) {
            new StringModelBindings(taskMessage).bindToLabelText(label);
        }
        
        public void bindProgressIcon(JLabel label) {
            new BooleanModelBindings(isSearching).bindToComponentVisibled(label);
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
    
    public static interface SequenceFileImportable {
        public Future<GeneticSequence> executeImportingSequenceFile(File file);
    }
}
