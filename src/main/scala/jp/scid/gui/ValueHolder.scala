package jp.scid.gui

import java.beans.{PropertyChangeListener, PropertyChangeEvent}

import swing.Publisher
import event.ValueChange

import com.jgoodies.binding.{value => bv}

/**
 * 一つの値を保持し、変化イベントを送出するデータモデル。
 * jGoodies Binding の Scala 実装。
 */
class ValueHolder[T: ClassManifest](initialValue: T) extends DataModel with Publisher {
  /** 現在の値 */
  private var currentValue: T = initialValue
  
  /** binding 用内部実装 */
  private val jgoodiesDelegate = new bv.ValueHolder(initialValue, false)
  jgoodiesDelegate.addPropertyChangeListener(new PropertyChangeListener() {
    def propertyChange(evt: PropertyChangeEvent) {
      val newVal = evt.getNewValue match {
        case null => initialValue
        case value => value.asInstanceOf[T]
      }
      :=(newVal)
    }
  })
  
  /** 値の取得 */
  def apply(): T = currentValue
  
  /** 値の適用 */
  def :=(newValue: T) = {
    if (currentValue != newValue) {
      val old = currentValue
      currentValue = newValue
      publishValueChange(old, newValue)
      jgoodiesDelegate.setValue(newValue)
    }
  }
  
  protected def publishValueChange(oldValue: T, newValue: T) {
    val event = ValueChange(this, oldValue, newValue)
    publish(event)
  }
}

object ValueHolder {
  import com.jgoodies.binding.adapter.TextComponentConnector
  import com.jgoodies.binding.beans.PropertyConnector
  import javax.swing.{JTextField, JComponent}
  import DataModel.Connector
  
  /**
   * String モデルと JTextField を結合する。
   */
  def connect(subject: ValueHolder[String],
      textField: JTextField): Connector = {
    val conn = new TextComponentConnector(subject.jgoodiesDelegate, textField)
    conn.updateTextComponent()
    new ConnectorTextImpl(conn)
  }
  
  /**
   * モデルと Java Bean オブジェクトを結合する。
   */
  def connect(subject: ValueHolder[_], component: AnyRef,
      property: String): Connector = {
    val conn = PropertyConnector.connect(subject.jgoodiesDelegate,
      "value", component, property)
    conn.updateProperty2()
    new ConnectorPropertyImpl(conn)
  }
  
  /**
   * Boolean 型 モデルと JComponent の isVisible のプロパティを結合する。
   */
  def connectVisible(subject: ValueHolder[Boolean], component: JComponent): Connector = {
    connect(subject, component, "visible")
  }
  
  private class ConnectorTextImpl(base: TextComponentConnector) extends Connector {
    def release() = base.release()
  }
  
  private class ConnectorPropertyImpl(base: PropertyConnector) extends Connector {
    def release() = base.release()
  }
}
