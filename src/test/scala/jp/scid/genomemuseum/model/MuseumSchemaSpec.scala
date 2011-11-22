package jp.scid.genomemuseum.model

import org.specs2.mock.Mockito

object MuseumSchemaSpec extends Mockito {
  def makeMock(schema: MuseumSchema) = {
    schema.userExhibitRoomService returns 
      UserExhibitRoomServiceSpec.makeMock(mock[UserExhibitRoomService])
    schema.museumExhibitService returns
      MuseumExhibitServiceSpec.makeMock(mock[MuseumExhibitService])
    schema.roomExhibitService(any) returns
      RoomExhibitServiceSpec.makeMock(mock[RoomExhibitService])
    schema
  }
}
