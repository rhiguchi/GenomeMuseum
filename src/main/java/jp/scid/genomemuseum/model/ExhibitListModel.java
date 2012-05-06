package jp.scid.genomemuseum.model;

import java.util.List;

import javax.swing.event.ChangeListener;

public interface ExhibitListModel {
    List<MuseumExhibit> fetchExhibits();
    
    boolean storeExhibit(MuseumExhibit exhibit);
    
    void addExhibitsChangeListener(ChangeListener listener);
    
    void removeExhibitsChangeListener(ChangeListener listener);
}

abstract class AbstractExhibitListModel implements ExhibitListModel {
    @Override
    public void addExhibitsChangeListener(ChangeListener listener) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void removeExhibitsChangeListener(ChangeListener listener) {
        // TODO Auto-generated method stub
        
    }
}
