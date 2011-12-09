package jp.scid.genomemuseum.model

import org.specs2._
import mock._

import UserExhibitRoom.RoomType._

object UserExhibitRoomSpec extends Mockito {
  def mockOf(roomType: RoomType) = {
    val room = mock[UserExhibitRoom]
    room.roomType returns roomType
    room
  }
}