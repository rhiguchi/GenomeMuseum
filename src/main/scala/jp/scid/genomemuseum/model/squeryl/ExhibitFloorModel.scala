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
class ExhibitFloorModel extends ExhibitRoomModel with IExhibitFloorModel {
  /** 部屋サービスとともに構築 */
  def this(roomService: IUserExhibitRoomService) {
    this()
    
    this.roomService = roomService
  }

  /** 部屋サービス */
  private var roomService: IUserExhibitRoomService = null
  
  
  /** {@inheritDoc} */
  def canAddRoom(target: IUserExhibitRoom) = {
    // 循環参照にならないように、祖先に子要素候補がいないか調べる
    def ancester(room: IUserExhibitRoom): Boolean = {
      roomService.getParent(room) match {
        case None => true
        case Some(`room` | `target`) => false
        case Some(parent) => ancester(parent)
      }
    }
    
    sourceRoom match {
      case Some(`target`) | None => false
      case Some(parentRoom) => ancester(parentRoom)
    }
  }
  
  /** {@inheritDoc} */
  def addRoom(element: IUserExhibitRoom) =
    roomService.setParent(element, Some(sourceRoom.get))
}
