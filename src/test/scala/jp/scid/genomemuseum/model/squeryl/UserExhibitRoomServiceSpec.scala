package jp.scid.genomemuseum.model.squeryl

import org.specs2._
import specification.Step
import execute.Result

import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom}
import IUserExhibitRoom.RoomType._

class UserExhibitRoomServiceSpec extends Specification with DatabaseConnectable {
  
  def is = "UserExhibitRoomService" ^ Step(openDatabase) ^
    p ^ "addRoom GroupRoom 親" ^
      "BasicRoom" ^ isTableInserted(roomAddingOf(BasicRoom)) ^ bt ^
      "GroupRoom" ^ isTableInserted(roomAddingOf(GroupRoom)) ^ bt ^
      "SmartRoom" ^ isTableInserted(roomAddingOf(SmartRoom)) ^ bt ^
      "BasicRoom 親では例外送出" ! roomAddingWithParentOf(BasicRoom).throwsIAException ^ bt ^
      "SmartRoom 親では例外送出" ! roomAddingWithParentOf(SmartRoom).throwsIAException ^ bt ^
    p ^ "remove" ^
      "要素の削除" ! remove.simple ^
      "親要素の削除で子要素も削除" ! remove.treeDeletion ^
    bt ^ "setParent" ^ canSetParent(roomAddingOf()) ^
    bt ^ "名前検索" ^ isNameSearchable ^
    bt ^ "保存" ^ isPersisted(savedEntity) ^
    bt ^ "親の取得" ^ canGetParent ^
    bt ^ "子の取得" ^ canGetChildren ^
    Step(closeDatabase) ^ end
  
  /** ショートカット */
  val table = schema.userExhibitRoom
  /** ショートカット */
  val service = new UserExhibitRoomService(table)
  
  /** テーブルに要素を挿入 */
  def insertRoom(roomType: RoomType = BasicRoom, name: String = "room",
      parentId: Option[Long] = None) = inTransaction {
    table.insert(UserExhibitRoom(name, roomType, parentId))
  }
  
  /** テーブルに親要素を挿入 */
  def insertParent(name: String = "room", parentId: Option[Long] = None) =
    insertRoom(GroupRoom, name, parentId)
  
  /** 要素を指定した数分挿入 */
  def insertRooms(count: Int, parent: Option[UserExhibitRoom] = None) = inTransaction {
    val pid = parent.map(_.id)
    Range(0, count).map
      { index => insertRoom(BasicRoom, "child" + index, pid) }.toList
  }
  
  class TestBase {
    def findBy(roomId: Long) = inTransaction { table.lookup(roomId) }
    
    def addParent(name: String = "parent") =
      addRoom(GroupRoom, "parent")
      
    def addRoom(roomType: RoomType = BasicRoom, name: String = "room") =
      service.addRoom(roomType, name, None)
    
    def parentIdFor(room: UserExhibitRoom) = inTransaction {
      from(table)(e => where(e.id === room.id) select(e.parentId)).head
    }
  }
  
  def remove = new TestBase {
    def simple = {
      val room = addRoom()
      val roomId = room.id
      service.remove(room)
      
      findBy(roomId) must beNone
    }
    
    def treeDeletion = {
      val parent = addParent()
      val child = service.addRoom(BasicRoom, "child", Some(parent))
      val childId = child.id
      service.remove(parent)
      
      findBy(childId) must beNone
    }
  }
  def setParent(roomType: RoomType = GroupRoom) = new TestBase {
    def parentIdAttached = {
      val parent = addRoom(roomType)
      parentIdFor(entityWithParentAttached(parent)) must beSome(parent.id)
    }
    
    def isValid = parentIdAttached
    
    def entityWithParentAttached(parent: UserExhibitRoom) = {
      val parentId = parent.id
      val room = addRoom()
      service.setParent(room, Some(parent))
      room
    }
  }
  
  def roomAddingOf(roomType: RoomType = BasicRoom, name: String = "room",
      parent: Option[UserExhibitRoom] = None) = 
    service.addRoom(roomType, name, parent)
  
  def roomAddingWithParentOf(roomType: RoomType) = new TestBase {
    def throwsIAException = {
      val parent = roomAddingOf(name = "child")
      service.addRoom(roomType, "parent", Some(parent)) must throwA[IllegalArgumentException]
    }
  }
  
