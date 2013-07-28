package jp.scid.genomemuseum.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.PlainDocument;

import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.genomemuseum.view.FileContentView;
import jp.scid.gui.model.ValueModel;
import jp.scid.motifviewer.gui.MotifViewerController;
import jp.scid.motifviewer.gui.MotifViewerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuseumExhibitContentViewer {
    private final static Logger logger = LoggerFactory.getLogger(MuseumExhibitContentViewer.class);
    
    private PlainDocument document = new PlainDocument();
    
    private GeneticSequence sequence = null;
    
    private ValueModel<GeneticSequence> model;
    
    private final ChangeListener modelListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            modelValueChange(model);
        }
    };
    
    // Controller
    private final MotifViewerController motifViewerController;
    
    public MuseumExhibitContentViewer() {
        motifViewerController = new MotifViewerController();
    }

    public void setModel(ValueModel<GeneticSequence> newModel) {
        if (this.model != null) {
            this.model.removeValueChangeListener(modelListener);
        }
        
        this.model = newModel;
        
        if (newModel != null) {
            newModel.addValueChangeListener(modelListener);
            modelValueChange(newModel);
        }
    }
    
    protected void modelValueChange(ValueModel<GeneticSequence> model) {
        setGeneticSequence(model.get());
    }
    
    public void setGeneticSequence(GeneticSequence sequence) {
        this.sequence = sequence;
        
        if (sequence != null) {
            setSequence(sequence.sequence());
        }
        
        reload();
    }

    // sequence
    public String getSequence() {
        return motifViewerController.getSequence();
    }
    
    private void setSequence(String sequence) {
        motifViewerController.setSequence(sequence);
    }
    
    public void clearContent() {
        try {
            document.remove(0, document.getLength());
        }
        catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }
    
    // contentText
    Reader getContentReader() throws IOException {
        File file = sequence.getFile();
        return new FileReader(file);
    }
    
    void appendDocumentString(String text) throws BadLocationException {
        int offset = document.getLength();
        document.insertString(offset, text, null);
    }
    
    // Actions
    public void reload() {
        clearContent();
        
        ContentLoader task = new ContentLoader();
        task.execute();
    }
    
    // Bindings
    public void bindFileContentView(FileContentView view) {
        bindFileContentTextArea(view.textArea);
    }

    public void bindMotifViewerView(MotifViewerView view) {
        motifViewerController.bind(view);
    }

    void bindFileContentTextArea(JTextArea textArea) {
        textArea.setDocument(document);
        ((DefaultCaret) textArea.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }

    class ContentLoader extends SwingWorker<Void, String> {
        private final int bufferSize = 8196;

        @Override
        protected Void doInBackground() throws Exception {
            // load String
            BufferedReader contentReader = new BufferedReader(getContentReader(), bufferSize);
            try {
                char[] cbuf = new char[bufferSize];
                int read;
                
                while (!isCancelled() && (read = contentReader.read(cbuf)) >= 0) {
                    String text = new String(cbuf, 0, read);
                    publish(text);
                }
            }
            finally {
                contentReader.close();
            }
            
            return null;
        }
        
        @Override
        protected void process(List<String> chunks) {
            StringBuilder sb = new StringBuilder(bufferSize * chunks.size());
            
            for (String string: chunks) {
                sb.append(string);
            }
            
            try {
                appendDocumentString(sb.toString());
            }
            catch (BadLocationException e) {
                cancel(false);
                logger.error("Fail to load file", e);
                JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), null, JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}