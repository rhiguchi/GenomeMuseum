package jp.scid.genomemuseum.model;

import java.util.List;

public class FreeExhibitBoxModel extends AbstractExhibitListModel {
    private final CollectionBox collectionBox;
    
    public FreeExhibitBoxModel(CollectionBox collectionBox) {
        this.collectionBox = collectionBox;
    }
    
    public long getId() {
        // TODO 
        return 0;
    }

    public List<Element> getElementListWith() {
        // TODO
        return null;
    }
    
    public void remove(Element element) {
        // TODO
    }
    
    public Element add(int index, GMExhibit exhibit) {
        // TODO
        return null;
    }
    
    public void updateIndexOfElement(int index, GMExhibit exhibit) {
        // TODO
    }
    
    public static class Element {
        private final GMExhibit exhibit;
        private final int orderIndex;
        
        Element(GMExhibit exhibit, int orderIndex) {
            super();
            this.exhibit = exhibit;
            this.orderIndex = orderIndex;
        }
        
        public GMExhibit exhibit() {
            return exhibit;
        }
        
        public int orderIndex() {
            return orderIndex;
        }
    }
}
