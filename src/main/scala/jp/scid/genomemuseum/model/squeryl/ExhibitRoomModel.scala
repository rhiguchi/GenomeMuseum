package jp.scid.genomemuseum.model.squeryl

import ca.odell.glazedlists.EventList

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit,
  UserExhibitRoom => IUserExhibitRoom, ExhibitRoomModel => IExhibitRoomModel}

/**
 * 部屋と展示物データリストのアダプター
 * @param contentList 部屋の内容データリスト
 * @param contanerToExhibitFunction 部屋の内容から展示物を取得する関数
 */
class ExhibitRoomModel extends IExhibitRoomModel {
  /** 現在の値（展示物リスト） */
  private var value: Option[EventList[IMuseumExhibit]] = None
  
  def this(exhibitList: EventList[IMuseumExhibit]) {
    this()
    exhibitEventList = Option(exhibitList)
  }
  
  def exhibitEventList = value
  
  def exhibitEventList_=(newList: Option[EventList[IMuseumExhibit]]) {
    this.value = newList
    firePropertyChange("value", null, getValue)
  }
  
  override def getValue() = exhibitEventList.getOrElse(null)
  
  var sourceRoom: Option[IUserExhibitRoom] = None
  
  def name = sourceRoom.map(_.name).getOrElse("NoName")
  
  def dispose() {
    value.foreach(_.dispose())
  }
}
