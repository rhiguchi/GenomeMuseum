package jp.scid.gui.event

import swing.event.Event
import jp.scid.gui.{ValueModel, ScalaEventTrait, StringValueModel, BooleanValueHolder,
  IntValueHolder}

/**
 * ValueModel のイベント
 */
trait ValueModelEvent[A] extends DataModelEvent {
  val source: ValueModel[A]
  val newValue: A
  val oldValue: A
}

case class StringValueChanged(
  source: StringValueModel,
  newValue: String = "",
  oldValue: String = ""
) extends ValueModelEvent[String]

case class BooleanValueChange(
  source: BooleanValueHolder,
  newValue: Boolean,
  oldValue: Boolean
) extends ValueModelEvent[Boolean]

case class IntValueChange(
  source: IntValueHolder,
  newValue: Int,
  oldValue: Int
) extends ValueModelEvent[Int]
