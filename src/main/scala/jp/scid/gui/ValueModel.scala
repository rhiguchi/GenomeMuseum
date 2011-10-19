package jp.scid.gui

import swing.Publisher
import com.jgoodies.binding.value.AbstractValueModel

/**
 * 一つの値を保持し、変化イベントを送出するデータモデル
 */
abstract class ValueModel[T: ClassManifest](initialValue: T) extends AbstractValueModel
    with DataModel with ScalaEventTrait with Publisher {
      
  private var currentValue: T = initialValue
  
  /** 値の取得 */
  def getValue = currentValue.asInstanceOf[AnyRef]
  
  /** 値の適用 */
  def setValue(newValue: AnyRef) {
    value = cast(newValue)
  }
  
  /** プロパティ取得 */
  def value = currentValue
  
  /** プロパティ適用 */
  def value_=(newValue: T) {
    if (currentValue != newValue) {
      val old = currentValue
      currentValue = newValue
      fireValueChange(old, newValue, false)
    }
  }
  
  /** 値の取得ショートカット */
  def apply(): T = value
  
  /** 値の適用ショートカット */
  def :=(newValue: T) = value = newValue
  
  protected def cast(value: AnyRef): T = {
    // キャストに失敗すると Exception が発生する
    try {
      implicitly[ClassManifest[T]].erasure.cast(value).asInstanceOf[T]
    }
    catch {
      case e: ClassCastException =>
        throw new IllegalArgumentException("value '%s' of class '%s' cannot a '%s' "
          .format(value, value.getClass, implicitly[ClassManifest[T]].erasure), e)
    }
  }
}
