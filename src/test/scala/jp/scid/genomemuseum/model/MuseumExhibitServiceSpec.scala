package jp.scid.genomemuseum.model

import org.specs2.mock._

object MuseumExhibitServiceSpec extends Mockito {
  def makeMock(mock: MuseumExhibitService) = {
    mock.allElements returns Nil
    mock.getExhibits(any) returns Nil
    mock
  }
}
