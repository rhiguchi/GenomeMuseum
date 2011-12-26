package jp.scid.genomemuseum.model

import org.specs2.mock.Mockito

import UserExhibitRoom.RoomType._

object UserExhibitRoomMock extends Mockito {
  def of(roomType: RoomType) = {
    val room = mock[UserExhibitRoom]
    room.roomType returns roomType
    room
  }
}
