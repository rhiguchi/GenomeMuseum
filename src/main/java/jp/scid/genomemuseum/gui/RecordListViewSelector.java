package jp.scid.genomemuseum.gui;

import jp.scid.bio.store.remote.RemoteSource;
import jp.scid.genomemuseum.view.MainView;
import jp.scid.genomemuseum.view.MainView.ContentsMode;
import jp.scid.gui.control.AbstractValueController;

public class RecordListViewSelector extends AbstractValueController<Object> {
    private final MainView mainView;
    
    public RecordListViewSelector(MainView mainView) {
        this.mainView = mainView;
    }

    private void setContentsMode(ContentsMode newValue) {
        mainView.setContentsMode(newValue);
    }
    
    @Override
    protected void processValueChange(Object newValue) {
        ContentsMode newMode;
        if (newValue instanceof RemoteSource) {
            newMode = ContentsMode.NCBI;
        }
        else {
            newMode = ContentsMode.LOCAL;
        }
        setContentsMode(newMode);
    }
}

