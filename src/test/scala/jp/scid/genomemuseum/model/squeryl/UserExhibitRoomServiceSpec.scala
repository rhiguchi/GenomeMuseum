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
    val roomTable = table[UserExhibitRoom]
    val exhibitTable = table[MuseumExhibit]
    val roomExhibitTable = table[RoomExhibit]
  
    /** 親子関係 */
    val roomTree = oneToManyRelation(roomTable, roomTable)
        .via((c, p) => c.parentId === p.id)
    roomTree.foreignKeyDeclaration.constrainReference(onDelete cascade)
    
    private val roomToRoomExhibitRelation = oneToManyRelation(roomTable, roomExhibitTable)
      .via((room, content) => room.id === content.roomId)
    roomToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
    
    /** 部屋の中身と展示物の関連 */
    private val exhibitToRoomExhibitRelation = oneToManyRelation(exhibitTable, roomExhibitTable)
      .via((exhibit, content) => exhibit.id === content.exhibitId)
    exhibitToRoomExhibitRelation.foreignKeyDeclaration.constrainReference(onDelete cascade)
  }
}

class UserExhibitRoomServiceSpec extends Specification with mock.Mockito {
  import UserExhibitRoomServiceSpec._
  
  private type Factory =
    (Table[UserExhibitRoom], Table[MuseumExhibit], Table[RoomExhibit]) =>
      UserExhibitRoomService
  
  def is = "展示物を置く部屋のデータモデル" ^
    "部屋を追加することができる" ^ canAddRoom(serviceOf) ^
    "親要素を取得できる" ^ canGetParent(serviceOf) ^
    "親要素を変更できる" ^ canSetParent(serviceOf) ^
    "子要素を取得できる" ^ canGetChildren(serviceOf) ^
    "部屋の除去" ^ canRemove(serviceOf) ^
    "部屋の更新" ^ canSave(serviceOf) ^
    end
  
  def serviceOf(table: Table[UserExhibitRoom], exhibitTable: Table[MuseumExhibit],
      relationTable: Table[RoomExhibit]) =
    new UserExhibitRoomService(table, exhibitTable, relationTable)
  
  def canAddRoom(f: Factory) =
    "データベースに追加される" ! addRoom(f).toTable ^
    "ID が付与される" ! addRoom(f).witId ^
    "親要素を適用する" ! addRoom(f).withParent ^
    "BasicRoom を親に指定するとその親が親となる" ! addRoom(f).parentBasicRooom ^
    "SmartRoom を親に指定するとその親が親となる" ! addRoom(f).parentSmartRoom ^
    bt
  
  def canGetParent(f: Factory) =
    "指定された親が返る" ! getParent(f).returnsParent ^
    "親が無い時は None が返る" ! getParent(f).returnsNoParent ^
    bt
  
  def canSetParent(f: Factory) =
    "新しい親を取得できる" ! setParent(f).persistsParentId ^
    "親をなくすことができる" ! setParent(f).persistsNoParentId ^
    "BasicRoom を親にすると例外" ! setParent(f).throwsExceptionWithBasicRoom ^
    "SmartRoom を親にすると例外" ! setParent(f).throwsExceptionWithSmartRoom ^
    "子孫を親にすると例外" ! setParent(f).throwsExceptionWithChild ^
    "自身を親にすると例外" ! setParent(f).throwsExceptionWithSelf ^
    bt
  
  def canGetChildren(f: Factory) =
    "None 指定でルート部屋の取得" ! getChildren(f).returnsItemsForRoot ^
    "親指定で子部屋の取得" ! getChildren(f).returnsItemsForParent ^
    "子が無い親は空を返す" ! getChildren(f).returnsNoItemsForNoChildren ^
    bt
  
  def canRemove(f: Factory) =
    "テーブルから除去される" ! remove(f).removesFromTable ^
    "取得できなくなる" ! remove(f).fromParent ^
    "除去されると true が返る" ! remove(f).returnsTrueToRemove ^
    "部屋が存在しないと false が返る" ! remove(f).returnsTrueNotToRemove ^
    "親を消すと子孫も消える" ! remove(f).removesDescendant ^
    bt
  
  def canSave(f: Factory) =
    "テーブルに変更が通知される" ! save(f).toTable ^
    bt
  
