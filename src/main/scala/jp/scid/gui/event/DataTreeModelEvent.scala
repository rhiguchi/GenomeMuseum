package jp.scid.gui.event

import jp.scid.gui.tree.DataTreeModel
import jp.scid.gui.tree.DataTreeModel.Path

/**
 * DataTreeModel のイベント
 */
trait DataTreeModelEvent[A <: AnyRef] extends DataModelEvent {
  val source: DataTreeModel[A]
}

case class DataTreePathsSelectionChanged[A <: AnyRef](
  source: DataTreeModel[A],
  newPaths: List[Path[A]] = Nil,
  oldPaths: List[Path[A]] = Nil
) extends DataTreeModelEvent[A] {
}