package jp.scid.genomemuseum.model

import java.util.Date
import org.squeryl.KeyedEntity

case class MuseumExhibit(
  var name: String = "",
  var sequenceLength: Int = 0,
  var accession: String = "",
  var identifier: String = "",
  var namespace: String = "",
  var version: Option[Int] = Some(0),
  var definition: String = "",
  var source: String = "",
  var organism: String = "",
  var date: Option[Date] = None,
  var sequenceUnit: String = "",
  var moleculeType: String = ""
) extends KeyedEntity[Long] {
  def this() = this("")
  var id: Long = 0
}
