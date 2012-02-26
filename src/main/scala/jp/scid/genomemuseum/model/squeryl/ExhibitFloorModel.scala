package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{ExhibitFloorModel => IExhibitFloorModel,
  UserExhibitRoom => IUserExhibitRoom, UserExhibitRoomService => IUserExhibitRoomService,
  ExhibitRoomModel => IExhibitRoomModel}

/**
 * 部屋と展示物データリストのアダプター
 * 
 * @param contentList 部屋内容
 * @param contanerToExhibitFunction 部屋内容と展示物の変換関数
 */
class ExhibitFloorModel extends ExhibitRoomModel with ExhibitFloor {
  /** 部屋サービス */
  var userExhibitRoomService: MuseumExhibitContentService = null
  
  /** 部屋サービスとともに構築 */
  def this(roomService: MuseumExhibitContentService) {
    this()
    
    this.userExhibitRoomService = roomService
  }
  
  /** 部屋ソースを返す。 */
  protected def exhibitFloor = sourceRoom
}

/**
 * 階層構造を実装するミックスイン
 */
trait ExhibitFloor extends IExhibitFloorModel {
  /** 展示室サービスオブジェクト */
  protected def userExhibitRoomService: MuseumExhibitContentService
  
  /** 展示階層を返す */
  protected def exhibitFloor: Option[IUserExhibitRoom]
  
  /**
   * この階層に部屋を追加できるかを返す。
   * 
   * すでにこの階層の部屋であるとき、またはこの階層の親階層であるときは
   * 追加できない。
   */
  def canAddRoom(room: IExhibitRoomModel): Boolean = room.sourceRoom match {
    case Some(sourceRoom: IUserExhibitRoom) =>
      userExhibitRoomService.canSetParent(sourceRoom, exhibitFloor)
    case _ => false
  }
  
  /** {@inheritDoc} */
  def addRoom(room: IExhibitRoomModel) = 
    userExhibitRoomService.setParent(room.sourceRoom.get.asInstanceOf[IUserExhibitRoom], exhibitFloor)

  /**
   * この部屋を親とする部屋のリストを返す
   */
  lazy val childRoomList =
    userExhibitRoomService.createChildRoomList(exhibitFloor)
      .asInstanceOf[java.util.List[IExhibitRoomModel]]
}