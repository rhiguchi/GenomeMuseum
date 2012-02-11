package jp.scid.genomemuseum.model.squeryl

import java.util.Date

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  UserExhibitRoomService => IUserExhibitRoomService, ExhibitRoom,
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
}

/**
 * GenomeMuseum データソースの Squeryl 実装
 */
private[squeryl] class UserExhibitRoomService(
  private[squeryl] val table: Table[UserExhibitRoom],
  exhibitRelation: OneToManyRelation[MuseumExhibit, RoomExhibit]
) extends IUserExhibitRoomService with UserExhibitRoomPublisher {
  type Node = UserExhibitRoom
  
  def userExhibitRoomTablePublisher = SquerylTriggerAdapter.connect(table, 2)
  
  /**
   * ローカルライブラリ用の部屋を返す
   * @param 名前などのプロキシ接続用ノード
   */
  def localLibraryExhibitRoom(libraryNode: ExhibitRoom) =
    new LocalLibraryExhibitRoom(libraryNode, table, exhibitRelation.leftTable)
  
  /**
   * 部屋をコンテンツ付きに変換する。
   */
  protected[squeryl] def roomContents(room: UserExhibitRoom) = room.roomType match {
    case BasicRoom =>
      new UserExhibitBasicRoomContentsProxy(room, exhibitTable, relationTable)
    case GroupRoom =>
      new UserExhibitGroupRoomContentsProxy(room, exhibitTable, relationTable, table)
    case SmartRoom =>
      new UserExhibitSmartRoomContentsProxy(room, exhibitTable, relationTable, table)
  }
  
  def addRoom(roomType: RoomType, name: String, parent: Option[IUserExhibitRoom]) = {
    val parentIdOp = parent flatMap {
      case RoomType(GroupRoom) => parent
      case elm => getParent(elm)
    } map {p => p.id}
    
    val newRoom = UserExhibitRoom(name, roomType, parentIdOp)
    
    inTransaction {
      table.insert(newRoom)
    }
    
    roomContents(newRoom)
  }
  
  def nameExists(name: String): Boolean = inTransaction {
    table.where( e => e.name === name).nonEmpty
  }
  
  def getParent(element: IUserExhibitRoom) = {
    parentFor(element.id) map roomContents
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
  
  def getChildren(parent: Option[IUserExhibitRoom]) = {
    val rooms = inTransaction {
      table.where(e => nvl(e.parentId, 0L) === parent.map(_.id).getOrElse(0L)).toList
    }
    rooms map roomContents
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
  
  def getContents(room: Option[IUserExhibitRoom]) = null // room match {
//    case Some(RoomType(BasicRoom)) | None =>
//      new MutableMuseumExhibitListModel(exhibitRelation, table, room)
//    case _ => new MuseumExhibitListModel(exhibitRelation, table, room)
//  }
  
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
  
  private def relationTable = exhibitRelation.rightTable
  private def exhibitTable = exhibitRelation.leftTable
}


/**
 * 全ローカルファイル所有クラス
 */
class LocalLibraryExhibitRoom(
  room: ExhibitRoom,
  roomTable: Table[UserExhibitRoom],
  exhibitTable: Table[MuseumExhibit]
) extends ExhibitRoom with GroupRoomContentsModel with IMutableMuseumExhibitListModel {
  import UserExhibitRoomService.getParentId
  
  // プロキシメソッド
  /** {@inheritDoc} */
  def name: String = room.name
  
  // RoomContentExhibits 実装
  /** 展示物のもとの部屋 */
  def userExhibitRoom = None
  
  /** 展示物 */
  def exhibitList = inTransaction {
    from(exhibitTable)( e => select(e) orderBy(e.id asc)).toList
  }
  
  /**
   * 親IDが存在する部屋は {@code true} 。
   */
  def canAddChild(target: IUserExhibitRoom) = {
    getParentId(target.id, roomTable).nonEmpty
  }
  
  /**
   * 親IDを除去する
   */
  def addChild(element: IUserExhibitRoom) = inTransaction {
    update(roomTable) ( e =>
      where(e.id === element.id)
      set(e.parentId := None)
    )
  }
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: IMuseumExhibit): Boolean = inTransaction {
    exhibitTable.delete(element.id)
  }
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素がまだサービスに永続化されていない時は、永続化される。
   * 要素がこのサービスに存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def add(element: IMuseumExhibit) = element match {
    case exhibit: MuseumExhibit if !exhibit.isPersisted => inTransaction {
      exhibitTable.insert(exhibit)
      true
    }
    case _ => false
  }
}

