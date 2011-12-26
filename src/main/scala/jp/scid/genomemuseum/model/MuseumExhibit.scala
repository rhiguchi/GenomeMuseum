package jp.scid.genomemuseum.model

import java.util.Date
import java.net.URI
import java.io.File

/**
 * 『展示物』インターフェイス
 */
trait MuseumExhibit {
  def id: Long
  var name: String
  var sequenceLength: Int
  var accession: String
  var identifier: String
  var namespace: String
  var version: Option[Int]
  var definition: String
  var source: String
  var organism: String
  var date: Option[Date]
  var sequenceUnit: String
  var moleculeType: String
  var filePath: String
  var fileSize: Long
  var fileType: MuseumExhibit.FileType.Value
  var filePathAsURI: URI
  
  var dataSourceUri: String
  
  def sourceFile: Option[File]
  def sourceFile_=(file: Option[File])
}

object MuseumExhibit {
  def apply(
    name: String = "",
    sequenceLength: Int = 0,
    accession: String = "",
    identifier: String = "",
    namespace: String = "",
    version: Option[Int] = None,
    definition: String = "",
    source: String = "",
    organism: String = "",
    date: Option[Date] = None,
    sequenceUnit: String = "",
    moleculeType: String = "",
    fileType: MuseumExhibit.FileType.Value = MuseumExhibit.FileType.Unknown
  ): MuseumExhibit = squeryl.MuseumExhibit(name, sequenceLength, accession, identifier, namespace,
    version, definition, source, organism, date, sequenceUnit, moleculeType,
    fileType = fileType)
  
  object FileType extends Enumeration {
    type FileType = Value
    val Unknown = Value(0, "Unknown")
    val GenBank = Value(1, "GenBank")
    val FASTA = Value(2, "FASTA")
  }
}
