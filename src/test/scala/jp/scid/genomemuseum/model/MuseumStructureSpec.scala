package jp.scid.genomemuseum.model

import collection.script.{Message, Include, Update, Remove}

import org.specs2._
import UserExhibitRoom.RoomType._

class MuseumStructureSpec extends Specification with mock.Mockito {
  private type Factory = UserExhibitRoomService => MuseumStructure
  
  def is = "MuseumStructure" ^
    "葉要素判定" ^ isLeafSpec(createStructure) ^
    "子要素" ^ childrenForSpec(createStructure) ^
    "更新" ^ updateSpec(createStructure) ^
    "ルートまでのパス" ^ pathToRootSpec(createStructure) ^
    "部屋の追加" ^ canAddRoom(createStructure) ^
    "部屋の移動可能判定" ^ canMoveSpec(createStructure) ^
    "部屋の移動" ^ canMoveRoom(createStructure) ^
    "部屋の削除" ^ canRemoveRoom(createStructure) ^
    "サービスのイベント委譲" ^ canDelegateEvent(createStructure) ^
    end
  
  def createStructure(service: UserExhibitRoomService) =
    new MuseumStructure(service)
  
  def isLeafSpec(f: Factory) =
    "root は false" ! isLeaf(f).root ^
    "ライブラリカテゴリ は false" ! isLeaf(f).sourcesRoot ^
    "ユーザー部屋カテゴリ は false" ! isLeaf(f).userRoomsRoot ^
    "ローカルライブラリ は true" ! isLeaf(f).localSource ^
    "ウェブ検索 は true" ! isLeaf(f).webSource ^
    "GroupRoom は false" ! isLeaf(f).gRoom ^
    "BasicRoom は true" ! isLeaf(f).bRoom ^
    "SmartRoom は true" ! isLeaf(f).sRoom ^
    bt
  
  def childrenForSpec(f: Factory) =
    "root は ライブラリカテゴリとユーザー部屋カテゴリ" ! childrenFor(f).root ^
    "ライブラリカテゴリはローカルをウェブ検索" ! childrenFor(f).sourcesRoot ^
    "ユーザー部屋カテゴリは roomService を参照" ! childrenFor(f).userRoomsRoot ^
    "BasicRoom は常に empty" ! childrenFor(f).bRoom ^
    "SmartRoom は常に empty" ! childrenFor(f).sRoom ^
    "GroupRoom は roomService を参照" ! childrenFor(f).gRoom ^
    bt
  
  def updateSpec(f: Factory) =
    "UserExhibitRoom は roomService の save をコール" ! update(f).callsSave ^
    "UserExhibitRoom の名前を更新" ! update(f).nameApplied ^
    bt
  
  def pathToRootSpec(f: Factory) =
    "root" ! pathToRoot(f).root ^
    "userRoomsRoot" ! pathToRoot(f).userRoomsRoot ^
    "sourcesRoot" ! pathToRoot(f).sourcesRoot ^
    "webSource" ! pathToRoot(f).webSource ^
    "localSource" ! pathToRoot(f).localSource ^
    "UserExhibitRoom" ! pathToRoot(f).userRoom ^
    bt
  
  def canAddRoom(f: Factory) =
    "親なしで部屋を追加" ! addRoom(f).withNoParemt ^
    "プロパティに指定した名前で作成される" ! addRoom(f).defaultName ^
    "既に名前が存在している時は連番が付与される" ! addRoom(f).nameWithSuffix ^
    "GroupRoom を親にして部屋を追加" ! addRoom(f).withParent ^
    bt
  
  def canMoveSpec(f: Factory) =
    "GroupRoom に移動可能" ! canMove(f).toGroupRoom ^
    "最上位に移動可能" ! canMove(f).toTop ^
    "最上位から最上位は移動できない" ! canMove(f).falseToTopFromTop ^
    "自分以下には移動できない" ! canMove(f).falseToDescOrEqual ^
    bt
  
  def canMoveRoom(f: Factory) =
    "GroupRoom に移動" ! moveRoom(f).toGroupRoom ^
    "最上位に移動" ! moveRoom(f).toTop ^
    bt
  
  def canRemoveRoom(f: Factory) =
    "サービスに処理を委譲" ! removeRoom(f).callsService ^
    bt
  
  def canDelegateEvent(f: Factory) =
    "Include" ! delegateEvent(f).include ^
    "Update" ! delegateEvent(f).udpate ^
    "Remove" ! delegateEvent(f).remove ^
    bt
  
  class TestBase(f: Factory) {
    val roomService = UserExhibitRoomServiceMock.of()
    val structure = f(roomService)
  }
  
