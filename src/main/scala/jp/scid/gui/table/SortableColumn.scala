package jp.scid.gui.table

import javax.swing.table.TableColumn

/**
 * 並び替え情報を持つ値のトレイト
 */
trait SortableColumn {
  def orderStatements: List[String]
}
