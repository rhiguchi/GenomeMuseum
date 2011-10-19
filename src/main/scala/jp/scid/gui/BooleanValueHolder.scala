package jp.scid.gui

import event.BooleanValueChange

/**
 * 文字列保持データモデル
 */
class BooleanValueHolder(initialValue: Boolean = false)
    extends ValueModel[Boolean](initialValue) {
  def convertPropertyEvent(propertyName: String, newValue: AnyRef, oldValue: AnyRef) =
    BooleanValueChange(this, cast(newValue), cast(oldValue))
  
  override def cast(newValue: AnyRef) = {
    newValue.asInstanceOf[java.lang.Boolean].booleanValue
  }
}
