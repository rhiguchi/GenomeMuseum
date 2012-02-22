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
class ExhibitRoomModel(
    room: IUserExhibitRoom,
    contentList: EventList[RoomExhibit],
    contanerToExhibitFunction: Function[RoomExhibit, MuseumExhibit])
    extends IExhibitRoomModel {
  /** transform */
  private val exhibitEventList = new FunctionList(contentList, contanerToExhibitFunction)
  
  def getValue() = exhibitEventList.asInstanceOf[EventList[IMuseumExhibit]]
  
  def get(index: Int) = exhibitEventList get index
  
  def sourceRoom = room
  
  private[squeryl] def roomId = room.id
}

/** クエリから中身リストを作成 */
class QueryContentList(table: Table[RoomExhibit])
    extends KeyedEntityEventList(table) {
}
