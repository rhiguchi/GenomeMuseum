package jp.scid.genomemuseum.model.squeryl

import ca.odell.glazedlists.EventList

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  ExhibitMuseumFloor => IExhibitMuseumFloor, ExhibitRoomModel => IExhibitRoomModel}

/**
 * 階層構造を実装するミックスイン
 */
trait ExhibitMuseumFloor extends IExhibitMuseumFloor {
  /** 展示室サービスオブジェクト */
  protected def freeExhibitPavilion: FreeExhibitPavilion
  
  /** 展示階層を返す */
  protected def floorModel: Option[IUserExhibitRoom]
  
  /**
   * この階層に部屋を追加できるかを返す。
   * 
   * すでにこの階層の部屋であるとき、またはこの階層の親階層であるときは
   * 追加できない。
   */
  def canAddRoom(room: IExhibitRoomModel): Boolean = room.sourceRoom match {
    case Some(sourceRoom: IUserExhibitRoom) =>
      freeExhibitPavilion.canSetParent(sourceRoom, floorModel)
    case _ => false
  }
  
  /** {@inheritDoc} */
  def addRoom(room: IExhibitRoomModel) = 
    freeExhibitPavilion.setParent(room.sourceRoom.get.asInstanceOf[IUserExhibitRoom], floorModel)

  /**
   * この部屋を親とする部屋のリストを返す
   */
  lazy val childRoomList =
    freeExhibitPavilion.createChildRoomList(floorModel)
      .asInstanceOf[EventList[IExhibitRoomModel]]
}