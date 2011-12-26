package jp.scid.genomemuseum.model

import org.specs2._
import mock._

object MuseumExhibitMock extends Mockito {
  def of(name: String) = {
    val e = mock[MuseumExhibit]
    e.name returns name
    e
  }
}
