package jp.scid.genomemuseum.model

import org.specs2._
import mock._

object MuseumExhibitSpec extends Mockito {
  def mockOf(name: String) = MuseumExhibitMock.of(name)
}

object MuseumExhibitMock extends Mockito {
  def of(name: String) = {
    val e = mock[MuseumExhibit]
    e.name returns name
    e
  }
}