  def roomTree = {
    val roomA = service.addRoom(GroupRoom, "roomA", None)
    val roomB = service.addRoom(GroupRoom, "roomB", Some(roomA))
    val roomC = service.addRoom(GroupRoom, "roomC", Some(roomB))
    IndexedSeq(roomA, roomB, roomC)
  }
  
  def roomEntity(room: UserExhibitRoom) = new TestBase {
    def hasId = room.id must_!= 0
    
    def rowCreated = findBy(room.id) must beSome
    
    def roomTypePersisted = {
      val roomTypeOp = inTransaction {
        from(table)(e => where(e.id === room.id) select(e.roomType)).headOption
      }
      roomTypeOp must beSome(room.roomType)
    }
    
    def namePersistence = {
      val roomNameOp = inTransaction {
        from(table)(e => where(e.id === room.id) select(e.name)).headOption
      }
      roomNameOp must beSome(room.name)
    }
    
    def notExist = findBy(room.id) must beNone
    
    def cannotSetParent(roomType: RoomType) = {
      val parent = addRoom(roomType)
      service.setParent(room, Some(parent)) must
        throwA[IllegalArgumentException] 
    }
    
    def canSetParent = {
      val parent = addParent()
      service.setParent(room, Some(parent))
      // 永続化検証
      parentIdFor(room) must beSome(parent.id)
    }
    
    def canUnsetParent = {
      service.setParent(room, None)
      // 永続化検証
      parentIdFor(room) must beNone
    }
  }
  
  def tableDataOf(roomId: Long) = new TestBase {
    def hasParentId(expected: Long) = {
      val pIdOp = inTransaction {
        from(table)(e => where(e.id === roomId) select(e.parentId)).headOption
      }
      pIdOp must beSome(expected)
    }
  }
  
  def nameExists(names: String*) = new TestBase {
    names.foreach(name => addRoom(name = name))
    
    def existsRoomFor(name: String) = service.nameExists(name) must beTrue
    
    def notExistsRoomFor(name: String) = service.nameExists(name) must beFalse
  }
  
  def getParentSpec = new TestBase {
    def returnsParent = {
      val parent = insertParent("parent")
      val child = insertRoom(parentId = Some(parent.id))
      service.getParent(child) must beSome(parent)
    }
    
    def returnsNone =
      service.getParent(insertRoom()) must beNone
  }
  
  def getChildrenSpec = new TestBase {
    def returnsChildren = {
      val parent = Some(insertParent())
      val children = insertRooms(5, parent)
      service.getChildren(parent) must haveTheSameElementsAs(children)
    }
    
    def rootChildren = {
      val List(c1, c2) = insertRooms(2)
      service.getChildren(None) must contain(c1, c2)
    }
    
    def noChild = {
      val parent = Some(insertParent())
      service.getChildren(parent) must beEmpty
    }
  }
  
  def savedEntity = {
    val room = insertRoom(BasicRoom, "0")
    room.name = "aaaaaa"
    service.save(room)
    room
  }
  
  def isTableInserted(room: => UserExhibitRoom) =
    "ID が付与" ! roomEntity(room).hasId ^
    "行が永続化" ! roomEntity(room).rowCreated ^
    "roomType が設定" ! roomEntity(room).roomTypePersisted ^
    "名前 が永続化" ! roomEntity(room).namePersistence
  
  def canSetParent(room: => UserExhibitRoom) =
    "GroupRoom を設定できる" ! roomEntity(room).canSetParent ^
    "BasicRoom では例外送出" ! roomEntity(room).cannotSetParent(BasicRoom) ^
    "SmartRoom では例外送出" ! roomEntity(room).cannotSetParent(SmartRoom) ^
    "親の設定を解除できる" ! roomEntity(room).canUnsetParent
  
  def isNameSearchable =
    "存在する部屋名には true" ! nameExists("a", "b").existsRoomFor("b") ^
    "存在しない部屋名には false" ! nameExists("c", "d").notExistsRoomFor("x")
    
  def isPersisted(room: => UserExhibitRoom) =
    "名前が永続化" ! roomEntity(room).namePersistence
  
  def canGetParent =
    "親が存在する要素には親要素" ! getParentSpec.returnsParent ^
    "親が存在しない要素は None" ! getParentSpec.returnsNone
  
  def canGetChildren =
    "要素の子を取得" ! getChildrenSpec.returnsChildren ^
    "ルート（親が存在しない）要素を取得" ! getChildrenSpec.rootChildren ^
    "子が存在しない時は Nil"! getChildrenSpec.noChild
}
