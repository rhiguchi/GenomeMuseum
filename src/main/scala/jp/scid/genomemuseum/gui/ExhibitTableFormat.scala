package jp.scid.genomemuseum.gui

import java.util.Date
import ca.odell.glazedlists.gui.TableFormat
import jp.scid.genomemuseum.model
import model.{MuseumExhibit}

/**
 * MuseumExhibit テーブルフォーマット
 */
class ExhibitTableFormat extends TableFormat[MuseumExhibit] {
  import ExhibitTableFormat._
  
  def getColumnCount = columnNames.size
  def getColumnName(column: Int) = columnNames(column)
  def getColumnValue(e: MuseumExhibit, column: Int) = ""
}

object ExhibitTableFormat {
  val columnNames = Array("col1", "col2", "col3")
}