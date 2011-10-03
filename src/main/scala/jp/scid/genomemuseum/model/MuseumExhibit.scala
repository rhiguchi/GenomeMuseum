package jp.scid.genomemuseum.model

import java.util.Date
import org.squeryl.KeyedEntity

case class MuseumExhibit(
  var name: String = "",
  var sequenceLength: Int = 0,
  var source: String = "",
  var date: Option[Date] = None
) extends KeyedEntity[Long] {
  def this() = this("")
  var id: Long = 0
}
