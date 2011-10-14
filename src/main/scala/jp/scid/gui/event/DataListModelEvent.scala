package jp.scid.gui.event

import jp.scid.gui.DataListModel

/**
 * DataTableModel のイベント
 */
trait DataListModelEvent[A] extends DataModelEvent {
  val source: DataListModel[A]
}

case class DataListSelectionChanged[A](
  source: DataListModel[A],
  isAdjusting: Boolean,
  selections: List[A] = Nil
) extends DataListModelEvent[A] {
}
