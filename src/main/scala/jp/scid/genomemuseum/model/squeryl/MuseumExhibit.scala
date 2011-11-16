package jp.scid.genomemuseum.model.squeryl

import java.util.Date
import java.net.URI
import org.squeryl.KeyedEntity

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit}
import IMuseumExhibit.FileType

/**
 * MuseumExhibit の Squeryl 用実装
 */
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
  var fileType: FileType.Value = FileType.Unknown
) extends IMuseumExhibit with KeyedEntity[Long] {
  def this() = this("", version = Some(0))
  var id: Long = 0
  
  def filePathAsURI = URI.create(filePath)
  def filePathAsURI_=(uri: URI) {
    filePath = uri.toString
  }
}
