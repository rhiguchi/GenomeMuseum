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
  private var value: EventList[_ <: IMuseumExhibit] = null
  
  def exhibitEventList = value
  
  def exhibitEventList_=(newList: EventList[_ <: IMuseumExhibit]) {
    this.value = newList
    setValue(newList)
  }
  
  override def getValue() = exhibitEventList
  
  def get(index: Int) = exhibitEventList get index
  
  var sourceRoom: Option[IUserExhibitRoom] = None
  
  private[squeryl] def roomId = sourceRoom.get.id
}
