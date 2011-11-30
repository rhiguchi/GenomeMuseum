package jp.scid.genomemuseum.gui

import ca.odell.glazedlists.gui.{AdvancedTableFormat, WritableTableFormat}
import jp.scid.genomemuseum.model.{SearchResult, TaskProgressModel}

/**
 * {@code SearchResult} テーブルフォーマット
 */
class WebServiceResultTableFormat extends AdvancedTableFormat[SearchResult]
    with WritableTableFormat[SearchResult] {
  import WebServiceResultTableFormat._
  
  def getColumnCount = columnNames.length
  def getColumnName(column: Int) = columnNames(column)
  def getColumnValue(e: SearchResult, column: Int) = column match {
    case 0 => e
    case 1 => e.identifier
    case 2 => e.accession
    case 3 => e.definition
    case 4 => e.length.asInstanceOf[AnyRef]
  }
  
  def getColumnClass(column: Int) = column match {
    case 0 => classOf[TaskProgressModel]
    case 4 => classOf[java.lang.Integer]
    case _ => classOf[Object]
  }
  
  def isEditable(obj: SearchResult, column: Int) = column match {
    case 0 => true
    case _ => false
  }
  
  def setColumnValue(obj: SearchResult, value: AnyRef, column: Int) = {
    obj
  } 
  
  def getColumnComparator(column: Int) = null
}

object WebServiceResultTableFormat {
  private val columnNames = Array("status", "identifier",
    "accession", "definition", "length")
}