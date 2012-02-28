package jp.scid.genomemuseum.model

import collection.script.{Message, Include, Update, Remove}

import org.specs2._
import UserExhibitRoom.RoomType._

class MuseumStructureSpec extends Specification with mock.Mockito {
  def is = "MuseumStructure" ^
    "葉要素判定" ^ isLeafSpec(createStructure) ^
    "子要素" ^ getChildrenSpec(createStructure) ^
    "ルートまでのパス" ^ pathToRootSpec(createStructure) ^
    "部屋の追加" ^ canAddRoom(createStructure) ^
    "部屋の削除" ^ canRemoveRoom(createStructure) ^
    end
  
  def createStructure() = new MuseumStructure()
  
  def isLeafSpec(s: => MuseumStructure) =
//    "root は false" ! isLeaf(s).root ^
//    "ライブラリカテゴリ は false" ! isLeaf(s).sourcesRoot ^
//    "ユーザー部屋カテゴリ は false" ! isLeaf(s).userRoomsRoot ^
//    "ローカルライブラリ は true" ! isLeaf(s).localSource ^
//    "ウェブ検索 は true" ! isLeaf(s).webSource ^
//    "GroupRoom は false" ! isLeaf(s).gRoom ^
//    "BasicRoom は true" ! isLeaf(s).bRoom ^
//    "SmartRoom は true" ! isLeaf(s).sRoom ^
    bt
  
  def getChildrenSpec(s: => MuseumStructure) =
//    "root は ライブラリカテゴリとユーザー部屋カテゴリ" ! getChildren(s).root ^
//    "ライブラリカテゴリはローカルをウェブ検索" ! getChildren(s).sourcesRoot ^
//    "ユーザー部屋カテゴリは roomService を参照" ! getChildren(s).userRoomsRoot ^
//    "BasicRoom は常に empty" ! getChildren(s).bRoom ^
//    "SmartRoom は常に empty" ! getChildren(s).sRoom ^
//    "GroupRoom は roomService を参照" ! getChildren(s).gRoom ^
    bt
  
  def pathToRootSpec(s: => MuseumStructure) =
//    "root" ! pathToRoot(s).root ^
//    "userRoomsRoot" ! pathToRoot(s).userRoomsRoot ^
//    "sourcesRoot" ! pathToRoot(s).sourcesRoot ^
//    "webSource" ! pathToRoot(s).webSource ^
//    "localSource" ! pathToRoot(s).localSource ^
//    "UserExhibitRoom" ! pathToRoot(s).userRoom ^
    bt
  
  def canAddRoom(s: => MuseumStructure) =
//    "親なしで部屋を追加" ! addRoom(s).withNoParemt ^
//    "プロパティに指定した名前で作成される" ! addRoom(s).defaultName ^
//    "既に名前が存在している時は連番が付与される" ! addRoom(s).nameWithSuffix ^
//    "GroupRoom を親にして部屋を追加" ! addRoom(s).withParent ^
    bt
  
  def canRemoveRoom(s: => MuseumStructure) =
//    "サービスに処理を委譲" ! removeRoom(s).callsService ^
    bt
  
  /** 葉要素判定 */
  def isLeaf(structure: MuseumStructure) = new {
//    private def getLeaf(func: MuseumStructure => ExhibitRoom) = structure.isLeaf(func(structure))
//    
//    def root = getLeaf(_.root) must beFalse
//    
//    def sourcesRoot = getLeaf(_.sourcesRoot) must beFalse
//    
//    def userRoomsRoot = getLeaf(_.userRoomsRoot) must beFalse
//    
//    def localSource = getLeaf(_.localSource) must beTrue
//    
//    def webSource = getLeaf(_.webSource) must beTrue
//    
//    def gRoom = structure.isLeaf(UserExhibitRoomMock.of(GroupRoom)) must beFalse
//    
//    def bRoom = structure.isLeaf(UserExhibitRoomMock.of(BasicRoom)) must beTrue
//    
//    def sRoom = structure.isLeaf(UserExhibitRoomMock.of(SmartRoom)) must beTrue
  }
  
  /** 子要素判定 */
  def getChildren(structure: MuseumStructure) = new {
//    import scala.collection.JavaConverters._
//    
//    val roomService = UserExhibitRoomServiceMock.of()
//    
//    val basicRoom = UserExhibitRoomMock.of(BasicRoom)
//    val smartRoom = UserExhibitRoomMock.of(SmartRoom)
//    val groupRoom = UserExhibitRoomMock.of(GroupRoom)
//    structure.userExhibitRoomService = Some(roomService)
//    
//    def root = structure.getChildren(structure.root).asScala must_==
//      List(structure.sourcesRoot, structure.userRoomsRoot)
//      
//    def sourcesRoot = structure.getChildren(structure.sourcesRoot).asScala must_==
//      List(structure.localSource, structure.webSource)
//    
//    def userRoomsRoot = {
//      val children = List(basicRoom, smartRoom, groupRoom)
//      roomService.getChildren(None) returns children
//      
//      structure.getChildren(structure.userRoomsRoot).asScala must_== children
//    }
//    
//    def bRoom = {
//      roomService.getChildren(Some(basicRoom)) returns List(basicRoom)
//      
//      structure.getChildren(basicRoom).asScala must beEmpty
//    }
//    
//    def sRoom = {
//      roomService.getChildren(Some(smartRoom)) returns List(smartRoom)
//      
//      structure.getChildren(smartRoom).asScala must beEmpty
//    }
//    
//    def gRoom = {
//      roomService.getChildren(Some(groupRoom)) returns List(basicRoom, smartRoom)
//      
//      structure.getChildren(groupRoom).asScala must_== List(basicRoom, smartRoom)
//    }
  }
  
