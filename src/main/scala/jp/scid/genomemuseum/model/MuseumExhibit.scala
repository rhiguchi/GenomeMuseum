package jp.scid.genomemuseum.model

import java.util.Date

case class MuseumExhibit(
  var name: String = "",
  var sequenceLength: Int = 0,
  var source: String = "",
  var date: Option[Date] = None
) {
}