package jp.scid.genomemuseum.gui

import ca.odell.glazedlists.gui.TableFormat
import jp.scid.genomemuseum.model.SearchResult
import SearchResult.Status._

/**
 * {@code SearchResult} テーブルフォーマット
 */
class WebServiceResultTableFormat extends TableFormat[SearchResult] {
  import WebServiceResultTableFormat._
  
  def getColumnCount = columnNames.length
  def getColumnName(column: Int) = columnNames(column)
  def getColumnValue(e: SearchResult, column: Int) = column match {
    case 0 => e.status // 状態 or ダウンロードボタン
    case 1 => e.identifier
    case 2 => e.accession
    case 3 => e.definition
    case 4 => e.length.asInstanceOf[AnyRef]
  }
}

object WebServiceResultTableFormat {
  private val columnNames = Array("status", "identifier",
    "accession", "definition", "length")
}