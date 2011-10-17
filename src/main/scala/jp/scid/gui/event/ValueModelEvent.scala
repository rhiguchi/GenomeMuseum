package jp.scid.gui.event

import swing.event.Event
import jp.scid.gui.{ValueModel, StringValueModel, ScalaEventTrait}

/**
 * ValueModel のイベント
 */
trait ValueModelEvent[A <: AnyRef] extends DataModelEvent {
  val source: ValueModel[A]
  val newValue: A
  val oldValue: A
}

case class StringValueChanged(
  source: StringValueModel,
  newValue: String = "",
  oldValue: String = ""
) extends ValueModelEvent[String]