  /** 葉要素判定 */
  def isLeaf(f: Factory) = new TestBase(f) {
    private def getLeaf(func: MuseumStructure => ExhibitRoom) = structure.isLeaf(func(structure))
    
    def root = getLeaf(_.root) must beFalse
    
    def sourcesRoot = getLeaf(_.sourcesRoot) must beFalse
    
    def userRoomsRoot = getLeaf(_.userRoomsRoot) must beFalse
    
    def localSource = getLeaf(_.localSource) must beTrue
    
    def webSource = getLeaf(_.webSource) must beTrue
    
    def gRoom = structure.isLeaf(UserExhibitRoomMock.of(GroupRoom)) must beFalse
    
    def bRoom = structure.isLeaf(UserExhibitRoomMock.of(BasicRoom)) must beTrue
    
    def sRoom = structure.isLeaf(UserExhibitRoomMock.of(SmartRoom)) must beTrue
  }
  
  /** 子要素判定 */
  def childrenFor(f: Factory) = new TestBase(f) {
    val basicRoom = UserExhibitRoomMock.of(BasicRoom)
    val smartRoom = UserExhibitRoomMock.of(SmartRoom)
    val groupRoom = UserExhibitRoomMock.of(GroupRoom)
    
    def root = structure.childrenFor(structure.root) must_==
      List(structure.sourcesRoot, structure.userRoomsRoot)
      
    def sourcesRoot = structure.childrenFor(structure.sourcesRoot) must_==
      List(structure.localSource, structure.webSource)
    
    def userRoomsRoot = {
      val children = List(basicRoom, smartRoom, groupRoom)
      roomService.getChildren(None) returns children
      
      structure.childrenFor(structure.userRoomsRoot) must_== children
    }
    
    def bRoom = {
      roomService.getChildren(Some(basicRoom)) returns List(basicRoom)
      
      structure.childrenFor(basicRoom) must beEmpty
    }
    
    def sRoom = {
      roomService.getChildren(Some(smartRoom)) returns List(smartRoom)
      
      structure.childrenFor(smartRoom) must beEmpty
    }
    
    def gRoom = {
      roomService.getChildren(Some(groupRoom)) returns List(basicRoom, smartRoom)
      
      structure.childrenFor(groupRoom) must_== List(basicRoom, smartRoom)
    }
  }
  
  /** 更新 */
  def update(f: Factory) = new TestBase(f) {
    val basicRoom = IndexedSeq(UserExhibitRoomMock.of(BasicRoom))
    val smartRoom = IndexedSeq(UserExhibitRoomMock.of(SmartRoom))
    val groupRoom = IndexedSeq(UserExhibitRoomMock.of(GroupRoom))
    
    def callsSave = {
      structure.update(basicRoom, "newValue")
      there was one(roomService).save(basicRoom.last)
    }
    
    def nameApplied = {
      structure.update(basicRoom, "newName")
      structure.update(basicRoom, "newName2")
      structure.update(smartRoom, "n")
      structure.update(groupRoom, "1")
      
      there was one(basicRoom.last).name_=("newName") then
        one(basicRoom.last).name_=("newName2") then
        one(smartRoom.last).name_=("n") then
        one(groupRoom.last).name_=("1")
    }
  }
  
  /** ルートまでのパス */
  def pathToRoot(f: Factory) = new TestBase(f) {
    private def getPath(func: MuseumStructure => ExhibitRoom) = structure.pathToRoot(func(structure))
    
    def root = getPath(_.root) must_== Seq(structure.root)
    
    def userRoomsRoot = getPath(_.userRoomsRoot) must_==
      Seq(structure.root, structure.userRoomsRoot)
    
    def sourcesRoot = getPath(_.sourcesRoot) must_==
      Seq(structure.root, structure.sourcesRoot)
    
    def webSource = getPath(_.webSource) must_==
      Seq(structure.root, structure.sourcesRoot, structure.webSource)
    
    def localSource = getPath(_.localSource) must_==
      Seq(structure.root, structure.sourcesRoot, structure.localSource)
    
    def userRoom = {
      val room1, room2, room3, room4 = UserExhibitRoomMock.of(GroupRoom)
      roomService.getParent(room4) returns Some(room3)
      roomService.getParent(room3) returns Some(room2)
      roomService.getParent(room2) returns Some(room1)
      roomService.getParent(room1) returns None
      
      structure.pathToRoot(room3) must_== (structure.root ::
        structure.userRoomsRoot :: room1 :: room2 :: room3 :: Nil)
    }
  }
  
