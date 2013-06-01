package jp.scid.genomemuseum.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import jp.scid.bio.store.sequence.GeneticSequence;
import jp.scid.genomemuseum.view.FileContentView;

import org.jdesktop.application.AbstractBean;
//import jp.scid.motifviewer.gui.MotifViewerController;
//import jp.scid.motifviewer.gui.MotifViewerView;

public class MuseumExhibitContentViewer extends AbstractBean {
    private Document document = new PlainDocument();
    
    private GeneticSequence sequence = null;
    
    // Controller
//    final MotifViewerController motifViewerController;
    
//    MuseumExhibitLibrary library = null;
    
    public MuseumExhibitContentViewer() {
//        motifViewerController = new MotifViewerController();
    }

    public void setExhibit(GeneticSequence sequence) {
        this.sequence = sequence;
    }

//    public MuseumExhibitLibrary getLibrary() {
//        return library;
//    }
//
//    public void setLibrary(MuseumExhibitLibrary library) {
//        this.library = library;
//    }
    
    // sequence
    public String getSequence() {
        return ""; //motifViewerController.getSequence();
    }
    
    public void setSequence(String sequence) {
        //motifViewerController.setSequence(sequence);
    }
    
    public void clearContent() {
        try {
            document.remove(0, document.getLength());
        }
        catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

//    public void bindOverviewMotifView(MotifViewerView view) {
    public void bindOverviewMotifView(Object view) {
//        motifViewerController.bind(view);
    }

    void bindFileContentTextArea(JTextArea textArea) {
        textArea.setDocument(document);
    }

    class ContentLoader extends SwingWorker<Void, String> {
        private final int bufferSize = 8196;
        
        @Override
        protected Void doInBackground() throws Exception {
            StringBuilder content = new StringBuilder();
            // load String
            BufferedReader contentReader = new BufferedReader(getContentReader(), bufferSize);
            try {
                char[] cbuf = new char[bufferSize];
                int read;
                
                while ((read = contentReader.read(cbuf)) >= 0) {
                    String text = new String(cbuf, 0, read);
                    publish(text);
                    content.append(text);
                }
            }
            finally {
                contentReader.close();
            }
            
            // get bio data
            
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}