package jp.scid.genomemuseum.model.squeryl

import org.specs2._
import specification.Step
import execute.Result

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom}
import IUserExhibitRoom.RoomType._
import SquerylConnection._

private[squeryl] object UserExhibitRoomServiceSpec {
  import org.squeryl.Schema
  
  private[squeryl] class TestSchema extends Schema {
    private val schemaName = "UserExhibitRoomServiceSpec_" + util.Random.alphanumeric.take(5).mkString
    override def name = Some(schemaName)
    
    val userExhibitRoom = table[UserExhibitRoom]
  
    /** 親子関係 */
    val roomTree = oneToManyRelation(userExhibitRoom, userExhibitRoom)
        .via((c, p) => c.parentId === p.id)
    roomTree.foreignKeyDeclaration.constrainReference(onDelete cascade)
  }
  
  private[squeryl] trait UserExhibitRoomTesting {
    protected def userExhibitRoomTable: Table[UserExhibitRoom]
    
    def insertRoom(roomType: RoomType) =
      userExhibitRoomTable.insert(UserExhibitRoom("name", roomType, None))
    
    def insertRoom(roomType: RoomType, parent: UserExhibitRoom) =
      userExhibitRoomTable.insert(UserExhibitRoom("name", roomType, Some(parent.id)))
  }
}

class UserExhibitRoomServiceSpec extends Specification {
  import UserExhibitRoomServiceSpec._
  
  private type Factory = Table[UserExhibitRoom] => UserExhibitRoomService
  
  def is = "UserExhibitRoomService" ^
    "部屋の追加" ^ canAddRoom(serviceOf) ^
    "親の取得" ^ canGetParent(serviceOf) ^
    "親の設定" ^ canSetParent(serviceOf) ^
    "子の取得" ^ canGetChildren(serviceOf) ^
    "部屋の除去" ^ canRemove(serviceOf) ^
    "部屋の更新" ^ canSave(serviceOf) ^
    end
  
  def emptySchema = {
    val schema = new TestSchema
    setUpSchema(schema)
    schema
  }
  
  def serviceOf(table: Table[UserExhibitRoom]) = {
    // FIXME
    new UserExhibitRoomService(table, null)
  }
  
  def canAddRoom(f: Factory) =
    "ID の付与" ! addRoom(f).idPersists ^
    "名前 の付与" ! addRoom(f).namePersists ^
    "BasicRoom 作成" ! addRoom(f).basic ^
    "GroupRoom 作成" ! addRoom(f).group ^
    "SmartRoom 作成" ! addRoom(f).smart ^
    "親 の付与" ! addRoom(f).parentPersists ^
    "BasicRoom を親にすると例外" ! addRoom(f).throwsExceptionWithBasicRoomParent ^
    "SmartRoom を親にすると例外" ! addRoom(f).throwsExceptionWithSmartRoomParent ^
    bt
  
  def canGetParent(f: Factory) =
    "親を取得" ! getParent(f).returnsParent ^
    "親が無い時は None" ! getParent(f).returnsNoParent ^
    bt
  
  def canSetParent(f: Factory) =
    "親 ID の永続化" ! setParent(f).persistsParentId ^
    "親 ID の解除" ! setParent(f).persistsNoParentId ^
    "BasicRoom を親にすると例外" ! setParent(f).throwsExceptionWithBasicRoom ^
    "SmartRoom を親にすると例外" ! setParent(f).throwsExceptionWithSmartRoom ^
    bt
  
  def canGetChildren(f: Factory) =
    "ルート項目の取得" ! getChildren(f).returnsItemsForRoot ^
    "子項目の取得" ! getChildren(f).returnsItemsForParent ^
    "子が無い親は空を返す" ! getChildren(f).returnsNoItemsForNoChildren ^
    bt
  
  def canRemove(f: Factory) =
    "テーブルから除去" ! remove(f).removesFromTable ^
    "除去されると true" ! remove(f).returnsTrueToRemove ^
    "部屋が存在しないと false" ! remove(f).returnsTrueNotToRemove ^
    "親を消すと子孫も消える" ! remove(f).removesDescendant ^
    bt
  
  def canSave(f: Factory) =
    "名前変更の永続化" ! save(f).tableParsists ^
    bt
  
  class TestBase(f: Factory) extends UserExhibitRoomTesting {
    val table = emptySchema.userExhibitRoom
    val service = f(table)
    
    def userExhibitRoomTable = table
  }
  
