package jp.scid.genomemuseum.gui

import java.util.Date
import java.util.Comparator

import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.gui.AdvancedTableFormat
import jp.scid.genomemuseum.model
import model.{MuseumExhibit}


object ExhibitTableFormat {
  private val comparableComparator = GlazedLists.comparableComparator[Comparable[_]]
  
  private class ColumnValueComparator(column: Column) extends Comparator[MuseumExhibit] {
    def compare(e1: MuseumExhibit, e2: MuseumExhibit) = {
      val v1 = column getColumnValue e1
      val v2 = column getColumnValue e2
      
      comparableComparator.compare(v1.asInstanceOf[Comparable[AnyRef]], v2.asInstanceOf[Comparable[AnyRef]])
    }
  }
  
  abstract class Column(val identifier: String) {
    def columnClass: Class[_] = classOf[String]
    
    def getColumnValue(e: MuseumExhibit): AnyRef
    
    def comparator: Comparator[MuseumExhibit] = new ColumnValueComparator(this)
  }
  
  object Name extends Column("name") {
    def getColumnValue(e: MuseumExhibit) = e.name
  }
  
  object SequenceLength extends Column("sequenceLength") {
    def getColumnValue(e: MuseumExhibit) = e.sequenceLength.asInstanceOf[AnyRef]
    
    override def columnClass = classOf[java.lang.Integer]
  }
  
  object Accession extends Column("accession") {
    def getColumnValue(e: MuseumExhibit) = e.accession
  }
  
  object Identifier extends Column("identifier") {
    def getColumnValue(e: MuseumExhibit) = e.identifier
  }
  
  object Namespace extends Column("namespace") {
    def getColumnValue(e: MuseumExhibit) = e.namespace
  }
  
  object Version extends Column("version") {
    def getColumnValue(e: MuseumExhibit) = e.version.map(e.accession + "." + _).getOrElse("")
  }
  
  object Definition extends Column("definition") {
    def getColumnValue(e: MuseumExhibit) = e.definition
  }
  
  object Source extends Column("source") {
    def getColumnValue(e: MuseumExhibit) = e.source
  }
  
  object Organism extends Column("organism") {
    def getColumnValue(e: MuseumExhibit) = e.organism
  }
  
  object Date extends Column("date") {
    def getColumnValue(e: MuseumExhibit) = e.date.map(_.toString).getOrElse("")
  }
  
  object SequenceUnit extends Column("sequenceUnit") {
    def getColumnValue(e: MuseumExhibit) = e.sequenceUnit
  }
  
  object MoleculeType extends Column("moleculeType") {
    def getColumnValue(e: MuseumExhibit) = e.moleculeType
  }
}

/**
 * MuseumExhibit テーブルフォーマット
 */
class ExhibitTableFormat extends AdvancedTableFormat[MuseumExhibit] {
  import ExhibitTableFormat._
  
  val columns =
    Vector(Name, SequenceLength, Accession, Identifier, Namespace, Version,
      Definition, Source, Organism, ExhibitTableFormat.Date, SequenceUnit, MoleculeType)
  
  def getColumnCount = columns.size
  def getColumnName(column: Int) = columns(column).identifier
  def getColumnValue(e: MuseumExhibit, column: Int) = columns(column).getColumnValue(e)
  def getColumnClass(column: Int) = columns(column).columnClass
  def getColumnComparator(column: Int) = columns(column).comparator
}
