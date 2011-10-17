package jp.scid.gui

import swing.Publisher
import event.StringValueChanged
import com.jgoodies.binding.value.AbstractValueModel

/**
 * 文字列保持データモデル
 */
class StringValueModel extends ValueModel[String] with ScalaEventTrait with Publisher {
  def this(initialValue: String) {
    this()
    value = initialValue
  }
  
  protected def valueForNull = ""
  
  def convertEvent(propertyName: String, newValue: AnyRef, oldValue: AnyRef) =
    StringValueChanged(this, cast(newValue), cast(oldValue))
}

abstract class ValueModel[T: ClassManifest] extends AbstractValueModel
    with DataModel with ScalaEventTrait with Publisher {
      
  private var currentValue: T = valueForNull
  
  def getValue = currentValue.asInstanceOf[AnyRef]
  
  def setValue(newValue: AnyRef) {
    value = cast(newValue)
  }
  
  def value = currentValue
  
  def value_=(newValue: T) {
    if (currentValue != newValue) {
      val old = currentValue
      currentValue = newValue
      fireValueChange(old, newValue, false)
    }
  }
  
  protected def valueForNull: T
  
  protected def cast(value: AnyRef): T = {
    // キャストに失敗すると Exception が発生する
    implicitly[ClassManifest[T]].erasure.cast(value).asInstanceOf[T]
  }
}
