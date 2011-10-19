package jp.scid.gui

import event.StringValueChanged

/**
 * 文字列保持データモデル
 */
class StringValueModel(initialValue: String = "")
    extends ValueModel[String](initialValue) {
  def convertPropertyEvent(propertyName: String, newValue: AnyRef, oldValue: AnyRef) =
    StringValueChanged(this, cast(newValue), cast(oldValue))
}
