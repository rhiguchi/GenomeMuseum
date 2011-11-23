package jp.scid.genomemuseum.model.squeryl

import org.specs2._
import org.specs2.{mutable => specsmutable}
import specification.Step

import scala.collection.mutable

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.OneToManyRelation

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom}
import IUserExhibitRoom.RoomType._

class RoomExhibitServiceSpec extends Specification with SquerylConnection {
  type RoomExhibitService = Service
  def is = "RoomExhibitService" ^ //^ Step(openDatabase) ^
    "部屋に展示物が無い" ^ isEmpty(emptyService) ^ bt ^
    "部屋に展示物がある" ^ isNotEmpty(serviceWithSomeExhibits) ^ bt ^
    "展示物の追加" ^ canAdd(emptyService) ^ bt ^
    "展示物の除去" ^ canRemove(serviceWithSomeExhibits) ^ bt ^
    "要素の作成" ^ canCreate(emptyService) ^ bt ^
    "順序の取得" ^ canGetIndex(serviceWithSomeExhibits) ^ bt ^
    end
  
  def schema = new Schema
  
  def emptyService = {
    setUpSchema()
    
    val room = UserExhibitRoom("room")
    schema.userExhibitRoom.insert(room)
    
    // 無関係な要素
    0 to 5 map (i => MuseumExhibit("exhibits" + i)) foreach
      schema.museumExhibit.insert
    
    new Service(room, schema.exhibitToRoomExhibitRelation, schema.nonPersistedExhibits)
  }
  
  def serviceWithSomeExhibits = {
    val service = emptyService
    def room = service.room
    
    val exhibits = 0 to 5 map (i => MuseumExhibit("item" + i))
    exhibits foreach schema.museumExhibit.insert
    
    exhibits map (e => RoomExhibit(room, e)) foreach schema.roomExhibit.insert
    
    service
  }
  
  def isEmpty(s: => RoomExhibitService) =
    "allElements が空" ! allElementsOf(s).returnsEmpty
  
  def isNotEmpty(s: => RoomExhibitService) =
    "allElements が存在" ! allElementsOf(s).returnsNonEmpty
  
  def canRemove(s: => RoomExhibitService) =
    "項目が取得できなくなる" ! removing(s).removesFromContents ^
    "除去に成功すると true" ! removing(s).returnsTrue ^
    "テーブルから除去" ! removing(s).removesFromTable ^
    "展示物自体は除去されない" ! removing(s).notRemoveExhibit ^
    "部屋の展示物ではないときは false" ! removing(s).returnsFalseByNonItem
  
  def canAdd(s: => RoomExhibitService) =
    "項目を取得" ! adding(s).allowToGetElement ^
    "テーブルに追加" ! adding(s).insertsToTable
  
  def canCreate(s: => RoomExhibitService) =
    "allElements で取得できる" ! createOn(s).returnsByAllElements ^
    "永続化はされない" ! createOn(s).notPersist ^
    "remove で除去される" ! createOn(s).remove ^
    "要素番号を取得" ! createOn(s).indexOf
  
  def canGetIndex(s: => RoomExhibitService) =
    "存在する要素から取得" ! indexOf(s).returnsIndex ^
    "存在しない要素は -1" ! indexOf(s).notReturnByNonItem
  
  class TestBase(val service: RoomExhibitService) {
    def allElements = service.allElements
    
    def insertExhibit = {
      val exhibit = MuseumExhibit("")
      service.exhibitTable.insert(exhibit)
      exhibit
    }
  }
  
  def allElementsOf(s: RoomExhibitService) = new TestBase(s) {
    def returnsEmpty = allElements must beEmpty
    
    def returnsNonEmpty = allElements.map(_.id) must not beEmpty
  }
  
  def removing(s: RoomExhibitService) = new TestBase(s) {
    def removesFromContents = {
      val (head :: tail) = allElements
      service.remove(head)
      allElements must haveTheSameElementsAs(tail)
    }
    
    def returnsTrue = {
      val (head :: tail) = allElements
      service.remove(head) must beTrue
    }
    
    def removesFromTable = {
      val (head :: tail) = allElements
      val oldId = head.id
      service.remove(head)
      service.table.lookup(oldId) must beNone
    }
    
    def notRemoveExhibit = {
      val (head :: tail) = allElements
      val exhibitId = head.id
      service.remove(head) must beTrue
      service.exhibitTable.lookup(exhibitId) must beSome
    }
    
    def returnsFalseByNonItem = {
      val exhibit = insertExhibit
      service.remove(exhibit) must beFalse
    }
  }
  
  def adding(s: RoomExhibitService) = new TestBase(s) {
    def allowToGetElement = {
      val exhibit = insertExhibit
      service.add(exhibit)
      allElements must contain(exhibit).only
    }
    
    def insertsToTable = {
      val exhibit = insertExhibit
      service.add(exhibit)
      service.table.where(e => (e.roomId === service.room.id))
        .map(_.exhibitId) must contain(exhibit.id)
    }
  }
  
  def createOn(s: RoomExhibitService) = new TestBase(s) {
    def returnsByAllElements = {
      val e1, e2, e3 = service.create
      allElements must contain(e1, e2, e3).inOrder
    }
    
    def notPersist = {
      val e1, e2, e3 = service.create
      from(service.exhibitTable)(e => select(e)).toList must not contain(e1, e2, e3)
    }
    
    def remove = {
      val e1, e2, e3 = service.create
      s.remove(e2)
      allElements must contain(e1, e3).inOrder and
        not contain(e2)
    }
    
    def indexOf = {
      val e1, e2, e3 = service.create
      List(e1, e2, e3) map service.indexOf must contain(0, 1, 2).only.inOrder
    }
  }
  
  def indexOf(s: RoomExhibitService) = new TestBase(s) {
    def returnsIndex = {
      service.create
      val elements = allElements
      val indice = elements map service.indexOf
      indice must_== (0 until elements.size)
    }
    
    def notReturnByNonItem = {
      val exhibit = insertExhibit
      service.indexOf(exhibit) must_== -1
    }
  }
  
  class Schema extends org.squeryl.Schema {
    val userExhibitRoom = table[UserExhibitRoom]
    val museumExhibit = table[MuseumExhibit]
    val roomExhibit = table[RoomExhibit]
    val nonPersistedExhibits = new SortedSetMuseumExhibitService    
    
    /** 部屋の中身と部屋の関連 */
    val roomToRoomExhibitRelation = oneToManyRelation(userExhibitRoom, roomExhibit)
      .via((room, content) => room.id === content.roomId)
    roomToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
      
    /** 部屋の中身と展示物の関連 */
    val exhibitToRoomExhibitRelation = oneToManyRelation(museumExhibit, roomExhibit)
      .via((exhibit, content) => exhibit.id === content.exhibitId)
    exhibitToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
  }
  
  import jp.scid.genomemuseum.model.{RoomExhibitService => IRoomExhibitService}
  class Service(val room: UserExhibitRoom,
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
}
