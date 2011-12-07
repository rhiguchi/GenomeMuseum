package jp.scid.genomemuseum.model.squeryl

import java.util.Date

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  UserExhibitRoomService => IUserExhibitRoomService}
import IUserExhibitRoom.RoomType._

/**
 * GenomeMuseum データソースの Squeryl 実装
 */
private[squeryl] class UserExhibitRoomService(private[squeryl] val table: Table[UserExhibitRoom])
    extends IUserExhibitRoomService with UserExhibitRoomPublisher {
  type Node = UserExhibitRoom
  
  def userExhibitRoomTablePublisher = SquerylTriggerAdapter.connect(table, 2)
  
  def addRoom(roomType: RoomType, name: String,
      parent: Option[IUserExhibitRoom]) = {
    ensureParentAllowed(parent)
    
    val parentIdOp = parent.map(_.id)
    val newRoom = UserExhibitRoom(name, roomType, parentIdOp)
    
    inTransaction {
      table.insert(newRoom)
    }
    
    newRoom
  }
  
  def nameExists(name: String): Boolean = inTransaction {
    table.where( e => e.name === name).nonEmpty
  }
  
  def getParent(element: IUserExhibitRoom): Option[UserExhibitRoom] = {
    parentFor(element.id)
  }
  
  def setParent(element: IUserExhibitRoom, parent: Option[IUserExhibitRoom]) {
    ensureParentAllowed(parent)
    
    val parentId = parent.map(_.id)
    inTransaction {
      update(table) ( e =>
        where(e.id === element.id)
        set(e.parentId := parentId)
      )
    }
  }
  
  def getChildren(parent: Option[IUserExhibitRoom]): List[UserExhibitRoom] = {
    inTransaction {
      table.where(e => nvl(e.parentId, 0L) === parent.map(_.id).getOrElse(0L)).toList
    }
  }
  
  def remove(element: IUserExhibitRoom) = inTransaction {
    table.delete(element.id)
  }
  
  def save(element: IUserExhibitRoom) = inTransaction {
    element match {
      case element: UserExhibitRoom => table.update(element)
      case _ =>
    }
  }
  
  /**
   * ID から親要素を取得する
   */
  private[squeryl] def parentFor(roomId: Long) = inTransaction {
    table.where(e =>
      e.id === from(table)(e =>
        where(e.id === roomId) select(e.parentId)).head)
      .headOption
  }
  
  /**
   * 親として設定できる要素であるか
   */
  private def ensureParentAllowed(value: Option[IUserExhibitRoom]) {
    value match {
      case Some(elm) if elm.roomType != GroupRoom =>
        throw new IllegalArgumentException()
      case _ =>
    }
  }
}
