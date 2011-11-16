package jp.scid.gui.event

import swing.event.Event
import jp.scid.gui.ValueHolder

case class ValueChange[A](
  source: ValueHolder[A],
  oldValue: A,
  newValue: A
) extends DataModelEvent
