package jp.scid.genomemuseum.model

import org.specs2._
import mock._
import UserExhibitRoom.RoomType._

class MuseumStructureSpec extends Specification with Mockito {
  def newMuseumStructure = {
    new MuseumStructure
  }
  
  def is = "MuseumStructure" ^
    "isLeaf" ^
      "root は false" ! initial.s1 ^
      "localSource は true" ! initial.s2 ^
      "webSource は true" ! initial.s3 ^
      "sourcesRoot は false" ! initial.s4 ^
      "userRoomsRoot は false" ! initial.s5 ^
      "UserExhibitRoom オブジェクト" ^
        "GroupRoom は false" ! initial.s6 ^
        "BasicRoom は true" ! initial.s7 ^
        "SmartRoom は true" ! initial.s8 ^
    bt ^ bt ^ "childrenFor" ^
      "root の子要素" ! initial.s9 ^
      "UserExhibitRoom オブジェクト" ^
        "GroupRoom で roomSource から取得" ! childrenFor.s1 ^
        "BasicRoom では Nil" ! childrenFor.s2 ^
        "SmartRoom では Nil" ! childrenFor.s3 ^
      bt ^"childrenForUserRooms の子要素" ! childrenForUserRooms.s1 ^
    bt ^ "pathToRoot" ^
      "root" ! pathToRoot.s1 ^
      "sourcesRoot" ! pathToRoot.s2 ^
      "localSource" ! pathToRoot.s3 ^
      "webSource" ! pathToRoot.s4 ^
      "userRoomsRoot" ! pathToRoot.s5 ^
      "UserExhibitRoom オブジェクト" ^
        "直下" ! pathToRoot.s6 ^
        "GroupRoom が親" ! pathToRoot.s7 ^
    bt ^ "update" ^
      "UserExhibitRoom オブジェクト" ^
        "名前の更新" ! update.s1 ^
        "roomSource#save 呼び出し" ! update.s2
  
  class TestBase {
    val structure = new MuseumStructure
    val roomSource = mock[TreeDataService[UserExhibitRoom]]
    structure.userExhibitRoomSource = roomSource
    
    val groupRoom = UserExhibitRoom("", GroupRoom)
    val basicRoom = UserExhibitRoom("", BasicRoom)
    val smartRoom = UserExhibitRoom("", SmartRoom)
  }
  
  val initial = new TestBase {
    def s1 = structure.isLeaf(structure.root) must beFalse
    
    def s2 = structure.isLeaf(structure.localSource) must beTrue
    
    def s3 = structure.isLeaf(structure.webSource) must beTrue
    
    def s4 = structure.isLeaf(structure.sourcesRoot) must beFalse
    
    def s5 = structure.isLeaf(structure.userRoomsRoot) must beFalse
    
    def s6 = structure.isLeaf(groupRoom) must beFalse
    
    def s7 = structure.isLeaf(basicRoom) must beTrue
    
    def s8 = structure.isLeaf(smartRoom) must beTrue
    
    def s9 = structure.childrenFor(structure.root) must
      contain(structure.sourcesRoot, structure.userRoomsRoot).inOrder
  }
  
  val childrenFor = new TestBase {
    val dummy1 = UserExhibitRoom("dummy1")
    val dummy2 = UserExhibitRoom("dummy2")
    roomSource.getChildren(Some(groupRoom)) returns List(dummy1, dummy2)
    roomSource.getChildren(Some(basicRoom)) returns List(dummy2)
    roomSource.getChildren(Some(smartRoom)) returns List(dummy1)
    
    val groupChildren = structure.childrenFor(groupRoom)
    val basicChildren = structure.childrenFor(basicRoom)
    val smartChildren = structure.childrenFor(smartRoom)
    
    val s1_1 = groupChildren must contain(dummy1, dummy2)
    val s1_2 = there was one(roomSource).getChildren(Some(groupRoom))
    def s1 = s1_1 and s1_2
    
    def s2 = basicChildren must_== Nil
    
    def s3 = smartChildren must_== Nil
  }
  
  val childrenForUserRooms = new TestBase {
    roomSource.getChildren(None) returns List(groupRoom, basicRoom, smartRoom)
    
    def s1 = structure.childrenFor(structure.userRoomsRoot) must
      contain(groupRoom, basicRoom, smartRoom)
  }
  
  val pathToRoot = new TestBase {
    val rootPath = IndexedSeq(structure.root)
    val sourcesPath = rootPath :+ structure.sourcesRoot
    val localSourcePath = sourcesPath :+ structure.localSource
    val webSourcePath = sourcesPath :+ structure.webSource
    val userRoomsPath = rootPath :+ structure.userRoomsRoot
    
    roomSource.getParent(basicRoom) returns None
    roomSource.getParent(groupRoom) returns None
    roomSource.getParent(smartRoom) returns Some(groupRoom)
    
    val basicRoomPath = userRoomsPath :+ basicRoom
    val smartRoomPath = userRoomsPath :+ groupRoom :+ smartRoom
    
    def s1 = structure.pathToRoot(structure.root) must_== rootPath
    
    def s2 = structure.pathToRoot(structure.sourcesRoot) must_== sourcesPath
    
    def s3 = structure.pathToRoot(structure.localSource) must_== localSourcePath
    
    def s4 = structure.pathToRoot(structure.webSource) must_== webSourcePath
    
    def s5 = structure.pathToRoot(structure.userRoomsRoot) must_== userRoomsPath
    
    def s6 = structure.pathToRoot(basicRoom) must_== basicRoomPath
    
    def s7 = structure.pathToRoot(smartRoom) must_== smartRoomPath
  }
  
  val update = new TestBase {
    val anyRoom = UserExhibitRoom("", GroupRoom)
    val userRoomsPath = structure.pathToRoot(structure.userRoomsRoot)
    
    structure.update(userRoomsPath :+ anyRoom, "new value")
    
    def s1 = anyRoom.name must_== "new value"
    
    def s2 = there was one(roomSource).save(anyRoom)
  }
}
