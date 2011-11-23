package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.OneToManyRelation

import jp.scid.genomemuseum.model.{RoomExhibitService => IRoomExhibitService,
  UserExhibitRoom => IUserExhibitRoom, MuseumExhibit => IMuseumExhibit}
import IUserExhibitRoom.RoomType._

/**
 * {@link jp.scid.genomemuseum.model.UserExhibitRoom} 中の
 * {@link jp.scid.genomemuseum.model.MuseumExhibit} データサービス。
 */
private[squeryl] class RoomExhibitService(val room: UserExhibitRoom,
    roomToRoomExhibit: OneToManyRelation[MuseumExhibit, RoomExhibit],
    nonPersistedExhibits: SortedSetMuseumExhibitService)
    extends MuseumExhibitService(roomToRoomExhibit.leftTable, nonPersistedExhibits)
    with IRoomExhibitService {
  
  def table = roomToRoomExhibit.rightTable
  def exhibitTable = roomToRoomExhibit.leftTable
  
  override def create = {
    val e = super.create
    nonPersistedExhibits.addRoomContent(room, e)
    e
  }
  
  override def allElements = inTransaction {
    val notPersisted = nonPersistedExhibits.roomContent(room).toList
    val contents = exhibitTable.where(e =>
      e.id in from(table)(e =>
        where(e.roomId === room.id) select(e.exhibitId)))
    .toList
    notPersisted ::: contents
  }
  
  def add(element: MuseumExhibit) = element.isPersisted match {
    case true => inTransaction {
      table.insert(RoomExhibit(room, element))
    }
    case false => nonPersistedExhibits.addRoomContent(room, element)
  }
  
  override def remove(exhibit: MuseumExhibit) = inTransaction {
    table.deleteWhere(e =>
        (e.roomId === room.id) and (e.exhibitId === exhibit.id)) match {
      case 0 => nonPersistedExhibits.removeRoomContent(room, exhibit)
      case _ => true
    }
  }
  
  override def indexOf(exhibit: MuseumExhibit) = inTransaction {
    val index = from(table)(e => where((e.roomId === room.id) and
        (e.exhibitId === exhibit.id)) select(e.id)).headOption match {
      case Some(index) =>
        from(table)(e => where(e.id lte index) compute(count)).toInt - 1
      case None => -1
    }
    index match {
      case -1 => nonPersistedExhibits.roomContent(room).indexOf(exhibit)
      case index => nonPersistedExhibits.roomContent(room).size + index
    }
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
