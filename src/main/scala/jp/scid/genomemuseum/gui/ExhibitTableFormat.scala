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
  
  def getColumnCount = 12
  def getColumnName(column: Int) = columnNames(column)
  def getColumnValue(e: MuseumExhibit, column: Int) = column match {
    case 0 => e.name
    case 1 => e.sequenceLength.asInstanceOf[AnyRef]
    case 2 => e.accession
    case 3 => e.identifier
    case 4 => e.namespace
    case 5 => e.version.map(e.accession + "." + _).getOrElse("")
    case 6 => e.definition
    case 7 => e.source
    case 8 => e.organism
    case 9 => e.date.map(_.toString).getOrElse("")
    case 10 => e.sequenceUnit
    case 11 => e.moleculeType
  }
}

object ExhibitTableFormat {
  val columnNames = Array("name", "sequenceLength", "accession", "identifier",
    "namespace", "version", "definition", "source", "organism", "date",
    "sequenceUnit", "moleculeType")
}