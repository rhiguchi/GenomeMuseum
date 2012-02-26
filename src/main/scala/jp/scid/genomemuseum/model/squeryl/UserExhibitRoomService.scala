package jp.scid.genomemuseum.model.squeryl

import java.util.Date

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{EventList, FilterList, matchers}
import matchers.Matcher

import jp.scid.gui.model.TreeSource.MappedPropertyChangeEvent
import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  UserExhibitRoomService => IUserExhibitRoomService, ExhibitRoom,
  MuseumExhibitService => IMuseumExhibitService, UriFileStorage,
  MuseumExhibit => IMuseumExhibit, MutableMuseumExhibitListModel => IMutableMuseumExhibitListModel,
  MuseumExhibitListModel => IMuseumExhibitListModel, GroupRoomContentsModel}
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
  
  /**
   * 子部屋を抽出する適合子
   */
  class ParentRoomMatcher(parent: Option[IUserExhibitRoom]) extends Matcher[UserExhibitRoom] {
    val parentId = parent.map(_.id).getOrElse(0L)
    
    def matches(room: UserExhibitRoom) = room.parentId.getOrElse(0L) == parentId
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
  import UserExhibitRoomService.ParentRoomMatcher
  
  /** 全ての部屋要素 */
  val allRoomList = new KeyedEntityEventList(table)
  
  /** 部屋を作成する。永続化はされない */
  def create(roomType: RoomType, baseName: String, parent: Option[IUserExhibitRoom]) = {
    require(parent.filter(_.roomType != GroupRoom).isEmpty, "roomType of parent must be GroupRoom")
    
    // TODO 名前検索
    UserExhibitRoom(baseName, roomType, parent.map(_.id))
  }
  
  /** 子要素のキャッシュ */
  //
  // ノード
  def addRoom(roomType: RoomType, name: String, parent: Option[IUserExhibitRoom]) = {
    val parentIdOp = parent flatMap {
      case RoomType(GroupRoom) => parent
      case elm => getParent(elm)
    } map {p => p.id}
    
    val newRoom = UserExhibitRoom(name, roomType, parentIdOp)
    allRoomList.add(newRoom)
    
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
  
  /**
   * 指定した部屋を親とするサブリストを作成。
   */
  def getFloorRoomList(parent: Option[IUserExhibitRoom]) =
    new FilterList(allRoomList, new ParentRoomMatcher(parent)).asInstanceOf[EventList[IUserExhibitRoom]]
  
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
  
  /**
   * 未使用の名前を検索する。
   * {@code baseName} の名前を持つ部屋がサービス中に存在するとき、
   * 連番をつけて次の名前を検索する。
   * @param baseName 基本の名前
   * @return 他と重複しない、部屋の名前。
   */
  private def findRoomNewName(baseName: String) = {
    def searchNext(index: Int): String = {
      val candidate = baseName + " " + index
      nameExists(candidate) match {
        case true => searchNext(index + 1)
        case false => candidate
      }
    }
    
    nameExists(baseName) match {
      case true => searchNext(1)
      case false => baseName
    }
  }
}
