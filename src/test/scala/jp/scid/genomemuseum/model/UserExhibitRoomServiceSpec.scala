package jp.scid.genomemuseum.model

import org.specs2.mock.Mockito

object UserExhibitRoomServiceSpec extends Mockito {
  def makeMock(service: UserExhibitRoomService) = {
    TreeDataServiceSpec.makeMock(service)
    service.addRoom(any, anyString, any) returns mock[UserExhibitRoom]
    service.nameExists(anyString) returns false
    service
  }
}