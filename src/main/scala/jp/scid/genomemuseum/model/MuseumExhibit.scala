package jp.scid.genomemuseum.model

import java.util.Date
import net.liftweb.mapper._

case class MuseumExhibit(
  var name: String = "",
  var sequenceLength: Int = 0,
  var source: String = "",
  var date: Option[Date] = None
) extends LongKeyedMapper[MuseumExhibit] with IdPK {
  def getSingleton = MuseumExhibit
}

object MuseumExhibit extends MuseumExhibit("", 0, "", None) with LongKeyedMetaMapper[MuseumExhibit] {
}