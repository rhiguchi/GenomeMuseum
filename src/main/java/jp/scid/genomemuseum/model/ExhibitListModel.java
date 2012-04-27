package jp.scid.genomemuseum.model;

import java.util.Collection;
import java.util.Collections;

import org.jooq.Condition;

public interface ExhibitListModel {
    Collection<Condition> getFetchCondition();
}

abstract class AbstractExhibitListModel implements ExhibitListModel {
    public Collection<Condition> getFetchCondition() {
        return Collections.emptyList();
    }
}