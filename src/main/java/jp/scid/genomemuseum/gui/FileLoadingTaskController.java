package jp.scid.genomemuseum.gui;

import static java.lang.String.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.bio.store.sequence.ImportableSequenceSource;
import jp.scid.genomemuseum.gui.NcbiEntryListController.SequenceFileImportable;
import jp.scid.genomemuseum.model.GeneticSequenceFileLoadingManager;
import jp.scid.gui.control.BooleanModelBindings;
import jp.scid.gui.control.StringModelBindings;
import jp.scid.gui.model.BeanPropertyAdapter;
import jp.scid.gui.model.MutableValueModel;
import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class FileLoadingTaskController implements PropertyChangeListener, SequenceFileImportable {
    private GeneticSequenceFileLoadingManager loadingManager;
    private final ExecutorService taskExecutor;
    private final DefaultBoundedRangeModel progressModel = new DefaultBoundedRangeModel();
    
    private final MutableValueModel<Boolean> isIndeterminate;
    private final MutableValueModel<String> progressMessage;
    
    private SequenceLibrary sequenceLibrary;
    
    public FileLoadingTaskController() {
        taskExecutor = Executors.newFixedThreadPool(1);
        
        setLoadingManager(new GeneticSequenceFileLoadingManager(taskExecutor));
        
        isIndeterminate = ValueModels.newBooleanModel(false);
        progressMessage = ValueModels.newValueModel("");
    }

    public void shutdownNow() {
        taskExecutor.shutdownNow();
    }
    
    @Override
    public Future<GeneticSequence> executeImportingSequenceFile(File file) {
        Callable<GeneticSequence> importTask = sequenceLibrary.createSequenceImportTask(file);
        return loadingManager.execute(importTask);
    }
    
    public void setSequenceLibrary(SequenceLibrary sequenceLibrary) {
        this.sequenceLibrary = sequenceLibrary;
    }
    
    public void executeLoading(Collection<File> files, ImportableSequenceSource dest) {
        loadingManager.executeLoading(files, dest);
    }
    
    public void setLoadingManager(GeneticSequenceFileLoadingManager loadingManager) {
        if (this.loadingManager != null) {
            this.loadingManager.removePropertyChangeListener(this);
        }
        
        this.loadingManager = loadingManager;
        
        if (loadingManager != null) {
            loadingManager.addPropertyChangeListener(this);
        }
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("executed".equals(evt.getPropertyName())) {
            progressModel.setMaximum((Integer) evt.getNewValue());
            updateIndeterminate();
            updateProgressMessage();
        }
        else if ("success".equals(evt.getPropertyName())) {
            progressModel.setValue((Integer) evt.getNewValue());
            updateIndeterminate();
            updateProgressMessage();
        }
    }
    
    private void updateIndeterminate() {
        boolean newValue = progressModel.getMaximum() <= progressModel.getValue();
        isIndeterminate.set(newValue);
    }
    
    private void updateProgressMessage() {
        String message = format("%d / %d...", progressModel.getValue(), progressModel.getMaximum());
        progressMessage.set(message);
    }

    
    public class Bindings {
        private final ValueModel<Boolean> conentePaneVisible;
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public Bindings() {
            conentePaneVisible = (ValueModel) new BeanPropertyAdapter(loadingManager, "inProgress", true, false);
        }
        
        public void bindProgressBar(JProgressBar progressBar) {
            progressBar.setModel(progressModel);
            new BooleanModelBindings(isIndeterminate).bindToProgressBarIndeterminate(progressBar);
        }
        
        public void bindStatusLabel(JLabel label) {
            new StringModelBindings(progressMessage).bindToLabelText(label);
        }
        
        public void bindContentPane(JComponent pane) {
            new BooleanModelBindings(conentePaneVisible).bindToComponentVisibled(pane);
        }
    }
}