  /** 部屋の追加 */
  def addRoom(f: Factory) = new TestBase(f) {
    val room1, room2, room3 = UserExhibitRoomMock.of(BasicRoom)
    roomService.addRoom(BasicRoom, structure.basicRoomDefaultName, None) returns room1
    roomService.addRoom(GroupRoom, structure.groupRoomDefaultName, None) returns room2
    roomService.addRoom(SmartRoom, structure.smartRoomDefaultName, None) returns room3
    roomService.addRoom(BasicRoom, "name1", None) returns room2
    roomService.addRoom(GroupRoom, "name2", None) returns room3
    roomService.addRoom(SmartRoom, "name3", None) returns room1
    roomService.nameExists("n") returns true
    roomService.nameExists("n 1") returns true
    roomService.nameExists("n 2") returns true
    
    val roomTypes = Seq(BasicRoom, GroupRoom, SmartRoom)

    def withNoParemt = roomTypes.map(t => structure.addRoom(t, None)) must_==
      Seq(room1, room2, room3)

    def defaultName = {
      structure.basicRoomDefaultName = "name1"
      structure.groupRoomDefaultName = "name2"
      structure.smartRoomDefaultName = "name3"
      
      roomTypes.map(t => structure.addRoom(t, None)) must_==
        Seq(room2, room3, room1)
    }
    
    def nameWithSuffix = {
      roomService.addRoom(BasicRoom, "n 3", None) returns room1
      structure.basicRoomDefaultName = "n"
      
      structure.addRoom(BasicRoom, None) must_== room1
    }
    
    def withParent = {
      val parent = Some(UserExhibitRoomMock.of(GroupRoom))
      roomService.addRoom(BasicRoom, structure.basicRoomDefaultName, parent) returns room1
      roomService.addRoom(GroupRoom, structure.groupRoomDefaultName, parent) returns room2
      roomService.addRoom(SmartRoom, structure.smartRoomDefaultName, parent) returns room3
      
      roomTypes.map(t => structure.addRoom(t, parent)) must_==
        Seq(room1, room2, room3)
    }
  }
  
  /** 部屋の移動可能性 */
  def canMove(f: Factory) = new TestBase(f) {
    val bRoom = UserExhibitRoomMock.of(BasicRoom)
    val gRoom = UserExhibitRoomMock.of(GroupRoom)
    val sRoom = UserExhibitRoomMock.of(SmartRoom)
    val rooms = Seq(bRoom, gRoom, sRoom)
    val roomTypes = Seq(BasicRoom, GroupRoom, SmartRoom)

    def toGroupRoom = {
      val dest = Some(UserExhibitRoomMock.of(GroupRoom))
      rooms.map(r => structure.canMove(r, dest)) must_== Seq(true, true, true)
    }
    
    def toTop = {
      val parent = Some(UserExhibitRoomMock.of(GroupRoom))
      rooms.foreach(r => roomService.getParent(r) returns parent)
      rooms.map(r => structure.canMove(r, None)) must_== Seq(true, true, true)
    }
    
    def falseToTopFromTop =
      rooms.map(r => structure.canMove(r, None)) must_== Seq(false, false, false)
    
    def falseToDescOrEqual = {
      val gRoom1, gRoom2, gRoom3 = UserExhibitRoomMock.of(GroupRoom)
      roomService.getParent(gRoom3) returns Some(gRoom2)
      roomService.getParent(gRoom2) returns Some(gRoom1)
      
      Seq(bRoom, gRoom, sRoom, gRoom2, gRoom3).map(r => structure.canMove(r, Some(gRoom3))) must_==
        Seq(true, true, true, false, false)
    }
  }
  
  /** 部屋の移動 */
  def moveRoom(f: Factory) = new TestBase(f) {
    val room = UserExhibitRoomMock.of(BasicRoom)
    val parent = Some(UserExhibitRoomMock.of(GroupRoom))
    roomService.getParent(room) returns parent
    
    val gRoom = UserExhibitRoomMock.of(GroupRoom)
    
    def toGroupRoom = {
      structure.moveRoom(room, None)
      there was one(roomService).setParent(room, None)
    }
    
    def toTop = {
      val room2 = UserExhibitRoomMock.of(BasicRoom)
      structure.moveRoom(room2, parent)
      there was one(roomService).setParent(room2, parent)
    }
  }
  
  /** 部屋の削除 */
  def removeRoom(f: Factory) = new TestBase(f) {
    val room = UserExhibitRoomMock.of(BasicRoom)
    structure.removeRoom(room)
    
    def callsService = there was one(roomService).remove(room)
  }
  
  /** サービスのイベント委譲 */
  def delegateEvent(f: Factory) = new {
    val roomService = UserExhibitRoomServiceMock.canPublish()
    val structure = f(roomService)
    val room = UserExhibitRoomMock.of(BasicRoom)
    
    var published = IndexedSeq.empty[Message[ExhibitRoom]]
    private val sub = new structure.Sub {
      def notify(pub: structure.Pub, event: Message[ExhibitRoom]) {
        published = published :+ event
      }
    }
    structure.subscribe(sub)
    
    def include = {
      val event = new Include(room)
      roomService.publish(event)
      published must_== Seq(event)
    }
    def udpate = {
      val event = new Update(room)
      roomService.publish(event)
      published must_== Seq(event)
    }
    def remove = {
      val event = new Remove(room)
      roomService.publish(event)
      published must_== Seq(event)
    }
  }
}
