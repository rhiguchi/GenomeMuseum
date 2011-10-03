package jp.scid.genomemuseum.model

import java.util.Date
import net.liftweb.mapper._

class MuseumExhibit extends LongKeyedMapper[MuseumExhibit] with IdPK {
  def getSingleton = MuseumExhibit
  
  object name extends MappedText(this)
  object sequenceLength extends MappedInt(this)
  object source extends MappedText(this)
  object dateUpdate extends MappedDate(this)
  
  def date = Option(dateUpdate.is)
}

object MuseumExhibit extends MuseumExhibit with LongKeyedMetaMapper[MuseumExhibit] {
  def apply(name: String, len: Int): MuseumExhibit =
    apply(name, len, "")
  
  def apply(name: String, len: Int, source: String): MuseumExhibit = {
    val e = MuseumExhibit.create
    e.name(name).sequenceLength(len).source(source)
  }
}