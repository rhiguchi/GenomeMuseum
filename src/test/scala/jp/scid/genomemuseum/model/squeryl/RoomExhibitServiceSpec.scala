package jp.scid.genomemuseum.model.squeryl

import org.specs2._
import specification.Step

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom}
import IUserExhibitRoom.RoomType._

class RoomExhibitServiceSpec extends Specification with DatabaseConnectable {
  def is = "RoomExhibitService" ^ Step(openDatabase) ^
    "項目の追加" ^ canAdd ^ bt ^
    "BasicRoom の項目の取得" ^ canGetAllElementsForBasicRoom ^ bt ^
    "GroupRoom の項目の取得" ^ canGetAllElementsFromGroupRoom ^ bt ^
    "項目の削除" ^ canRemoveForBasicRoom ^ bt ^
    "親項目の削除" ^ relations ^ bt ^
    Step(closeDatabase) ^
    end
  
  def table = schema.roomExhibit
  
  val roomService = schema.userExhibitRoomService
  
  val exhibitService = schema.museumExhibitService
  
  def serviceOf(room: UserExhibitRoom) = new RoomExhibitService(table, 
    schema.userExhibitRoom, schema.museumExhibit, room)
  
  def getContentsOf(roomId: Long) = inTransaction {
    RoomExhibitService.getContentsOf(roomId, table, schema.museumExhibit)
  }
  
  class TestBase {
    protected def addRoom(roomType: RoomType = BasicRoom) =
      roomService.addRoom(roomType, "", None)
      
    protected def addExhibitTo(service: RoomExhibitService, elements: MuseumExhibit*) =
      elements foreach service.add
    
    protected def newServiceOf(roomType: RoomType) = serviceOf(addRoom(roomType))
  }
  
  def canAdd =
    "BasicRoom に追加するとテーブルに永続化される" ! addSpec.insertToTable ^
    "GroupRoom に追加しようとすると例外" ! addSpec.throwExpBy(GroupRoom) ^
    "SmartRoom に追加しようとすると例外" ! addSpec.throwExpBy(SmartRoom) ^
    "同じ項目を複数追加することはできない" ! addSpec.duplicationNotAllowed
  
  def canGetAllElementsForBasicRoom =
    "新規部屋の要素は 0 個" ! allElementSpec.emptyAtInitial ^
    "テーブルに挿入された要素を取得" ! allElementSpec.returnsElements
  
  def canGetAllElementsFromGroupRoom =
    "新規部屋の要素は 0 個" ! allElementSpec.emptyAtInitialGroupRoom ^
    "子孫の部屋の要素を取得する" ! allElementSpec.returnsDescendentElements
  
  def canRemoveForBasicRoom =
    "テーブルから削除される" ! removeSpec.deleteFromTable ^
    "この部屋には無い要素は無視される" ! removeSpec.ignore
  
  def relations =
    "Exhibit が削除されると項目も削除される" ! relationsSpec.removeExhibit ^
    "Room が削除されると項目も削除される" ! relationsSpec.removeRoom
  
  def addSpec = new TestBase {
    def insertToTable = {
      val room = addRoom()
      val exhibit = exhibitService.create()
      serviceOf(room).add(exhibit)
      
      val e = transaction {
        table.where(e => (e.roomId === room.id) and (e.exhibitId === exhibit.id)).headOption
      }
      e must beSome
    }
    
    def throwExpBy(roomType: RoomType) = {
      val exhibit = exhibitService.create()
      newServiceOf(roomType).add(exhibit) must throwA[IllegalArgumentException]
    }
    
    def duplicationNotAllowed = {
      val service = newServiceOf(BasicRoom)
      val e = exhibitService.create()
      addExhibitTo(service, e, e, e)
      getContentsOf(service.room.id) must haveSize(1)
    }
  }
  
  def allElementSpec = new TestBase {
    def emptyAtInitial = {
      newServiceOf(BasicRoom).allElements must beEmpty
    }
    
    def returnsElements = {
      val e1, e2, e3 = exhibitService.create()
      val service = newServiceOf(BasicRoom)
      addExhibitTo(service, e1, e2, e3)
      
      service.allElements must contain(e1, e2, e3).only.inOrder
    }
    
    def emptyAtInitialGroupRoom = {
      newServiceOf(GroupRoom).allElements must beEmpty
    }
    
    def returnsDescendentElements = {
      val eB1, eB2, eAA1, eAA2, eAC1, eABA1, eABA2 = exhibitService.create()
      
      val roomA = roomService.addRoom(GroupRoom, "roomA", None)
      val roomB = roomService.addRoom(BasicRoom, "roomB", None)
      addExhibitTo(serviceOf(roomB), eB1, eB2)
      
      val roomA_A = roomService.addRoom(BasicRoom, "roomA_A", Some(roomA))
      addExhibitTo(serviceOf(roomA_A), eAA1, eAA2)
      val roomA_B = roomService.addRoom(GroupRoom, "roomA_B", Some(roomA))
      val roomA_C = roomService.addRoom(BasicRoom, "roomA_C", Some(roomA))
      addExhibitTo(serviceOf(roomA_C), eAC1)
      
      val roomA_B_A = roomService.addRoom(BasicRoom, "roomA_B_A", Some(roomA_B))
      addExhibitTo(serviceOf(roomA_B_A), eABA1, eABA2)
      
      serviceOf(roomA).allElements must contain(eAA1, eAA2, eAC1, eABA1, eABA2).only
    }
  }
  
  def removeSpec = new TestBase {
    def deleteFromTable = {
      val e1, e2, e3 = exhibitService.create()
      val service = newServiceOf(BasicRoom)
      addExhibitTo(service, e1, e2, e3)
      service.remove(e2)
      
      getContentsOf(service.room.id) must contain(e1, e3).only.inOrder
    }
    
    def ignore = {
      val service = newServiceOf(BasicRoom)
      val service2 = newServiceOf(BasicRoom)
      val e1, e2, e3 = exhibitService.create()
      addExhibitTo(service2, e1, e2, e3)
      
      service.remove(e2)
      getContentsOf(service2.room.id) must contain(e1, e2, e3).only.inOrder
    }
  }
  
  def relationsSpec = new TestBase {
    def removeExhibit = {
      val e1 = exhibitService.create()
      val service = newServiceOf(BasicRoom)
      addExhibitTo(service, e1)
      exhibitService.remove(e1)
      
      getContentsOf(service.room.id) must beEmpty
    }
    
    def removeRoom = {
      val e1 = exhibitService.create()
      val service = newServiceOf(BasicRoom)
      addExhibitTo(service, e1)
      roomService.remove(service.room)
      
      getContentsOf(service.room.id) must beEmpty
    }
  }
}
