package jp.scid.gui.control;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

public class UriDocumentLoader extends AbstractValueController<URI> {
    private ContentLoader currentLoader = null;
    
    private final PlainDocument document = new PlainDocument();
    
    public UriDocumentLoader() {
    }

    PlainDocument getDocument() {
        return document;
    }
    
    public void bindTextComponent(JTextComponent view) {
        view.setDocument(getDocument());
    }
    
    public void setUri(URI newValue) {
        if (newValue == null) {
            clearDocumentContent();
        }
        else {
            ContentLoader task = createContentLoader(newValue);
            stopCurrentTask();
            execute(task);
        }
    }
    
    @Override
    protected void processValueChange(URI newValue) {
        setUri(newValue);
    }
    
    void clearDocumentContent() {
        try {
            document.remove(0, document.getLength());
        }
        catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    ContentLoader createContentLoader(URI source) {
        return new ContentLoader(source, getDocument());
    }
    
    synchronized void stopCurrentTask() {
        if (currentLoader != null && !currentLoader.isDone())
            currentLoader.cancel(true);
    }
    
    synchronized void execute(ContentLoader task) {
        currentLoader = task;
        task.execute();
    }
    
    static class ContentLoader extends SwingWorker<Void, String> {
        private final URI source;
        private final PlainDocument document;
        
        public ContentLoader(URI source, PlainDocument document) {
            this.source = source;
            this.document = document;
        }

        @Override
        protected Void doInBackground() throws Exception {
            InputStreamReader reader = new InputStreamReader(source.toURL().openStream());
            char[] cbuf = new char[8196];
            int read;
            
            try {
                while ((read = reader.read(cbuf)) != -1) {
                    if (isCancelled())
                        break;
                    String string = new String(cbuf, 0, read);
                    publish(string);
                }
            }
            finally {
                reader.close();
            }
            
            return null;
        }
        
        @Override
        protected void process(List<String> chunks) {
            for (String string: chunks) {
                try {
                    document.insertString(document.getLength(), string, null);
                }
                catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
