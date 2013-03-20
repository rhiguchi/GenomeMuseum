package jp.scid.gui.model;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jp.scid.gui.model.TransformValueModel.Transformer;

class Transformers {
    private BooleanNegator booleanNegator;
    
    public BooleanNegator getBooleanNegator() {
        if (booleanNegator == null)
            booleanNegator = new BooleanNegator();
        
        return booleanNegator;
    }
    
    static class BooleanNegator implements Transformer<Boolean, Boolean> {
        @Override
        public Boolean apply(Boolean subject) {
            if (subject == null)
                return Boolean.FALSE;
            return subject.booleanValue() ? Boolean.FALSE : Boolean.TRUE;
        }
    }
    
    static class CollectionSelector implements Transformer<Object, Boolean> {
        private Set<?> selectableElements = Collections.emptySet();
        
        public CollectionSelector(Object selectableElement) {
            if (selectableElement == null)
                throw new IllegalArgumentException("selectableElement must not be null");
            
            selectableElements = Collections.singleton(selectableElement);
        }
        
        public CollectionSelector(Set<?> selectableElements) {
            if (selectableElements == null)
                throw new IllegalArgumentException("selectableElements must not be null");
                
            this.selectableElements = new HashSet<Object>(selectableElements);
        }
        
        @Override
        public Boolean apply(Object subject) {
            return selectableElements.contains(subject);
        }
    }
    
    static class BooleanElementValue<E> implements Transformer<Boolean, E> {
        private E valueForTrue;
        private E valueForFalse;
        
        public BooleanElementValue(E valueForTrue, E valueForFalse) {
            this.valueForTrue = valueForTrue;
            this.valueForFalse = valueForFalse;
        }
        
        public BooleanElementValue(E valueForTrue) {
            this(valueForTrue, null);
        }
        
        @Override
        public E apply(Boolean subject) {
            return subject != null && subject.booleanValue() ? valueForTrue : valueForFalse;
        }
    }
    
    static class StringFormatter implements Transformer<Object, String> {
        private final String format;
        
        public StringFormatter(String format) {
            this.format = format;
        }
        
        @Override
        public String apply(Object subject) {
            return String.format(format, subject);
        }
    }
}