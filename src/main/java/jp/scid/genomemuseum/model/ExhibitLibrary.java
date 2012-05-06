package jp.scid.genomemuseum.model;


public interface ExhibitLibrary extends ExhibitCollectionModel {
    boolean deleteExhibit(MuseumExhibit exhibit, boolean withFile);
}