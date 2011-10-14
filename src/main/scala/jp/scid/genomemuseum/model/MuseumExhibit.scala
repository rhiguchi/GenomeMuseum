package jp.scid.genomemuseum.model

import java.util.Date
import java.net.URI
import org.squeryl.KeyedEntity

case class MuseumExhibit(
  var name: String = "",
  var sequenceLength: Int = 0,
  var accession: String = "",
  var identifier: String = "",
  var namespace: String = "",
  var version: Option[Int] = None,
  var definition: String = "",
  var source: String = "",
  var organism: String = "",
  var date: Option[Date] = None,
  var sequenceUnit: String = "",
  var moleculeType: String = "",
  var filePath: String = "",
  var fileSize: Long = 0,
  var fileType: MuseumExhibit.FileType.Value = MuseumExhibit.FileType.Unknown
) extends KeyedEntity[Long] {
  def this() = this("", version = Some(0))
  var id: Long = 0
  
  def filePathAsURI = URI.create(filePath)
  def filePathAsURI_=(uri: URI) {
    filePath = uri.toString
  }
}

object MuseumExhibit {
  object FileType extends Enumeration {
    type FileType = Value
    val Unknown = Value(0, "Unknown")
    val GenBank = Value(1, "GenBank")
    val FASTA = Value(2, "FASTA")
  }
}