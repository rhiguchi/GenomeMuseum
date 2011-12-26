package jp.scid.genomemuseum.model

import org.specs2.mock.Mockito

object MuseumSchemaMock extends Mockito {
  def of(roomService: UserExhibitRoomService, exhibitService: MuseumExhibitService) = {
    val schema = mock[MuseumSchema]
    schema.userExhibitRoomService returns roomService
    schema.museumExhibitService returns exhibitService
    schema
  }
}
