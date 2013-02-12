package jp.scid.genomemuseum.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import jp.scid.bio.Feature;
import jp.scid.bio.GenBank;
import jp.scid.bio.SequenceBioData;
import jp.scid.bio.SequenceBioDataFormat;
import jp.scid.bio.SequenceBioDataReader;
import jp.scid.genomemuseum.model.MuseumExhibit;
import jp.scid.genomemuseum.model.MuseumExhibitLibrary;
import jp.scid.genomemuseum.view.FileContentView;
//import jp.scid.motifviewer.gui.MotifViewerController;
//import jp.scid.motifviewer.gui.MotifViewerView;

import org.jdesktop.application.AbstractBean;

public class MuseumExhibitContentViewer extends AbstractBean {
    private Document document = new PlainDocument();
    
    private MuseumExhibit exhibit = null;
    
    // Controller
//    final MotifViewerController motifViewerController;
    
    MuseumExhibitLibrary library = null;
    
    public MuseumExhibitContentViewer() {
//        motifViewerController = new MotifViewerController();
    }

    public void setExhibit(MuseumExhibit exhibit) {
        this.exhibit = exhibit;
    }

    public MuseumExhibitLibrary getLibrary() {
        return library;
    }

    public void setLibrary(MuseumExhibitLibrary library) {
        this.library = library;
    }
    
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
        URL url = exhibit.getSourceFileAsUrl();
        InputStreamReader reader = new InputStreamReader(url.openStream());
        return reader;
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
            Reader source = new StringReader(content.toString());
            
            SequenceBioDataFormat<?> format = library.getFormat(exhibit.getFileType());
            
            if (format != null) {
                loadBioData(source, format);
            }
            
            return null;
        }

        void loadBioData(Reader source, SequenceBioDataFormat<?> format) throws IOException {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            SequenceBioDataReader bioDataReader =
            new SequenceBioDataReader(source, format);

            // TODO multisection
            if (bioDataReader.hasNext()) {
                SequenceBioData bioData = bioDataReader.next();

                setSequence(bioData.getSequence());

                if (bioData instanceof GenBank) {
                    List<Feature> features = ((GenBank) bioData).getFeatures();
                }

                // TODO set to controller
            }

            bioDataReader.close();
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