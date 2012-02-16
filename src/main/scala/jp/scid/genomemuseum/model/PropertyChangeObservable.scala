package jp.scid.genomemuseum.model

import java.beans.{PropertyChangeListener, PropertyChangeSupport, PropertyChangeEvent}

private[model] trait PropertyChangeObservable {
  
  private[model] lazy val propertyChangeSupport = new PropertyChangeSupport(this)
  
  def addPropertyChangeListener(listener: PropertyChangeListener) =
    propertyChangeSupport.addPropertyChangeListener(listener)
    
  def removePropertyChangeListener(listener: PropertyChangeListener) =
    propertyChangeSupport.removePropertyChangeListener(listener)
  
  protected[model] def firePropertyChange(propertyName: String, oldValue: Any, newValue: Any) =
    propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue)
  
  protected[model] def firePropertyChange(event: PropertyChangeEvent) =
    propertyChangeSupport.firePropertyChange(event)
  
  protected[model] def fireValuePropertyChange(oldValue: Any, newValue: Any) =
    firePropertyChange("value", oldValue, newValue)
  
  protected[model] def fireIndexedPropertyChange(
      propertyName: String, index: Int, oldValue: Any, newValue: Any) =
    propertyChangeSupport.fireIndexedPropertyChange(propertyName, index, oldValue, newValue)
  
  protected[model] def fireValueIndexedPropertyChange(index: Int, oldValue: Any, newValue: Any) =
    propertyChangeSupport.fireIndexedPropertyChange("value", index, oldValue, newValue)
}