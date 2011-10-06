package jp.scid.gui.table

/**
 * 並び替え情報を持つ値のトレイト
 */
trait SortableColumn {
  def orderStatements: List[String]
  var orderStatement: String = ""
}
