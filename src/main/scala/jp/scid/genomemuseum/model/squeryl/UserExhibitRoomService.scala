package jp.scid.genomemuseum.model.squeryl

import java.util.Date

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{EventList, FilterList, matchers}
import matchers.Matcher

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  UserExhibitRoomService => IUserExhibitRoomService}
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
    
    def matches(room: UserExhibitRoom) = room.parentId match {
      case Some(`parentId`) => true
      case _ => false
    }
  }
}

/**
 * GenomeMuseum データソースの Squeryl 実装
 */
class UserExhibitRoomService(table: Table[UserExhibitRoom]) extends IUserExhibitRoomService {
  import UserExhibitRoomService.ParentRoomMatcher
  
  /** 全ての部屋要素 */
  val allRoomList = new KeyedEntityEventList(table)
  
  /** 部屋を作成する。永続化はされない */
  def create(roomType: RoomType, baseName: String, parent: Option[IUserExhibitRoom]) = {
    parent foreach ensureParentAllowed
    
    val name = findRoomNewName(baseName)
    
    UserExhibitRoom(name, roomType, parent.map(_.id))
  }
  
  /** 子要素のキャッシュ */
  //
  // ノード
  def addRoom(roomType: RoomType, name: String, parent: Option[IUserExhibitRoom]) = {
    val newRoom = create(roomType, name, parent)
    allRoomList.add(newRoom)
    newRoom
  }
  
  /**
   * この名前をもつ部屋が存在するか。
   * @param name
   * @return 存在する時は {@code true} 。
   */
  def nameExists(name: String): Boolean = inTransaction {
    table.where( e => e.name === name).nonEmpty
  }
  
  /** 親を取得 */
  def getParent(element: IUserExhibitRoom) = parentFor(element.id)
  
  /** 親を設定 */
  def setParent(element: IUserExhibitRoom, parent: Option[IUserExhibitRoom]) {
    parent foreach ensureParentAllowed
    
    lookup(element.id) foreach { room =>
      room.parentId = parent.map(_.id)
      allRoomList.elementChanged(room)
    }
  }
  
  /**
   * 指定した部屋を親とするサブリストを作成。
   */
  def getFloorRoomList(parent: Option[IUserExhibitRoom]) =
    new FilterList(allRoomList, new ParentRoomMatcher(parent)).asInstanceOf[EventList[IUserExhibitRoom]]
  
  def remove(element: IUserExhibitRoom) = lookup(element.id) match {
    case Some(room) => allRoomList.remove(room)
    case None => false
  }
  
  def save(element: IUserExhibitRoom) = inTransaction {
    element match {
      case element: UserExhibitRoom => allRoomList.elementChanged(element)
      case _ =>
    }
  }
  
  /**
   * ID から親要素を取得する
   */
  private[squeryl] def parentFor(roomId: Long) = inTransaction {
    from(table)(e => where(e.id === roomId) select(e.parentId))
        .headOption.flatMap(identity).flatMap(id => lookup(id))
  }
  
  /** ID から部屋を取得 */
  private[squeryl] def lookup(roomId: Long) = allRoomList.findOrNull(roomId) match {
    case null => None
    case room => Some(room)
  }
  
  /**
   * 親として設定できる要素であるか
   */
  private def ensureParentAllowed(room: IUserExhibitRoom) = room match {
    case RoomType(GroupRoom) =>
    case _ => throw new IllegalArgumentException("parent must be a GroupRoom")
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
