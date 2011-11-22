package jp.scid.genomemuseum.model

import org.specs2.mock._

object MuseumExhibitServiceSpec extends Mockito {
  def makeMock(mock: MuseumExhibitService) = {
    ListDataServiceSpec.makeMock(mock)
    mock
  }
}

object RoomExhibitServiceSpec extends Mockito {
  def makeMock(mock: RoomExhibitService) = {
    MuseumExhibitServiceSpec.makeMock(mock)
    mock
  }
}