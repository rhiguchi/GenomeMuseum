package jp.scid.genomemuseum.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jooq.RecordHandler;
import org.jooq.SimpleSelectQuery;
import org.jooq.UpdatableRecord;
import org.jooq.UpdatableTable;
import org.jooq.impl.Factory;

abstract class JooqEntityService<E, R extends UpdatableRecord<R>> implements EntityService<E> {
    protected final Factory factory;
    
    protected final UpdatableTable<R> table;
    
    private String whereSql = "id = ?";
    
    protected JooqEntityService(Factory factory, UpdatableTable<R> table) {
        this.factory = factory;
        this.table = table;
    }

    public int getCount() {
        return factory.selectCount().from(table).fetchOne(0, Integer.class);
    }
    
    @Override
    public E find(long id) {
        R record = factory.selectFrom(table)
                .where(whereSql, id).fetchOne();
        
        return record == null ? null : createElement(record);
    }
    
    @Override
    public List<E> search(String sql, Object... params) {
        SimpleSelectQuery<R> query = factory.selectQuery(table);
        if (sql != null) {
            query.addConditions(Factory.condition(sql, params));
        }
        List<E> list = query.fetchInto(new ElementBuilder()).build();
        
        return list;
    }
    
    abstract E createElement(R record);
    
    abstract R recordOfElement(E element);
    
    @Override
    public E newElement() {
        R record = newRecord();
        return createElement(record);
    }

    R newRecord() {
        return factory.newRecord(table);
    }

    public boolean store(E box) {
        boolean result = recordOfElement(box).store() > 0;
        
        return result;
    }

    public boolean delete(E element) {
        int count = recordOfElement(element).delete();
        
        return count > 0;
    }
    
    class ElementBuilder implements RecordHandler<R> {
        List<E> list = new LinkedList<E>();
        
        @Override
        public void next(R record) {
            E element = createElement(record);
            list.add(element);
        }
        
        public List<E> build() {
            return new ArrayList<E>(list);
        }
    }
}