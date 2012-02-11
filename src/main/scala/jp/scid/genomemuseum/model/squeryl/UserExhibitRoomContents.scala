package jp.scid.genomemuseum.model.squeryl

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.Table

import jp.scid.genomemuseum.model.{MuseumExhibitListModel => IMuseumExhibitListModel,
  MuseumExhibit => IMuseumExhibit, UserExhibitRoom => IUserExhibitRoom, ExhibitRoom,
  GroupRoomContentsModel, MutableMuseumExhibitListModel => IMutableMuseumExhibitListModel}

/**
 * UserExhibitRoom の展示物を取得するプロクシクラス
 */
abstract class UserExhibitRoomContents(room: IUserExhibitRoom)
    extends IUserExhibitRoom with IMuseumExhibitListModel {
  // プロキシメソッド
  /** {@inheritDoc} */
  override def id = room.id
  
  /** {@inheritDoc} */
  override def roomType = room.roomType
  
  /** {@inheritDoc} */
  override def name: String = room.name
  
  /** {@inheritDoc} */
  override def name_=(newName: String) = room.name = newName
  
  /** もとの部屋 */
  val userExhibitRoom = Some(room)
}

/**
 * ツリーノードコンテンツ用プロキシクラス
 */
class UserExhibitBasicRoomContentsProxy(
  room: UserExhibitRoom,
  exhibitTable: Table[MuseumExhibit],
  relationTable: Table[RoomExhibit]
) extends UserExhibitRoomContents(room) with IMutableMuseumExhibitListModel {
  /** 展示物 */
  def exhibitList = inTransaction {
    val exhibitIdList = UserExhibitRoomService.getElementIds(id, relationTable)
    
    val elements = exhibitTable.where(e => e.id in exhibitIdList).toIndexedSeq
    val elmMap = elements.view.map(e => (e.id, e)).toMap
    exhibitIdList.view.flatMap(id => elmMap.get(id)).toList
  }
  
  /**
   * 要素を追加する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def add(element: IMuseumExhibit) = inTransaction {
    exhibitTable.lookup(element.id) match {
      case Some(exhibit) =>
        relationTable.insert(RoomExhibit(room, exhibit))
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
  def remove(element: IMuseumExhibit): Boolean = {
    val relations = relationTable.where(e => e.exhibitId === element.id).toList
    relations.headOption map { relation =>
      relationTable delete relation.id
    } getOrElse false
  }
}

/**
 * GroupRoom コンテンツ用プロキシクラス
 */
class UserExhibitGroupRoomContentsProxy(
  room: UserExhibitRoom,
  exhibitTable: Table[MuseumExhibit],
  relationTable: Table[RoomExhibit],
  roomTable: Table[UserExhibitRoom]
) extends UserExhibitRoomContents(room) with GroupRoomContentsModel {
  import UserExhibitRoomService.getParentId
  
  /**
   * 子要素すべての展示物を取得する。
   */
  def exhibitList = inTransaction {
    val exhibitIdList = UserExhibitRoomService.getLeafs(id, roomTable)
      .flatMap(r => UserExhibitRoomService.getElementIds(r.id, relationTable))
    
    val elements = exhibitTable.where(e => e.id in exhibitIdList).toIndexedSeq
    val elmMap = elements.view.map(e => (e.id, e)).toMap
    exhibitIdList.view.flatMap(id => elmMap.get(id)).toList
  }

  /** {@inheritDoc} */
  def canAddChild(target: IUserExhibitRoom) = {
    val thisRoomId = this.room.id
    val targetId = target.id
    
    // 循環参照にならないように、祖先に子要素候補がいないか調べる
    def ancester(myId: Long): Boolean = {
      getParentId(myId, roomTable) match {
        case None => true
        case Some(`thisRoomId` | `targetId`) => false
        case Some(parentId) => ancester(parentId)
      }
    }
    
    ancester(thisRoomId)
  }
  
  /** {@inheritDoc} */
  def addChild(element: IUserExhibitRoom) = inTransaction {
    update(roomTable) ( e =>
      where(e.id === element.id)
      set(e.parentId := Some(id))
    )
  }  
}

/**
 * SmartRoom コンテンツ用プロキシクラス
 */
class UserExhibitSmartRoomContentsProxy(
  room: UserExhibitRoom,
  exhibitTable: Table[MuseumExhibit],
  relationTable: Table[RoomExhibit],
  roomTable: Table[UserExhibitRoom]
) extends UserExhibitRoomContents(room) with IMuseumExhibitListModel {
  /** 展示物 */
  def exhibitList = inTransaction {
    // TODO implement
    Nil
  }
}

