package jp.scid.gui.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

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
    
    public static ValueModel<Object> newTreeSelectedNodeObject(TreeSelectionModel model) {
        TreeSelectedNodeConnector connector = new TreeSelectedNodeConnector();
        connector.setSource(model);
        return connector.getValueModel();
    }

    public static ValueModel<Boolean> newInstanceCheckModel(ValueModel<? extends Object> base, Class<?> testClass) {
        InstanceMatchConnector conn = new InstanceMatchConnector(testClass);
        conn.setSource(base);
        return conn.getValueModel();
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
    
    public static ValueModel<Boolean> newListElementsExistenceModel(ListModel source) {
        ListCountModelAdapter adapter = new ListCountModelAdapter(newIntegerModel(0));
        adapter.setSource(source);
        
        NumberThresholdModel<Integer> model = new NumberThresholdModel<Integer>(0);
        model.setSource(adapter.getValueModel());
        
        return model.getValueModel();
    }
    
    static class ListCountModelAdapter extends ValueModelConnector<Integer, ListModel> implements ListDataListener {
        public ListCountModelAdapter(ValueModel<Integer> target) {
            super(target);
        }
        
        @Override
        protected Integer getModelValue(ListModel source) {
            return source.getSize();
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            updateModelValue();
        }
        
        @Override
        public void intervalRemoved(ListDataEvent e) {
            updateModelValue();
        }
        
        @Override
        public void contentsChanged(ListDataEvent e) {
            updateModelValue();
        }
        
        @Override
        protected void installSourceChangeListener(ListModel source) {
            source.addListDataListener(this);
        }
        
        @Override
        protected void uninstallSourceChangeListener(ListModel source) {
            source.removeListDataListener(this);
        }
    }
}

class NumberThresholdModel<T extends Number & Comparable<T>> extends ValueModelValueConnector<Boolean, T> {
    private final T thresholdValue;
    
    public NumberThresholdModel(T thresholdValue) {
        super(ValueModels.newBooleanModel(false));
        
        if (thresholdValue == null) throw new IllegalArgumentException("thresholdValue must not be null");
        this.thresholdValue = thresholdValue;
    }
    
    @Override
    protected Boolean convertModelValue(T sourceValue) {
        return thresholdValue.compareTo(sourceValue) < 0;
    }
}

class InstanceMatchConnector extends ValueModelValueConnector<Boolean, Object> {
    private Class<?> testClass;
    
    public InstanceMatchConnector(Class<?> testClass) {
        super(ValueModels.newBooleanModel(false));
        
        if (testClass == null) throw new IllegalArgumentException("testClass must not be null");
        this.testClass = testClass;
    }
    
    @Override
    protected Boolean convertModelValue(Object sourceValue) {
        return testClass.isInstance(sourceValue);
    }
}

class TreeSelectedNodeConnector extends ValueModelConnector<Object, TreeSelectionModel> implements TreeSelectionListener {
    public TreeSelectedNodeConnector() {
        super(ValueModels.newNullableValueModel());
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        updateModelValue();
    }

    @Override
    protected Object getModelValue(TreeSelectionModel source) {
        TreePath path = source.getLeadSelectionPath();
        if (path == null) {
            return null;
        }
        if (path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            return ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        }
        return null;
    }

    @Override
    protected void installSourceChangeListener(TreeSelectionModel source) {
        source.addTreeSelectionListener(this);
    }

    @Override
    protected void uninstallSourceChangeListener(TreeSelectionModel source) {
        source.removeTreeSelectionListener(this);
    }
}


abstract class ValueModelValueConnector<T, S> extends ValueModelConnector<T, ValueModel<? extends S>> implements PropertyChangeListener {

    protected ValueModelValueConnector(ValueModel<T> target) {
        super(target);
    }

    abstract protected T convertModelValue(S sourceValue);
    
    @Override
    final protected T getModelValue(ValueModel<? extends S> source) {
        return convertModelValue(source.getValue());
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        updateModelValue();
    }
    
    @Override
    protected void installSourceChangeListener(ValueModel<? extends S> source) {
        source.addPropertyChangeListener(this);
    }

    @Override
    protected void uninstallSourceChangeListener(ValueModel<? extends S> source) {
        source.removePropertyChangeListener(this);
    }
}

abstract class ValueModelConnector<T, S> {
    private final ValueModel<T> target;
    private S source;

    protected ValueModelConnector(ValueModel<T> target) {
        if (target == null) throw new IllegalArgumentException("target must not be null");
        this.target = target;
    }
    
    public ValueModelConnector(T initialValue) {
        this(ValueModels.newValueModel(initialValue));
    }

    public ValueModel<T> getValueModel() {
        return target;
    }

    protected void setValue(T newValue) {
        target.setValue(newValue);
    }
    
    public void updateModelValue() {
        T newValue = getModelValue(source);
        setValue(newValue);
    }
    
    abstract protected T getModelValue(S source);
    
    public void setSource(S source) {
        if (this.source != null) {
            uninstallSourceChangeListener(this.source);
        }
        
        this.source = source;
        updateModelValue();
        
        if (source != null) {
            installSourceChangeListener(source);
        }
    }
    
    abstract protected void installSourceChangeListener(S source);
    
    abstract protected void uninstallSourceChangeListener(S source);
}
