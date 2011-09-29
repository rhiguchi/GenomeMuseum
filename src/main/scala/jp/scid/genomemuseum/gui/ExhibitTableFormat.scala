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
  
  def getColumnCount = 4
  def getColumnName(column: Int) = columnNames(column)
  def getColumnValue(e: MuseumExhibit, column: Int) = column match {
    case 0 => e.name
    case 1 => e.sequenceLength.asInstanceOf[AnyRef]
    case 2 => e.source
    case 3 => e.date.map(_.toString).getOrElse("")
  }
}

object ExhibitTableFormat {
  val columnNames = Array("name", "sequenceLength", "source", "date")
}