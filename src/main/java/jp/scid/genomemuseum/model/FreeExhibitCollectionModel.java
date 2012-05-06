package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.util.List;

import jp.scid.genomemuseum.model.sql.tables.records.CollectionBoxItemRecord;

import org.jooq.impl.Factory;

public interface FreeExhibitCollectionModel extends ExhibitCollectionModel {
    void addExhibit(int index, MuseumExhibit e);
}
