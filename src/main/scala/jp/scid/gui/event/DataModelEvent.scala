package jp.scid.gui.event

import swing.event.Event
import jp.scid.gui.DataModel

/**
 * GUI データモデルのイベント
 */
trait DataModelEvent extends Event {
  val source: DataModel
}