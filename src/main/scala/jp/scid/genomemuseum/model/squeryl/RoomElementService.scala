package jp.scid.genomemuseum.model.squeryl

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.Table

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom}
import IUserExhibitRoom.RoomType._

@deprecated("2012/02/12", "not use")
private[squeryl] object RoomElementService {
  /** 部屋の子を取得する */
  private[squeryl] def getChildren(room: UserExhibitRoom, roomTable: Table[UserExhibitRoom]) =
    roomTable.where(e => e.parentId === room.id).toList
  
  /** GroupRoom を展開し、葉要素のみを取得する */
  def getLeafs(target: UserExhibitRoom, roomTable: Table[UserExhibitRoom]) = {
    import scala.collection.mutable.{Buffer, ListBuffer}
    
    @annotation.tailrec
    def getLeafs(rooms: List[UserExhibitRoom], leafs: Buffer[UserExhibitRoom]): Buffer[UserExhibitRoom] = {
      rooms match {
        case Nil => leafs
        case head :: tail => head.roomType match {
          case GroupRoom => 
            getLeafs(getChildren(head, roomTable) ::: tail, leafs)
          case _ => getLeafs(tail, leafs += head)
        }
      }
    }
    
    getLeafs(List(target), ListBuffer.empty).toList
  }
  
  /** 部屋の要素の ID を取得 */
  def getElementIds(room: UserExhibitRoom, relationTable: Table[RoomExhibit]) =
    from(relationTable)(e =>
      where(e.roomId === room.id) select(e.exhibitId) orderBy(e.id asc)).toList
}

/**
 * 部屋の中身を取得する
 */
@deprecated("2012/02/12", "not use")
private[squeryl] trait RoomElementService {
  import RoomElementService._
  
  /**
   * 部屋の要素を取得する。
   */
  @deprecated("2012/02/12", "not use")
  def getExhibits(room: IUserExhibitRoom) = inTransaction {
    val exhibitIdList = getLeafs(room, roomTable)
      .flatMap(r => getElementIds(r, relationTable))
    
    val elements = exhibitTable.where(e => e.id in exhibitIdList).toIndexedSeq
    val elmMap = elements.view.map(e => (e.id, e)).toMap
    exhibitIdList.view.flatMap(id => elmMap.get(id)).toList
  }
  
  /**
   * 部屋に要素を追加する。
   * BasicRoom のみ追加可能
   */
  @deprecated("2012/02/12", "not use")
  def addElement(room: IUserExhibitRoom, item: MuseumExhibit) {
    if (room.roomType != BasicRoom)
      throw new IllegalArgumentException("Cannot add element to %s.".format(room))
    inTransaction {
      relationTable.insert(RoomExhibit(room, item))
    }
  }
  
  /**
   * 部屋の要素オブジェクトを取得する。
   */
  def getElementsOf(room: IUserExhibitRoom) = inTransaction {
    from(relationTable)(e => where(e.roomId === room.id) select(e) orderBy(e.id asc)).toList
  }
  
  /**
   * BasicRoom の要素を除去する。
   */
  def removeElement(element: RoomExhibit) = {
    relationTable.delete(element.id)
  }
  
  /** インターフェイスをエンティティに暗黙変換。該当するものが無ければ id 0 オブジェクトを返す */
  implicit private def toEntity(room: IUserExhibitRoom): UserExhibitRoom = room match {
    case room: UserExhibitRoom => room
    case _ => roomTable.lookup(room.id).getOrElse(UserExhibitRoom())
  }
  
  /** BasicRoom の要素を保持する関係オブジェクト */
  protected[squeryl] def exhibitRelation: OneToManyRelation[MuseumExhibit, RoomExhibit]
  /** 部屋のテーブルオブジェクト */
  protected[squeryl] def roomTable: Table[UserExhibitRoom]
  
  private[squeryl] def relationTable = exhibitRelation.rightTable
  private[squeryl] def exhibitTable = exhibitRelation.leftTable
}
