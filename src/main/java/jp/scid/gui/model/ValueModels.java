package jp.scid.gui.model;

import java.util.Collections;
import java.util.List;

import jp.scid.gui.model.Transformers.BooleanElementValue;
import jp.scid.gui.model.Transformers.CollectionSelector;
import jp.scid.gui.model.Transformers.StringFormatter;

public class ValueModels {
    private final static Transformers t = new Transformers();
    
    private ValueModels() {
    }
    
    public static <T> ValueModel<T> newNullableValueModel() {
        return new SimpleValueModel<T>();
    }
    
    public static <T> NonNullValueModel<T> newValueModel(T initialValue) {
        return new NonNullValueModel<T>(initialValue);
    }
    
    public static <T> NonNullValueModel<List<T>> newListModel() {
        return new NonNullValueModel<List<T>>(Collections.<T>emptyList());
    }
    
    public static NonNullValueModel<Boolean> newBooleanModel(boolean initialValue) {
        return new NonNullValueModel<Boolean>(initialValue);
    }
    
    public static NonNullValueModel<Integer> newIntegerModel(int initialValue) {
        return new NonNullValueModel<Integer>(initialValue);
    }
    
    public static <T> ValueModel<Boolean> newSelectionBooleanModel(ValueModel<T> adaptee, T selectionValue) {
        CollectionSelector selector = new CollectionSelector(selectionValue);
        BooleanElementValue<T> trueTransformer = new BooleanElementValue<T>(selectionValue);
        TransformValueModel<T, Boolean> valueModel = new TransformValueModel<T, Boolean>(selector, trueTransformer);
        valueModel.setSubject(adaptee);
        return valueModel;
    }
    
    public static ValueModel<Boolean> newNegationBooleanModel(ValueModel<Boolean> adaptee) {
        TransformValueModel<Boolean, Boolean> model = new TransformValueModel<Boolean, Boolean>(t.getBooleanNegator()); 
        model.setSubject(adaptee);
        return model; 
    }
    
    public static <T> ValueModel<String> newFormatStringModel(ValueModel<T> adaptee, String format) {
        StringFormatter transformer = new StringFormatter(format);
        TransformValueModel<T, String> model = new TransformValueModel<T, String>(transformer); 
        model.setSubject(adaptee);
        
        return model; 
    }
}
