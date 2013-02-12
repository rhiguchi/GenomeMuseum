package jp.scid.genomemuseum.gui;

import java.util.List;

import org.jooq.Result;
import org.jooq.SelectQuery;
import org.jooq.UpdatableRecord;
import org.jooq.UpdatableTable;
import org.jooq.impl.Factory;

@Deprecated
public class JooqRecordController<E extends UpdatableRecord<E>> extends ListController<E> {
    
    // persistence
    protected Factory factory = null;
    
    protected final UpdatableTable<E> table;
    
    public JooqRecordController(UpdatableTable<E> table) {
        this.table = table;
    }
    
    public JooqRecordController(Factory factory, UpdatableTable<E> table) {
        this(table);
        this.factory = factory;
    }
    
    // persistence methods
    public Factory getFactory() {
        return factory;
    }
    
    public void setFactory(Factory factory) {
        this.factory = factory;
    }
    
    public void fetch() {
        SelectQuery query = factory.selectQuery();
        makeQuery(query);
        Result<E> result = query.fetchInto(table);
        setSource(result);
    }
    
    @Override
    protected E createElement() {
        if (factory == null) {
            throw new IllegalStateException("need factory");
        }
        
        E record = factory.newRecord(table);
        return record;
    }
    
    protected void makeQuery(SelectQuery query) {
        
    }
    
    public static interface SelectQueriable {
        void makeQuery(SelectQuery query);
    }
}