  /** ルートまでのパス */
  def pathToRoot(structure: MuseumStructure) = new {
//    private def getPath(func: MuseumStructure => ExhibitRoom) = structure.pathToRoot(func(structure))
//    
//    def root = getPath(_.root) must_== Seq(structure.root)
//    
//    def userRoomsRoot = getPath(_.userRoomsRoot) must_==
//      Seq(structure.root, structure.userRoomsRoot)
//    
//    def sourcesRoot = getPath(_.sourcesRoot) must_==
//      Seq(structure.root, structure.sourcesRoot)
//    
//    def webSource = getPath(_.webSource) must_==
//      Seq(structure.root, structure.sourcesRoot, structure.webSource)
//    
//    def localSource = getPath(_.localSource) must_==
//      Seq(structure.root, structure.sourcesRoot, structure.localSource)
//    
//    def userRoom = {
//      val roomService = UserExhibitRoomServiceMock.of()
//      val room1, room2, room3, room4 = UserExhibitRoomMock.of(GroupRoom)
//      roomService.getParent(room4) returns Some(room3)
//      roomService.getParent(room3) returns Some(room2)
//      roomService.getParent(room2) returns Some(room1)
//      roomService.getParent(room1) returns None
//      
//      structure.pathToRoot(room3) must_== (structure.root ::
//        structure.userRoomsRoot :: room1 :: room2 :: room3 :: Nil)
//    }
  }
  
  /** 部屋の追加 */
  def addRoom(structure: MuseumStructure) = new {
//    val roomService = UserExhibitRoomServiceMock.of()
//    val room1, room2, room3 = UserExhibitRoomMock.of(BasicRoom)
//    roomService.addRoom(BasicRoom, structure.basicRoomDefaultName, None) returns room1
//    roomService.addRoom(GroupRoom, structure.groupRoomDefaultName, None) returns room2
//    roomService.addRoom(SmartRoom, structure.smartRoomDefaultName, None) returns room3
//    roomService.addRoom(BasicRoom, "name1", None) returns room2
//    roomService.addRoom(GroupRoom, "name2", None) returns room3
//    roomService.addRoom(SmartRoom, "name3", None) returns room1
//    roomService.nameExists("n") returns true
//    roomService.nameExists("n 1") returns true
//    roomService.nameExists("n 2") returns true
//    
//    val roomTypes = Seq(BasicRoom, GroupRoom, SmartRoom)
//    
//    structure.userExhibitRoomService = Some(roomService)
//
//    def withNoParemt = roomTypes.map(t => structure.addRoom(t, None)) must_==
//      Seq(room1, room2, room3)
//
//    def defaultName = {
//      structure.basicRoomDefaultName = "name1"
//      structure.groupRoomDefaultName = "name2"
//      structure.smartRoomDefaultName = "name3"
//      
//      roomTypes.map(t => structure.addRoom(t, None)) must_==
//        Seq(room2, room3, room1)
//    }
//    
//    def nameWithSuffix = {
//      roomService.addRoom(BasicRoom, "n 3", None) returns room1
//      structure.basicRoomDefaultName = "n"
//      
//      structure.addRoom(BasicRoom, None) must_== room1
//    }
//    
//    def withParent = {
//      val parent = Some(UserExhibitRoomMock.of(GroupRoom))
//      roomService.addRoom(BasicRoom, structure.basicRoomDefaultName, parent) returns room1
//      roomService.addRoom(GroupRoom, structure.groupRoomDefaultName, parent) returns room2
//      roomService.addRoom(SmartRoom, structure.smartRoomDefaultName, parent) returns room3
//      
//      roomTypes.map(t => structure.addRoom(t, parent)) must_==
//        Seq(room1, room2, room3)
//    }
  }
  
  
  /** 部屋の削除 */
  def removeRoom(structure: MuseumStructure) = new {
//    val room = UserExhibitRoomMock.of(BasicRoom)
//    val roomService = UserExhibitRoomServiceMock.of()
//    structure.userExhibitRoomService = Some(roomService)
//    
//    structure.removeRoom(room)
//    
//    def callsService = there was one(roomService).remove(room)
  }
}
