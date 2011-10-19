package jp.scid.gui

import event.IntValueChange

/**
 * 文字列保持データモデル
 */
class IntValueHolder(initialValue: Int = 0) extends
    ValueModel[Int](initialValue) {
  def convertPropertyEvent(propertyName: String, newValue: AnyRef, oldValue: AnyRef) =
    IntValueChange(this, cast(newValue), cast(oldValue))
  
  override def cast(newValue: AnyRef) = {
    newValue.asInstanceOf[java.lang.Integer].intValue
  }
}
