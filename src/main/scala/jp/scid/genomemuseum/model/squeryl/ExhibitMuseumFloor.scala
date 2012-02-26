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
  
  /**
   * この階層に部屋を追加できるかを返す。
   * 
   * すでにこの階層の部屋であるとき、またはこの階層の親階層であるときは
   * 追加できない。
   */
  def canAddRoom(room: IExhibitRoomModel): Boolean = room.sourceRoom match {
    case Some(sourceRoom: IUserExhibitRoom) =>
      freeExhibitPavilion.canSetParent(sourceRoom, roomModel)
    case _ => false
  }
  
  /** {@inheritDoc} */
  def addRoom(room: IExhibitRoomModel) = 
    freeExhibitPavilion.setParent(room.sourceRoom.get.asInstanceOf[IUserExhibitRoom], roomModel)

  /**
   * この部屋を親とする部屋のリストを返す
   */
  lazy val childRoomList =
    freeExhibitPavilion.createChildRoomList(roomModel)
      .asInstanceOf[EventList[IExhibitRoomModel]]
  
  /**
   * 名前を設定する
   */
  def name_=(newName: String) = roomModel.foreach(_.name = newName)
}