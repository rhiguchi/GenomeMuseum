package jp.scid.gui.control;

import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor;

public class TextMatcherEditorRefilterator<E> extends AbstractValueController<String> {
    private final SearchEngineTextMatcherEditor<E> textMatcherEditor;
    
    public TextMatcherEditorRefilterator(SearchEngineTextMatcherEditor<E> textMatcherEditor) {
        super();
        this.textMatcherEditor = textMatcherEditor;
    }
    
    public TextMatcherEditorRefilterator(TextFilterator<E> filterator) {
        this(new SearchEngineTextMatcherEditor<E>(filterator));
    }
    
    public SearchEngineTextMatcherEditor<E> getTextMatcherEditor() {
        return textMatcherEditor;
    }
    
    @Override
    protected void processValueChange(String newValue) {
        textMatcherEditor.refilter(newValue);
    }
}
