package jp.scid.genomemuseum.model.squeryl

import ca.odell.glazedlists.EventList

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  ExhibitMuseumSpace => IExhibitMuseumSpace,
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
  def canAddRoom(room: IExhibitMuseumSpace): Boolean = freeExhibitPavilion.canSetParent(room, this)
  
  /** {@inheritDoc} */
  def addRoom(room: IExhibitMuseumSpace) =  freeExhibitPavilion.setParent(room, this)

  /**
   * この部屋を親とする部屋のリストを返す
   */
  lazy val childRoomList =
    freeExhibitPavilion.createChildRoomList(this)
      .asInstanceOf[EventList[IExhibitMuseumSpace]]
}