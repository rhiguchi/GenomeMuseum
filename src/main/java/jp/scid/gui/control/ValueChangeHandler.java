package jp.scid.gui.control;

import jp.scid.gui.model.ValueModel;

public abstract class ValueChangeHandler<T> extends AbstractController<ValueModel<T>> {

    @Override
    protected void processPropertyChange(ValueModel<T> model, String property) {
        T newValue = model.getValue();
        valueChanged(newValue);
    }

    abstract protected void valueChanged(T newValue);
}
