package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{RoomExhibitService => IRoomExhibitService,
  UserExhibitRoom => IUserExhibitRoom, MuseumExhibit => IMuseumExhibit}
import IUserExhibitRoom.RoomType._

/**
 * {@link jp.scid.genomemuseum.model.UserExhibitRoom} 中の
 * {@link jp.scid.genomemuseum.model.MuseumExhibit} データサービス。
 */
private[squeryl] class RoomExhibitService(table: Table[RoomExhibit], roomTable: Table[UserExhibitRoom],
    exhibitTable: Table[MuseumExhibit], val room: IUserExhibitRoom)
    extends MuseumExhibitService(exhibitTable) with IRoomExhibitService {
  import RoomExhibitService._
  
  def add(element: IMuseumExhibit) = inTransaction {
    // BasicBox 以外は例外送出。
    if (room.roomType != BasicRoom)
        throw new IllegalArgumentException("room must be a BasicRoom")
    
    table.insert(RoomExhibit(room.id, element.id))
  }
  
  override def remove(exhibit: MuseumExhibit) = inTransaction {
    val c = table.deleteWhere(e =>
      (e.roomId === room.id) and (e.exhibitId === exhibit.id))
    c > 0
  }
  
  override def allElements: List[MuseumExhibit] = inTransaction {
    room.roomType match {
      case BasicRoom | SmartRoom => getContentsOf(room.id, table, exhibitTable)
      case GroupRoom =>
        val contents = getAllContents(room.id, table, roomTable)
        getExhibitsOf(contents, exhibitTable)
    }
  }
  
  override def indexOf(element: MuseumExhibit) = inTransaction {
    allElements.indexOf(element)
  }
  
  /**
   * 要素を新しく作成し、この部屋に追加する。
   */
  override def create() = inTransaction {
    // BasicBox 以外は例外送出。
    if (room.roomType != BasicRoom)
        throw new IllegalArgumentException("room must be a BasicRoom")
    
    val newElement = super.create()
    add(newElement)
    newElement
  }
}

private[squeryl] object RoomExhibitService {
  import collection.mutable.{Buffer, ListBuffer}
  /**
   * 指定した ID の部屋の Exhibit を取得する
   */
  def getContentsOf(roomId: Long, table: Table[RoomExhibit],
      exhibitTable: Table[MuseumExhibit]) = {
    exhibitTable.where(e =>
      e.id in from(table)(e =>
        where(e.roomId === roomId) select(e.exhibitId)))
      .toList
    }
  
  def getExhibitsOf(contents: Seq[RoomExhibit], exhibitTable: Table[MuseumExhibit]) = {
    val idSet = contents.map(_.exhibitId)
    exhibitTable.where(e => e.id in idSet).toList
  }
  
  /**
   * 指定した ID の部屋以下すべての部屋の項目を取得する
   */
  def getAllContents(roomId: Long, table: Table[RoomExhibit],
      roomTable: Table[UserExhibitRoom]): List[RoomExhibit] = {
    
    def getChildIds(roomId: Long): List[Long] =
      from(roomTable)(e => where(e.parentId === roomId) select(e.id)).toList
    
    def getContents(roomId: Long): List[RoomExhibit] =
      table.where(e => e.roomId === roomId).toList
      
    @annotation.tailrec
    def retrieveContents(roomIds: List[Long], contents: List[RoomExhibit]): List[RoomExhibit] = {
      roomIds match {
        case Nil => contents
        case roomId :: tail =>
          retrieveContents(getChildIds(roomId) ::: tail,
            contents ::: getContents(roomId))
      }
    }
    
    retrieveContents(List(roomId), Nil)
  }
}