  def addRoom(f: Factory) = new TestBase(f) {
    def idPersists = {
      val room1, room2, room3 = service.addRoom(BasicRoom, "name", None)
      List(room1, room2, room3).map(_.id) must_== List(1, 2, 3)
    }
    
    def namePersists = {
      val rooms = List("nameA", "nameB", "nameC") map
        (name => service.addRoom(BasicRoom, name, None))
      table.where(e => e.id in rooms.map(_.id)).map(_.name) must
        contain("nameA", "nameB", "nameC").only
    }
    
    def basic = {
      val room = service.addRoom(BasicRoom, "name", None)
      table.lookup(room.id).map(_.roomType) must beSome(BasicRoom)
    }
    
    def group = {
      val room = service.addRoom(GroupRoom, "name", None)
      table.lookup(room.id).map(_.roomType) must beSome(GroupRoom)
    }
    
    def smart = {
      val room = service.addRoom(SmartRoom, "name", None)
      table.lookup(room.id).map(_.roomType) must beSome(SmartRoom)
    }
    
    def parentPersists = {
      val parent1, parent2 = service.addRoom(GroupRoom, "parent", None)
      val children = List(parent1, parent2, parent2, parent1).map
        {p => service.addRoom(BasicRoom, "child", Some(p))}
      table.where(e => e.id in children.map(_.id)).flatMap(_.parentId) must_==
        List(parent1.id, parent2.id, parent2.id, parent1.id)
    }
    
    def throwsExceptionWithBasicRoomParent = {
      val parent = service.addRoom(BasicRoom, "parent", None)
      service.addRoom(BasicRoom, "child", Some(parent)) must
        throwA[IllegalArgumentException]
    }
    
    def throwsExceptionWithSmartRoomParent = {
      val parent = service.addRoom(SmartRoom, "parent", None)
      service.addRoom(BasicRoom, "child", Some(parent)) must
        throwA[IllegalArgumentException]
    }
  }
  
  def getParent(f: Factory) = new TestBase(f) {
    val parent = insertRoom(GroupRoom)
    
    def returnsParent = {
      val child = insertRoom(BasicRoom, parent)
      service.getParent(child) must beSome(parent)
    }
    
    def returnsNoParent = service.getParent(parent) must beNone
  }
  
  def setParent(f: Factory) = new TestBase(f) {
    def persistsParentId = {
      val parent = insertRoom(GroupRoom)
      val child = insertRoom(BasicRoom)
      service.setParent(child, Some(parent))
      table.lookup(child.id).flatMap(_.parentId) must beSome(parent.id)
    }
    
    def persistsNoParentId = {
      val child = insertRoom(BasicRoom, insertRoom(GroupRoom))
      service.setParent(child, None)
      table.lookup(child.id).get.parentId must beNone
    }
    
    def throwsExceptionWithBasicRoom = {
      val parent = insertRoom(BasicRoom)
      val child = insertRoom(BasicRoom)
      service.setParent(child, Some(parent)) must
        throwA[IllegalArgumentException]
    }
    
    def throwsExceptionWithSmartRoom = {
      val parent = insertRoom(SmartRoom)
      val child = insertRoom(BasicRoom)
      service.setParent(child, Some(parent)) must
        throwA[IllegalArgumentException]
    }
  }
  
  def getChildren(f: Factory) = new TestBase(f) {
    val p = insertRoom(GroupRoom)
    val c1, c2 = insertRoom(BasicRoom)
    val c3, c4 = insertRoom(BasicRoom, p)
    val c5 = insertRoom(GroupRoom, p)
    
    def returnsItemsForRoot =
      service.getChildren(None) must contain(p, c1, c2).only
    
    def returnsItemsForParent =
      service.getChildren(Some(p)) must contain(c3, c4, c5).only
    
    def returnsNoItemsForNoChildren =
      service.getChildren(Some(c5)) must beEmpty
  }
  
  def remove(f: Factory) = new TestBase(f) {
    val r1 = insertRoom(GroupRoom)
    val r2 = insertRoom(GroupRoom, r1)
    val r3 = insertRoom(BasicRoom, r2)
    
    assert(table.lookup(r3.id).nonEmpty)
    val result = service.remove(r1)
    
    def removesFromTable = table.lookup(r1.id) must beNone
    
    def returnsTrueToRemove = result must beTrue
    
    def returnsTrueNotToRemove = service.remove(r1) must beFalse
    
    def removesDescendant =  table.lookup(r3.id) must beNone
  }
  
  def save(f: Factory) = new TestBase(f) {
    val r1, r2, r3 = insertRoom(BasicRoom)
    r1.name = "a"
    r2.name = "b"
    r3.name = "c"
    List(r1, r2, r3) foreach (service.save)
    
    def tableParsists = {
      List(r1, r2, r3) flatMap (r => table.lookup(r.id)) map (_.name) must
        contain("a", "b", "c").only
    }
  }
}
