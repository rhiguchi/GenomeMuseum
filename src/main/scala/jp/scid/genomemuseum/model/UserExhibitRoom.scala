package jp.scid.genomemuseum.model

object UserExhibitRoom {
  /**
   * 部屋の種類を示す序列。
   */
  object RoomType extends Enumeration {
    type RoomType = Value
    /** ユーザーが中身を設定できる『部屋』を示す */
    val BasicRoom = Value(1)
    /** 『部屋』をまとめる『部屋』 */
    val GroupRoom = Value(2)
    /** 条件を設定することで自動で中身が定まる『部屋』 */
    val SmartRoom = Value(3)
    
    def unapply(room: UserExhibitRoom): Option[Value] = {
      Some(room.roomType)
    }
  }
  
  def unapply(room: ExhibitRoom): Option[(UserExhibitRoom)] = room match {
    case room: UserExhibitRoom => Some(room)
    case _ => None
  }
}

import UserExhibitRoom.RoomType._

/**
 * ユーザーが中身である MuseumExhibits を設定できる『部屋』インターフェイス。
 * 『部屋』の種類は 3 つある。
 */
trait UserExhibitRoom extends ExhibitRoom {
  /** 部屋の識別子 */
  def id: Long
  
  /** 部屋の種類 */
  def roomType: UserExhibitRoom.RoomType.Value
  
  /** この部屋の表示名を設定する */
  def name_=(newName: String)
  
  /** この部屋が保持する展示物を返す */
  @deprecated("", "")
  private[model] def exhibitListModel(implicit roomService: UserExhibitRoomService) = roomType match {
    case BasicRoom => new UserExhibitBasicRoom(this, roomService)
    case GroupRoom => new UserExhibitGroupRoom(this, roomService)
    case SmartRoom => new UserExhibitSmartRoom(this, roomService)
  }
}

// Classes for MuseumStructure
@deprecated("", "")
abstract class UserExhibitRoomContents(room: UserExhibitRoom) extends MuseumExhibitListModel {
  /** もとの部屋 */
  def userExhibitRoom = Some(room)
}

/**
 * ツリーノードコンテンツ用プロキシクラス
 */
@deprecated("", "")
class UserExhibitBasicRoom(
  basicRoom: UserExhibitRoom,
  contentsService: ExhibitRoomContentsService
) extends UserExhibitRoomContents(basicRoom) with MutableMuseumExhibitListModel {
  // BasicRoom のみ許可
  require(basicRoom.roomType == BasicRoom)
  
  /** 展示物 */
  def exhibitList: List[MuseumExhibit] = contentsService.getExhibitList(basicRoom)
  
  /**
   * 要素を追加する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def add(element: MuseumExhibit) = contentsService.addExhibit(basicRoom, element)
  /**
   * 要素を除去する。
   * 
   * 要素の ID が存在するとき、この部屋から除去される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def remove(element: MuseumExhibit) = contentsService.removeExhibit(basicRoom, element)
}

/**
 * GroupRoom コンテンツ用プロキシクラス
 */
@deprecated("", "")
class UserExhibitGroupRoom(
  groupRoom: UserExhibitRoom,
  roomService: UserExhibitRoomService
) extends UserExhibitRoomContents(groupRoom) with GroupRoomContentsModel {
  // GroupRoom のみ許可
  require(groupRoom.roomType == GroupRoom)
  
  /**
   * 子要素すべての展示物を取得する。
   */
  def exhibitList = roomService.getExhibitList(groupRoom)

  /** {@inheritDoc} */
  def canAddChild(target: UserExhibitRoom) = {
    // 循環参照にならないように、祖先に子要素候補がいないか調べる
    def ancester(room: UserExhibitRoom): Boolean = {
      roomService.getParent(room) match {
        case None => true
        case Some(`groupRoom` | `room`) => false
        case Some(parent) => ancester(parent)
      }
    }
    
    groupRoom == target match {
      case true => false
      case false => ancester(groupRoom)
    }
  }
  
  /** {@inheritDoc} */
  def addChild(element: UserExhibitRoom) =
    roomService.setParent(element, Some(groupRoom))
}


/**
 * SmartRoom コンテンツ用プロキシクラス
 */
@deprecated("", "")
class UserExhibitSmartRoom(
  smartRoom: UserExhibitRoom,
  contentsService: ExhibitRoomContentsService
) extends UserExhibitRoomContents(smartRoom) with MuseumExhibitListModel {
  // SmartRoom のみ許可
  require(smartRoom.roomType == SmartRoom)
  
  /** 展示物 */
  def exhibitList = contentsService.getExhibitList(smartRoom)
}

