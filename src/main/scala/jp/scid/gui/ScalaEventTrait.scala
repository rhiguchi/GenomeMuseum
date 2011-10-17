package jp.scid.gui

import java.beans.{PropertyChangeListener, PropertyChangeEvent}

import swing.Publisher

import com.jgoodies.binding.value.{ValueModel => jgValueModel}

import event.DataModelEvent

/**
 * JGoodeis との接続トレイト
 */
trait ScalaEventTrait {
  this: jgValueModel with Publisher =>
  
  private val selfPCL = new JavaEventHandler(this)
  addValueChangeListener(selfPCL)
  
  protected[gui] def convertEvent(propertyName: String, newValue: AnyRef, oldValue: AnyRef): DataModelEvent
}

private class JavaEventHandler(set: ScalaEventTrait with Publisher) extends PropertyChangeListener {
  def propertyChange(evt: PropertyChangeEvent) {
    val pubEvt = set.convertEvent(evt.getPropertyName, evt.getNewValue, evt.getOldValue)
    set.publish(pubEvt)
  }
}