  class TestBase(f: Factory) {
    val schema = new UserExhibitRoomServiceSpec.TestSchema
    SquerylConnection.setUpSchema(schema)
    
    val roomTable = spy(schema.roomTable)
    val exhibitTable = spy(schema.exhibitTable)
    val roomExhibitTable = spy(schema.roomExhibitTable)
    
    val service = f(roomTable, exhibitTable, roomExhibitTable)
  }
  
  def addRoom(f: Factory) = new TestBase(f) {
    val basicRoom = service.addRoom(BasicRoom, "name", None)
    val groupRoom = service.addRoom(GroupRoom, "name", None)
    val smartRoom = service.addRoom(SmartRoom, "name", Some(groupRoom))
    
    def toTable = there was one(roomTable).insert(basicRoom) then
      one(roomTable).insert(groupRoom) then one(roomTable).insert(smartRoom)
    
    def witId = Seq(basicRoom, groupRoom, smartRoom) map (_.id) must
      contain(1L, 2L, 3L).only.inOrder
    
    def withParent = smartRoom.parentId must beSome(groupRoom.id)
    
    def parentBasicRooom = {
      val room = service.addRoom(BasicRoom, "child", Some(basicRoom))
      room.parentId must_== None
    }
    
    def parentSmartRoom = {
      val room = service.addRoom(BasicRoom, "child", Some(smartRoom))
      room.parentId must beSome(groupRoom.id)
    }
  }
  
  def getParent(f: Factory) = new TestBase(f) {
    val groupRoom = service.addRoom(GroupRoom, "name", None)
    val basicRoom = service.addRoom(BasicRoom, "name", Some(groupRoom))
    
    def returnsParent = service.getParent(basicRoom) must beSome(groupRoom)
    
    def returnsNoParent = service.getParent(groupRoom) must beNone
  }
  
  def setParent(f: Factory) = new TestBase(f) {
    val groupRoom = service.addRoom(GroupRoom, "name", None)
    val basicRoom = service.addRoom(BasicRoom, "name", None)
    service.setParent(basicRoom, Some(groupRoom))
    
    def persistsParentId = service.getParent(basicRoom) must beSome(groupRoom)
    
    def persistsNoParentId = {
      service.setParent(basicRoom, None)
      service.getParent(basicRoom) must beNone
    }
    
    def throwsExceptionWithBasicRoom = {
      val parent = service.addRoom(BasicRoom, "name", None)
      service.setParent(basicRoom, Some(parent)) must
        throwA[IllegalArgumentException]
    }
    
    def throwsExceptionWithSmartRoom = {
      val parent = service.addRoom(SmartRoom, "name", None)
      service.setParent(basicRoom, Some(parent)) must
        throwA[IllegalArgumentException]
    }
    
    def throwsExceptionWithChild = todo
    
    def throwsExceptionWithSelf = todo
  }
  
  def getChildren(f: Factory) = new TestBase(f) {
    val root1, root2 = service.addRoom(GroupRoom, "name", None)
    val child1, child2 = service.addRoom(GroupRoom, "name", Some(root1))
    
    def returnsItemsForRoot =
      service.getChildren(None) must contain(root1, root2).only
    
    def returnsItemsForParent =
      service.getChildren(Some(root1)) must contain(child1, child2).only
    
    def returnsNoItemsForNoChildren =
      service.getChildren(Some(root2)) must beEmpty
  }
  
  def remove(f: Factory) = new TestBase(f) {
    import org.squeryl.PrimitiveTypeMode._
    
    val parent = service.addRoom(GroupRoom, "name", None)
    val child1, child2 = service.addRoom(GroupRoom, "name", Some(parent))
    
    val result = service.remove(child1)
    
    def removesFromTable = there was one(roomTable).delete(child1.id)
    
    def fromParent = service.getChildren(Some(parent)) must contain(child2).only
    
    def returnsTrueToRemove = result must beTrue
    
    def returnsTrueNotToRemove = service.remove(child1) must beFalse
    
    def removesDescendant = {
      service.remove(parent)
      service.getParent(child2) must beNone
    }
  }
  
  def save(f: Factory) = new TestBase(f) {
    val room = service.addRoom(GroupRoom, "name", None)
    service save room
    
    def toTable = there was one(roomTable).update(room)
  }
}
