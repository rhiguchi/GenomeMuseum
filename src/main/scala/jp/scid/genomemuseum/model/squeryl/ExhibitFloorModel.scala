package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{ExhibitFloorModel => IExhibitFloorModel,
  UserExhibitRoom => IUserExhibitRoom, UserExhibitRoomService => IUserExhibitRoomService}

/**
 * 部屋と展示物データリストのアダプター
 * 
 * @param contentList 部屋内容
 * @param contanerToExhibitFunction 部屋内容と展示物の変換関数
 */
class ExhibitFloorModel extends ExhibitRoomModel with ExhibitFloor {
  /** 部屋サービス */
  var roomService: IUserExhibitRoomService = null
  
  /** 部屋サービスとともに構築 */
  def this(roomService: IUserExhibitRoomService) {
    this()
    
    this.roomService = roomService
  }
  
  /** 部屋ソースを返す。 */
  protected def exhibitFloor = sourceRoom
}

/**
 * 階層構造を実装するミックスイン
 */
trait ExhibitFloor extends IExhibitFloorModel {
  /** 展示室サービスオブジェクト */
  protected def roomService: IUserExhibitRoomService
  
  /** 展示階層を返す */
  protected def exhibitFloor: Option[IUserExhibitRoom]
  
  /**
   * この階層に部屋を追加できるかを返す。
   * 
   * すでにこの階層の部屋であるとき、またはこの階層の親階層であるときは
   * 追加できない。
   */
  def canAddRoom(target: IUserExhibitRoom) = {
    // 循環参照にならないように、祖先に子要素候補がいないか調べる
    def ancester(room: IUserExhibitRoom): Boolean = {
      roomService.getParent(room) match {
        case None => true
        case Some(`room` | `target`) => false
        case Some(parent) => ancester(parent)
      }
    }
    
    exhibitFloor match {
      case None => roomService.getParent(target).nonEmpty
      case Some(`target`) => false
      case Some(parentRoom) => ancester(parentRoom)
    }
  }
  
  /** {@inheritDoc} */
  def addRoom(element: IUserExhibitRoom) =
    roomService.setParent(element, exhibitFloor)

  /**
   * この部屋を親とする部屋のリストを返す
   */
  def childRoomList = roomService.getChildren(exhibitFloor)
}