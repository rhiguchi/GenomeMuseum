package jp.scid.genomemuseum.model.squeryl

import org.specs2._

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.OneToManyRelation
import org.squeryl.Table

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom}
import IUserExhibitRoom.RoomType._
import SquerylConnection._

object RoomElementServiceSpec {
  import org.squeryl.Schema
  
  private[squeryl] class TestMuseumSchema extends Schema {
    private val schemaName = "RoomElementServiceSpec" + util.Random.alphanumeric.take(5).mkString
    override def name = Some(schemaName)
    
    val museumExhibit = table[MuseumExhibit]
    val userExhibitRoom = table[UserExhibitRoom]
    val roomExhibit = table[RoomExhibit]
  
    val roomRelation = oneToManyRelation(userExhibitRoom, roomExhibit)
      .via((room, content) => room.id === content.roomId)
    roomRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
    
    val exhibitRelation = oneToManyRelation(museumExhibit, roomExhibit)
      .via((exhibit, content) => exhibit.id === content.exhibitId)
    exhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
    /** 親子関係 */
    val roomTree = oneToManyRelation(userExhibitRoom, userExhibitRoom)
        .via((c, p) => c.parentId === p.id)
    roomTree.foreignKeyDeclaration.constrainReference(onDelete cascade)
  }
}

class RoomElementServiceSpec extends Specification {
  import RoomElementServiceSpec._
  
  def is = "RoomElementService" ^
    "getChildren メソッド" ^ getChildrenSpec(emptySchema.userExhibitRoom) ^ bt ^
    "getLeafs メソッド" ^ getLeafsSpec(emptySchema.userExhibitRoom) ^ bt ^
    "空の部屋" ^ emptyRoomSpec(simpleService) ^ bt ^
    "要素を持つ BasicRoom" ^ basicRoomServiceSpec(simpleService) ^ bt ^
    "子を持つ GroupRoom" ^ groupRoomSpec(simpleService) ^ bt ^
    "部屋に要素を追加" ^ addElementSpec(simpleService) ^ bt ^
    "部屋の要素を取得" ^ getElementsOfSpec(simpleService) ^ bt ^
    "部屋の要素を削除" ^ removeElementSpec(simpleService) ^ bt ^
    end
  
  private class SimpleRoomElementService(
      val roomTable: Table[UserExhibitRoom],
      val exhibitRelation: OneToManyRelation[MuseumExhibit, RoomExhibit])
      extends RoomElementService
  
  def emptySchema = {
    val schema = new TestMuseumSchema
    setUpSchema(schema)
    schema
  }
  
  def simpleService: RoomElementService = {
    val schema = emptySchema
    
    val room = UserExhibitRoom("room", BasicRoom)
    schema.userExhibitRoom insert room
    
    new SimpleRoomElementService(schema.userExhibitRoom, schema.exhibitRelation)
  }
  
  def getElements(count: Int, table: Table[MuseumExhibit]) =
    1 to count map (i => MuseumExhibit("item" + i)) map table.insert
  
  def addElementTo(room: UserExhibitRoom, elements: Seq[MuseumExhibit], table: Table[RoomExhibit]) =
    elements.map(e => RoomExhibit(room, e)).foreach(table.insert)
  
  def getChildrenSpec(roomTable: => Table[UserExhibitRoom]) =
    "子の無い要素は空配列を返す" ! getChildren(roomTable).returnsEmtpy ^
    "子がある要素は子要素配列を返す" ! getChildren(roomTable).returnsChildren
  
  def getLeafsSpec(roomTable: => Table[UserExhibitRoom]) =
    "子の無い GroupRoom は空配列を返す" ! getLeafsOn(roomTable).s1 ^
    "BasicRoom は自身を返す" ! getLeafsOn(roomTable).s2 ^
    "SmartRoom は自身を返す" ! getLeafsOn(roomTable).s3 ^
    "子ノードを返す" ! getLeafsOn(roomTable).s4 ^
    "子孫ノードを返す" ! getLeafsOn(roomTable).s5
  
  def emptyRoomSpec(s: => RoomElementService) =
    "BasicRoom は空配列を返す" ! getExhibits(s).returnsEmptyFromEmptyBasicRoom ^
    "GroupRoom は空配列を返す" ! getExhibits(s).returnsEmptyFromEmptyGroupRoom ^
    "SmartRoom は空配列を返す" ! getExhibits(s).returnsEmptyFromEmptySmartRoom
  
  def basicRoomServiceSpec(s: => RoomElementService) =
    "要素を返す" ! basicRoom(s).returnsSomeElement ^
    "部屋の要素を返す" ! basicRoom(s).returnsRoomElement
  
