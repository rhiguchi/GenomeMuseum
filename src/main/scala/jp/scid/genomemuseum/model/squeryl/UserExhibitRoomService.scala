package jp.scid.genomemuseum.model.squeryl

import java.util.Date

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.PrimitiveTypeMode._

import jp.scid.gui.model.TreeSource.MappedPropertyChangeEvent
import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  UserExhibitRoomService => IUserExhibitRoomService, ExhibitRoom,
  MuseumExhibitService => IMuseumExhibitService, UriFileStorage,
  MuseumExhibit => IMuseumExhibit, MutableMuseumExhibitListModel => IMutableMuseumExhibitListModel,
  MuseumExhibitListModel => IMuseumExhibitListModel, GroupRoomContentsModel,
  ExhibitRoomContentsService}
import IUserExhibitRoom.RoomType
import RoomType._

object UserExhibitRoomService {
  /**
   * 親IDを取得する
   */
  def getParentId(roomId: Long, roomTable: Table[UserExhibitRoom]): Option[Long] = inTransaction {
    from(roomTable)(e =>
        where(e.id === roomId) select(e.parentId)).headOption.flatMap(a => a)
  }
  
  /** 部屋の子を取得する */
  def getChildren(roomId: Long, roomTable: Table[UserExhibitRoom]) =
    roomTable.where(e => e.parentId === roomId).toList

  /** 部屋の要素の ID を取得 */
  def getElementIds(roomId: Long, relationTable: Table[RoomExhibit]) =
    from(relationTable)(e =>
      where(e.roomId === roomId) select(e.exhibitId) orderBy(e.id asc)).toList
  
  /** GroupRoom を展開し、葉要素のみを取得する */
  def getLeafs(rootId: Long, roomTable: Table[UserExhibitRoom]) = {
    import scala.collection.mutable.{Buffer, ListBuffer}
    
    @annotation.tailrec
    def getLeafs(rooms: List[UserExhibitRoom], leafs: Buffer[UserExhibitRoom]): Buffer[UserExhibitRoom] = {
      rooms match {
        case Nil => leafs
        case head :: tail => head.roomType match {
          case GroupRoom => 
            getLeafs(getChildren(head.id, roomTable) ::: tail, leafs)
          case _ => getLeafs(tail, leafs += head)
        }
      }
    }
    
    val rooms = roomTable.lookup(rootId).toList
    getLeafs(rooms, ListBuffer.empty).toList
  }
}

/**
 * GenomeMuseum データソースの Squeryl 実装
 */
private[squeryl] class UserExhibitRoomService(
  table: Table[UserExhibitRoom],
  exhibitTable: Table[MuseumExhibit],
  relationTable: Table[RoomExhibit]
) extends IUserExhibitRoomService {
  /** 子要素のキャッシュ */
  //
  // ノード
  def addRoom(roomType: RoomType, name: String, parent: Option[IUserExhibitRoom]) = {
    val parentIdOp = parent flatMap {
      case RoomType(GroupRoom) => parent
      case elm => getParent(elm)
    } map {p => p.id}
    
    val newRoom = UserExhibitRoom(name, roomType, parentIdOp)
    inTransaction {
      table.insert(newRoom)
    }
    
    fireChildrenChange(parent)
    
    newRoom
  }
  
  def nameExists(name: String): Boolean = inTransaction {
    table.where( e => e.name === name).nonEmpty
  }
  
  def getParent(element: IUserExhibitRoom) = {
    parentFor(element.id)
  }
  
  def setParent(element: IUserExhibitRoom, parent: Option[IUserExhibitRoom]) {
    parent foreach ensureParentAllowed
    
    val parentId = parent.map(_.id)
    val oldParent = parentFor(element.id)
    
    inTransaction {
      update(table) ( e =>
        where(e.id === element.id)
        set(e.parentId := parentId)
      )
    }
    
    fireMappedPropertyChangeEvent("parent", element, oldParent, parent)
  }
  
  def getChildren(parent: Option[IUserExhibitRoom]) =
    retrieveChildren(parent.map(_.id).getOrElse(0L)).toList
  
  private def retrieveChildren(parentId: Long) = inTransaction {
    table.where(e => nvl(e.parentId, 0L) === parentId).toIndexedSeq
  }
  
  /** 全ての子孫にある葉要素を返す */
  def getAllLeafs(room: IUserExhibitRoom) = inTransaction {
    UserExhibitRoomService.getLeafs(room.id, table)
  }
  
  def remove(element: IUserExhibitRoom) = {
    val parent = parentFor(element.id)
    
    val result = inTransaction {
      table.delete(element.id)
    }
    result match {
      case true =>
      case false => fireChildrenChange(parent)
    }
    result
  }
  
  def save(element: IUserExhibitRoom) = inTransaction {
    element match {
      case element: UserExhibitRoom => table.update(element)
      case _ =>
    }
  }
  
  private def fireChildrenChange(parent: Option[IUserExhibitRoom]) {
    fireMappedPropertyChangeEvent("children", parent, null, null)
  }
  
  private def fireMappedPropertyChangeEvent(
      propertyName: String, key: AnyRef, oldValue: AnyRef, newValue: AnyRef) {
    val pce = new MappedPropertyChangeEvent(this, propertyName, key, oldValue, newValue)
    firePropertyChange(pce)
  }
  
  //
  // コンテンツ
  /**
   * 要素を追加する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def addExhibit(room: IUserExhibitRoom, element: IMuseumExhibit) = inTransaction {
    exhibitTable.lookup(element.id) match {
      case Some(exhibit) =>
        relationTable.insert(RoomExhibit(room.id, exhibit.id))
        fireMappedPropertyChangeEvent("exhibitList", room, null, null)
        true
      case None => false
    }
  }
  
  /**
   * 要素を除去する。
   * 
   * 要素の ID が存在するとき、この部屋から除去される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def removeExhibit(room: IUserExhibitRoom, element: IMuseumExhibit) = inTransaction {
    val relations = relationTable.where(e =>
        e.exhibitId === element.id and e.roomId === room.id).toList
    relations.headOption map { relation => relationTable delete relation.id } match {
      case Some(true) =>
        fireMappedPropertyChangeEvent("exhibitList", room, null, null)
        true
      case _ => false
    }
  }
  
  /**
   * 部屋のコンテンツを表示する
   */
  def getExhibitList(room: IUserExhibitRoom) = inTransaction {
    val exhibitIdList = UserExhibitRoomService.getLeafs(room.id, table)
      .flatMap(r => UserExhibitRoomService.getElementIds(r.id, relationTable))
    
    val elements = exhibitTable.where(e => e.id in exhibitIdList).toIndexedSeq
    val elmMap = elements.view.map(e => (e.id, e)).toMap
    exhibitIdList.view.flatMap(id => elmMap.get(id)).toList
  }
  
  /**
   * ID から親要素を取得する
   */
  private[squeryl] def parentFor(roomId: Long) = inTransaction {
    from(table)(e => where(e.id === roomId) select(e.parentId))
        .headOption.flatMap(id => id).flatMap(id => table.lookup(id))
  }
  
  /**
   * 親として設定できる要素であるか
   */
  private def ensureParentAllowed(room: IUserExhibitRoom) {
    room match {
      case RoomType(GroupRoom) =>
      case _ => throw new IllegalArgumentException("parent must be a GroupRoom")
    }
  }
}
