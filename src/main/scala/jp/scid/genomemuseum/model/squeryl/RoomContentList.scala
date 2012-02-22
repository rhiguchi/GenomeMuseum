package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{GlazedLists, CollectionList, FunctionList,
  BasicEventList, EventList}

import jp.scid.genomemuseum.model.{FreeExhibitRoomModel => IFreeExhibitRoomModel,
  MuseumExhibit => IMuseumExhibit, ExhibitFloorModel => IExhibitFloorModel,
  UserExhibitRoom => IUserExhibitRoom}

/**
 * 自由に展示物を入れ替えできる部屋のモデル
 * 
 * @param roomId 部屋の ID
 * @param table 部屋内容テーブル
 */
class FreeExhibitRoomModel extends ExhibitRoomModel with IFreeExhibitRoomModel {
  /** 部屋の中身 */
  var contentList: EventList[RoomExhibit] = null

  /** 部屋の中身を指定して初期化 */
  def this(contentList: EventList[RoomExhibit]) {
    this()
    this.contentList = contentList
  }
  
  /**
   * 要素を追加する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def addContent(element: IMuseumExhibit) =
    contentList.add(RoomExhibit(roomId, element.id))
  
  /**
   * インデックスの展示物を置換する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def setContent(index: Int, element: IMuseumExhibit) = {
    val oldExhibit = get(index)
    val content = contentList.get(index)
    content.exhibitId = element.id
    contentList.set(index, content)
    oldExhibit
  }
  
  /**
   * 指定項目番目の要素を除去する。
   */
  def removeContent(index: Int) = {
    val removed = get(index)
    contentList.remove(index)
    removed
  }
}

/**
 * 部屋と展示物データリストのアダプター
 * 
 * @param contentList 部屋内容
 * @param contanerToExhibitFunction 部屋内容と展示物の変換関数
 */
class ExhibitFloorModel extends ExhibitRoomModel with IExhibitFloorModel {
  /** 部屋サービスとともに構築 */
  def this(roomService: UserExhibitRoomService) {
    this()
    
    this.roomService = roomService
  }

  /** 部屋サービス */
  private var roomService: UserExhibitRoomService = null
  
  
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