  def groupRoomSpec(s: => RoomElementService) =
    "子の部屋の要素を" ! groupRoom(s).returnsElementsOfChildren ^
    "子孫階層の要素を返す" ! groupRoom(s).returnsDeepDescendant
  
  def addElementSpec(s: => RoomElementService) =
    "項目が取得できる" ! addElement(s).cangetExhibits ^
    "テーブルに追加" ! addElement(s).insertsToTable ^
    "GroupRoom には追加できない" ! addElement(s).cannotAddToGroupRoom ^
    "SmartRoom には追加できない" ! addElement(s).cannotAddToSmartRoom
  
  def getElementsOfSpec(s: => RoomElementService) =
    "項目の要素を取得" ! getElementsOf(s).returnsElements ^
    "空の部屋からは要素を返さない" ! getElementsOf(s).returnsEmptyFromEmptyRoom
  
  def removeElementSpec(s: => RoomElementService) =
    "項目が取得できなくなる" ! removeElement(s).removesItem ^
    "要素が取得できなくなる" ! removeElement(s).removesElement ^
    "テーブルから除去" ! removeElement(s).removesFromTable ^
    "除去されると true" ! removeElement(s).returnsTrueToSuccess ^
    "存在しない時は false" ! removeElement(s).returnsFalstToFail
  
  def insertRooms(roomTable: Table[UserExhibitRoom], roomType: RoomType, count: Int) =
    0 to count map (i => UserExhibitRoom("item" + i, roomType)) map roomTable.insert
  
  def getChildren(roomTable: Table[UserExhibitRoom]) = new Object {
    import RoomElementService.{getChildren => function}
    
    val rooms = insertRooms(roomTable, GroupRoom, 2)
    val target = rooms(2)
    
    def returnsEmtpy = function(target, roomTable) must beEmpty
    
    def returnsChildren = {
      val parent = Some(target.id)
      val children = List(UserExhibitRoom("child1", SmartRoom, parent),
        UserExhibitRoom("child2", GroupRoom, parent),
        UserExhibitRoom("child3", BasicRoom, parent)) map roomTable.insert
      
      insertRooms(roomTable, BasicRoom, 2)
      
      function(target, roomTable) must haveTheSameElementsAs(children)
    }
  }
  
  def getLeafsOn(roomTable: Table[UserExhibitRoom]) = new Object {
    import RoomElementService.{getLeafs => function}
    
    def createTarget(roomType: RoomType) = {
      val parent = roomTable insert UserExhibitRoom("parent", GroupRoom)
      roomTable insert UserExhibitRoom("target", roomType, Some(parent.id))
    }
    
    def s1 = function(createTarget(GroupRoom), roomTable) must beEmpty
    
    def s2 = {
      val target = createTarget(BasicRoom)
      function(target, roomTable) must contain(target).only
    }
    
    def s3 = {
      val target = createTarget(SmartRoom)
      function(target, roomTable) must contain(target).only
    }
    
    def s4 = {
      val target = createTarget(GroupRoom)
      val children = List(BasicRoom, GroupRoom, GroupRoom, SmartRoom, GroupRoom, BasicRoom) map
        (t => UserExhibitRoom("item", t, Some(target.id))) map roomTable.insert filter
          (_.roomType != GroupRoom)
      
      function(target, roomTable) must haveTheSameElementsAs(children)
    }
    
    def s5 = {
      val target = createTarget(GroupRoom)
      val c1 = roomTable insert UserExhibitRoom("c1", BasicRoom, Some(target.id))
      val c2 = roomTable insert UserExhibitRoom("c2", GroupRoom, Some(target.id))
      val c2_1 = roomTable insert UserExhibitRoom("c2_1", GroupRoom, Some(c2.id))
      val c2_1_1 = roomTable insert UserExhibitRoom("c2_1_1", GroupRoom, Some(c2_1.id))
      val c2_1_2 = roomTable insert UserExhibitRoom("c2_1_2", SmartRoom, Some(c2_1.id))
      val c2_2 = roomTable insert UserExhibitRoom("c2_1", BasicRoom, Some(c2.id))
      
      function(target, roomTable) must contain(c1, c2_1_2, c2_2).only.inOrder
    }
  }
  
  abstract class TestBase(val service: RoomElementService) {
    def roomOf(roomType: RoomType) =
      service.roomTable insert UserExhibitRoom("room", roomType)
    
    def elementOf(name: String = "item") = service.exhibitTable insert MuseumExhibit(name)
  }
  
