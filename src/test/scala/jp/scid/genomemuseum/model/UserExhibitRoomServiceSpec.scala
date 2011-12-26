package jp.scid.genomemuseum.model

object UserExhibitRoomServiceMock extends org.specs2.mock.Mockito {
  def of(tree: (UserExhibitRoom, Seq[UserExhibitRoom])*) = {
    val service = mock[UserExhibitRoomService]
    service.addRoom(any, anyString, any) returns mock[UserExhibitRoom]
    service.getChildren(any) returns Nil
    service.getParent(any) returns None
    
    tree.foreach { case (parent, children) =>
      val pOpt = Some(parent)
      service.getChildren(pOpt) returns children
      children foreach { child =>
        service.getParent(child) returns pOpt
      }
    }
    service
  }
  
  def canPublish() = {
    val service = spy(new EventUserExhibitRoomService)
    service.addRoom(any, anyString, any) returns mock[UserExhibitRoom]
    service.getChildren(any) returns Nil
    service.getParent(any) returns None
    service
  }
}

class EventUserExhibitRoomService extends UserExhibitRoomService {
  import UserExhibitRoom.RoomType._
  import scala.collection.script.Message
  
  override def publish(event: Message[UserExhibitRoom]) {
    super.publish(event)
  }
  def addRoom(roomType: RoomType, name: String,
    parent: Option[UserExhibitRoom]): UserExhibitRoom = null
  def nameExists(name: String) = false
  def remove(element: UserExhibitRoom) = false
  def setParent(element: UserExhibitRoom, parent: Option[UserExhibitRoom]) {}
  def getChildren(parent: Option[UserExhibitRoom]): Iterable[UserExhibitRoom] = Nil
  def getParent(element: UserExhibitRoom): Option[UserExhibitRoom] = None
  def save(element: UserExhibitRoom) {}
}
