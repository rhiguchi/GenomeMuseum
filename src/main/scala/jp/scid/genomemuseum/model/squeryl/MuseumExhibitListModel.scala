package jp.scid.genomemuseum.model.squeryl

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.Table

import jp.scid.genomemuseum.model.{MuseumExhibitListModel => IMuseumExhibitListModel,
  MuseumExhibit => IMuseumExhibit, UserExhibitRoom => IUserExhibitRoom}

class MuseumExhibitListModel(
    exhibitRelation: OneToManyRelation[MuseumExhibit, RoomExhibit],
    roomTable: Table[UserExhibitRoom],
    room: Option[IUserExhibitRoom]
) extends IMuseumExhibitListModel {
  
  override def getRoom = room
  
  override def getValue() = {
    import collection.JavaConverters._
    
    val elements: List[IMuseumExhibit] = getRoom match {
      case Some(room) => getExhibits(room)
      case _ => allElements
    }
    
    elements.asJava
  }
  
  override def setValue(ehixibiList: java.util.List[IMuseumExhibit]) {
    // TODO implement
  }
  
  private[squeryl] def allElements = inTransaction {
    from(exhibitTable)( e => select(e) orderBy(e.id asc)).toList
  }
  
  implicit private[squeryl] def toEntity(room: IUserExhibitRoom): UserExhibitRoom = room match {
    case room: UserExhibitRoom => room
    case _ => roomTable.lookup(room.id).getOrElse(UserExhibitRoom())
  }
  
  def getExhibits(room: IUserExhibitRoom) = inTransaction {
    val exhibitIdList = RoomElementService.getLeafs(room, roomTable)
      .flatMap(r => RoomElementService.getElementIds(r, relationTable))
    
    val elements = exhibitTable.where(e => e.id in exhibitIdList).toIndexedSeq
    val elmMap = elements.view.map(e => (e.id, e)).toMap
    exhibitIdList.view.flatMap(id => elmMap.get(id)).toList
  }
  
  private[squeryl] def relationTable = exhibitRelation.rightTable
  private[squeryl] def exhibitTable = exhibitRelation.leftTable
}