  def getExhibits(s: RoomElementService) = new TestBase(s) {
    def returnsEmptyFromEmptyBasicRoom =
      service.getExhibits(roomOf(BasicRoom)) must beEmpty
    
    def returnsEmptyFromEmptyGroupRoom =
      service.getExhibits(roomOf(GroupRoom)) must beEmpty
    
    def returnsEmptyFromEmptySmartRoom =
      service.getExhibits(roomOf(SmartRoom)) must beEmpty
  }
  
  def basicRoom(s: RoomElementService) = new TestBase(s) {
    import RoomElementService._
    
    val room = roomOf(BasicRoom)
    val elements = getElements(3, service.exhibitTable)
    addElementTo(room, elements, service.relationTable)
    addElementTo(room, elements, service.relationTable)
    
    def returnsSomeElement = service.getExhibits(room) must not beEmpty
    
    def returnsRoomElement = {
      val exhibitIdList = getElementIds(room, service.relationTable)
      
      service.getExhibits(room).map(_.id) must haveTheSameElementsAs(exhibitIdList)
    }
  }
  
  def groupRoom(s: RoomElementService) = new TestBase(s) {
    import RoomElementService._
    
    val room = roomOf(GroupRoom)
    
    def createChildFor(parent: UserExhibitRoom) = {
      val child = service.roomTable insert UserExhibitRoom("room", BasicRoom, Some(parent.id))
      val elements = getElements(3, service.exhibitTable)
      addElementTo(child, elements, service.relationTable)
      addElementTo(child, elements, service.relationTable)
      child
    }
    
    def returnsElementsOfChildren = {
      val child = createChildFor(room)
      val exhibitIdList = getElementIds(child, service.relationTable)
      
      service.getExhibits(room).map(_.id) must haveTheSameElementsAs(exhibitIdList)
    }
    
    def returnsDeepDescendant = {
      val descendant = (1 to 100).foldLeft(room) { (parent, index) =>
        val child = UserExhibitRoom("room" + index, GroupRoom, Some(parent.id))
        service.roomTable.insert(child)
        child
      }
      
      val child1, child2 = createChildFor(room)
      val exhibitIdList = getElementIds(child1, service.relationTable) :::
        getElementIds(child2, service.relationTable)
      
      service.getExhibits(room).map(_.id) must haveTheSameElementsAs(exhibitIdList)
    }
  }
  
  def addElement(s: RoomElementService) = new TestBase(s) {
    val e1, e2 = elementOf("item1")
    
    def cangetExhibits = {
      val room = roomOf(BasicRoom)
      List(e1, e2, e1) foreach (e => service.addElement(room, e))
      
      service.getExhibits(room) must contain(e1, e2, e1).only.inOrder
    }
    
    def insertsToTable = {
      val room = roomOf(BasicRoom)
      List(e1, e2, e1) foreach (e => service.addElement(room, e))
      
      from(service.relationTable)(e => where(e.roomId === room.id) select(e.exhibitId)
        orderBy(e.id asc)).toList must contain(e1.id, e2.id, e1.id).only.inOrder
    }
    
    def cannotAddToGroupRoom = service.addElement(roomOf(GroupRoom), e1) must
      throwA[IllegalArgumentException]
    
    def cannotAddToSmartRoom = service.addElement(roomOf(SmartRoom), e1) must
      throwA[IllegalArgumentException]
  }
  
  def getElementsOf(s: RoomElementService) = new TestBase(s) {
    val room = roomOf(BasicRoom)
    val e1, e2, dummy = elementOf("item")
    
    def returnsElements = {
      val elements = List(e1, e2, e2, e1).map(e => 
        service.relationTable.insert(RoomExhibit(room, e)))
      service.getElementsOf(room) must haveTheSameElementsAs(elements)
    }
    
    def returnsEmptyFromEmptyRoom = service.getElementsOf(room) must beEmpty
  }
  
  def removeElement(s: RoomElementService) = new TestBase(s) {
    val room = roomOf(BasicRoom)
    val e1, e2, e3 = elementOf("item")
    val elements = List(e1, e2, e3, e1).map(e => 
      service.relationTable.insert(RoomExhibit(room, e)))
    val result = service.removeElement(elements.head)
    
    def removesItem =
      service.getExhibits(room) must contain(e2, e3, e1).only.inOrder
    
    def removesElement =
      service.getElementsOf(room) must haveTheSameElementsAs(elements.tail)
    
    def removesFromTable =
      service.relationTable.lookup(elements.head.id) must beNone
    
    def returnsTrueToSuccess = result must beTrue
    
    def returnsFalstToFail = service.removeElement(elements.head) must beFalse
  }
}
