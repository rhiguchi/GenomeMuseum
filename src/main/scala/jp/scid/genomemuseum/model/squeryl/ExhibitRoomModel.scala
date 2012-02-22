package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{EventList, FunctionList}
import ca.odell.glazedlists.FunctionList.Function

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit,
  UserExhibitRoom => IUserExhibitRoom, ExhibitRoomModel => IExhibitRoomModel}

object ExhibitRoomModel {
  /**
   * 展示物を取得する関数
   */
  class ContanerToExhibitFunction(table: Table[MuseumExhibit])
      extends FunctionList.Function[RoomExhibit, MuseumExhibit] {
    def evaluate(container: RoomExhibit) = inTransaction {
      table.lookup(container.exhibitId).get
    }
  }
}

/**
 * 部屋と展示物データリストのアダプター
 * @param contentList 部屋の内容データリスト
 * @param contanerToExhibitFunction 部屋の内容から展示物を取得する関数
 */
class ExhibitRoomModel extends IExhibitRoomModel {
  def this(exhibitList: EventList[IMuseumExhibit]) {
    this()
  }
  private var value: EventList[_ <: IMuseumExhibit] = null
  
  def exhibitEventList = value
  
  def exhibitEventList_=(newList: EventList[_ <: IMuseumExhibit]) {
    this.value = newList
    setValue(newList.asInstanceOf[java.util.List[IMuseumExhibit]])
  }
  
  def getValue() = exhibitEventList.asInstanceOf[EventList[IMuseumExhibit]]
  
  override def setValue(newExhibitList: java.util.List[IMuseumExhibit]) {
    firePropertyChange("value", null, newExhibitList)
  }
  
  def get(index: Int) = exhibitEventList get index
  
  var sourceRoom: Option[IUserExhibitRoom] = None
  
  private[squeryl] def roomId = sourceRoom.get.id
}
