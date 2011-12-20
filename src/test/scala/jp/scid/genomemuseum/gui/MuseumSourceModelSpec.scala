package jp.scid.genomemuseum.gui

import org.specs2._
import mock._
import jp.scid.gui.tree.DataTreeModel.Path
import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitRoom,
     UserExhibitRoomService, UserExhibitRoom}
import UserExhibitRoom.RoomType._

class MuseumSourceModelSpec extends Specification with Mockito {
  def is = "MuseumSourceModel" ^
//    "ローカルライブラリへのパス取得" ! s1 ^
//    "ライブラリノードへのパス取得" ! s2 ^
//    "ユーザールームルートへのパス取得" ! s3 ^
//    "findAncestorGroupRoom" ^
//      "GroupRoom のみ" ! s5 ^
//      "最終ノードが GroupRoom ではない" ! s6 ^
//      "最初と最終ノードが GroupRoom ではない" ! s7 ^
//      "GroupRoom を含まない" ! s8 ^
//    bt ^ "部屋の追加" ^
//      "サービス未設定時は例外" ! s4 ^
//      "サービスのメソッドコール" ! withService.s1 ^
//      "GroupRoom 親を指定して追加" ! withService.s3 ^
//      "GroupRoom でない親は例外" ! withService.s4 ^
//    bt ^ "選択パスに部屋追加" ^
//      "無選択時" ^
//        "サービスのメソッドコール" ! addUserRoom.s1 ^
//        "追加された部屋が選択される" ! addUserRoom.s2 ^
//      bt ^ "GroupRoom 選択時" ^
//        "サービスのメソッドコール" ! addUserRoomSel.s1 ^
//        "追加された部屋が選択される" ! addUserRoomSel.s2 ^
//      bt ^ "GroupRoom の子で BasicRoom 選択時" ^
//        "サービスのメソッドコール" ! addUserRoomSel2.s1 ^
//        "追加された部屋が選択される" ! addUserRoomSel2.s2 ^
//      bt ^ "同じ名前が存在するとき、名前に連番がつく" ! addUserRoomName.s1 ^
//    bt ^ "moveRoom" ^
//      "Group フォルダへ移動" ! moveRoom.s1 ^
//      "ルートへ移動" ! moveRoom.s2 ^
//      "GroupRoom 以外を親に指定で例外" ! moveRoom.s3 ^
//      "自分の子孫を指定すると例外" ! moveRoom.s4 ^
//    bt ^ "UserExhibitRoom 削除" ^
//      "サービスのメソッドコール" ! removeRoom.s1 ^
//    bt ^ "Swing Event 発行" ^
//      "addRoom to user rooms root" ! insertEvent.s1 ^
//      "addRoom to GroupRoom" ! insertEvent.s2 ^
//      "removeRoom from user rooms root" ! removeEvent.s1 ^
//      "removeRoom from GroupRoom" ! removeEvent.s2 ^
//      "moveRoom" ! moveEvent.s1
    end
//  class TestBase {
//    val structure = new MuseumStructure()
//    val model = new MuseumSourceModel(structure)
//  }
//  
//  // 初期状態
//  val initialState = new TestBase
//  
//  def s1 = pathTest(initialState.model.pathForLocalLibrary,
//    initialState.structure.localSource)
//  
//  def s2 = pathTest(initialState.model.pathForLibraries,
//    initialState.structure.sourcesRoot)
//  
//  def s3 = pathTest(initialState.model.pathForUserRooms,
//    initialState.structure.userRoomsRoot)
//  
//  def pathTest(path: Path[ExhibitRoom], lastNode: ExhibitRoom) = {
//    (path.head must_== initialState.structure.root) and
//    (path.last must_== lastNode)
//  }
//  
//  def s4 = initialState.model.addRoom(BasicRoom, "name", None) must
//    throwA[IllegalStateException]
//  
//  import MuseumSourceModel.findAncestorGroupRoom
//  val group = UserExhibitRoom("group", GroupRoom)
//  
//  def s5 = {
//    findAncestorGroupRoom(Path(group)) must_== Path(group)
//  }
//  
//  def s6 = {
//    findAncestorGroupRoom(Path(group, UserExhibitRoom("group", BasicRoom))) must_== Path(group)
//  }
//  
//  def s7 = {
//    val room = UserExhibitRoom("group", BasicRoom)
//    findAncestorGroupRoom(Path(room, group,
//      UserExhibitRoom("group", BasicRoom))) must_== Path(room, group)
//  }
//  
//  def s8 = {
//    findAncestorGroupRoom(Path(UserExhibitRoom("", BasicRoom),
//      UserExhibitRoom("", SmartRoom))) must beEmpty
//  }
//  
//  class WithService extends TestBase {
//    val service = mock[UserExhibitRoomService]
//    service.addRoom(any, any, any) returns UserExhibitRoom("room")
//    model.dataService = service
//  }
//  
//  // サービス付き
//  val withService = new WithService {
//    val newRoom = model.addRoom(GroupRoom, "group", None)
//    model.addRoom(BasicRoom, "basic", None)
//    model.addRoom(SmartRoom, "smart", None)
//    
//    val parent = Some(UserExhibitRoom("parent", GroupRoom))
//    model.addRoom(BasicRoom, "child", parent)
//    
//    def s1_1 = there was one(service).addRoom(BasicRoom, "basic", None)
//    def s1_2 = there was one(service).addRoom(SmartRoom, "smart", None)
//    def s1_3 = there was one(service).addRoom(GroupRoom, "group", None)
//    def s1 = s1_1 and s1_2 and s1_3
//    
//    def s3 = there was one(service).addRoom(BasicRoom, "child", parent) 
//    
//    def s4_1 = model.addRoom(BasicRoom, "", Some(UserExhibitRoom("parent",
//      SmartRoom))) must throwA[IllegalArgumentException] 
//    def s4_2 = model.addRoom(GroupRoom, "", Some(UserExhibitRoom("parent",
//      SmartRoom))) must throwA[IllegalArgumentException] 
//    def s4 = s4_1 and s4_2
//  }
//  
//  val addUserRoom = new WithService {
//    model.addUserRoomToSelectedPath(BasicRoom)
//    model.addUserRoomToSelectedPath(GroupRoom)
//    val newRoom = model.addUserRoomToSelectedPath(SmartRoom)
//    val selection = model.selectedPath
//    
//    def s1_1 = there was one(service)
//      .addRoom(BasicRoom, model.basicRoomDefaultName, None)
//    def s1_2 = there was one(service)
//      .addRoom(GroupRoom, model.groupRoomDefaultName, None)
//    def s1_3 = there was one(service)
//      .addRoom(SmartRoom, model.smartRoomDefaultName, None)
//    def s1 = s1_1 and s1_2 and s1_3
//    
//    def s2_1 = selection must beSome
//    def s2_2 = selection.get must_== (model.pathForUserRooms :+ newRoom)
//    def s2 = s2_1 and s2_2
//  }
//  
//  val addUserRoomSel = new WithService {
//    // GroupRoom を選択
//    val groupRoom = UserExhibitRoom("group", GroupRoom)
//    model.selectPath(model.pathForUserRooms :+ groupRoom)
//    val newRoom = model.addUserRoomToSelectedPath(BasicRoom)
//    
//    def s1 = there was one(service)
//      .addRoom(BasicRoom, model.basicRoomDefaultName, Some(groupRoom))
//    def s2 = model.selectedPath.get must_==
//      (model.pathForUserRooms :+ groupRoom :+ newRoom)
//  }
//  
//  val addUserRoomSel2 = new WithService {
//    // GroupRoom, BasicRoom を選択
//    val groupRoom = UserExhibitRoom("group", GroupRoom)
//    model.selectPath(model.pathForUserRooms :+ group :+
//      UserExhibitRoom("parent", BasicRoom))
//    val newRoom = model.addUserRoomToSelectedPath(BasicRoom)
//    
//    def s1 = there was one(service)
//      .addRoom(BasicRoom, model.basicRoomDefaultName, Some(groupRoom))
//    def s2 = model.selectedPath.get must_==
//      (model.pathForUserRooms :+ groupRoom :+ newRoom)
//  }
//  
//  val addUserRoomName = new WithService {
//    service.nameExists(model.basicRoomDefaultName) returns true
//    model.addUserRoomToSelectedPath(BasicRoom)
//    service.nameExists(model.basicRoomDefaultName + " 1") returns true
//    model.addUserRoomToSelectedPath(BasicRoom)
//    
//    def s1_1 = there was one(service)
//      .addRoom(BasicRoom, model.basicRoomDefaultName + " 1", None)
//    def s1_2 = there was one(service)
//      .addRoom(BasicRoom, model.basicRoomDefaultName + " 2", None)
//    def s1 = s1_1 and s1_2
//  }
//  
//  val moveRoom = new WithService {
//    val basicRoom = UserExhibitRoom("room1", BasicRoom)
//    val groupRoom = UserExhibitRoom("room2", GroupRoom)
//    val smartRoom = UserExhibitRoom("rooom4", SmartRoom)
//    val groupRoom2 = UserExhibitRoom("room3", GroupRoom)
//    
//    service.getParent(basicRoom) returns None
//    service.getParent(groupRoom) returns None
//    service.getParent(smartRoom) returns Some(groupRoom)
//    service.getParent(groupRoom2) returns Some(groupRoom)
//    
//    model.moveRoom(basicRoom, Some(groupRoom))
//    model.moveRoom(smartRoom, None)
//    
//    def s1 = there was one(service).setParent(basicRoom, Some(groupRoom))
//    
//    def s2 = there was one(service).setParent(smartRoom, None)
//    
//    def s3_1 = model.moveRoom(groupRoom, Some(basicRoom)) must
//      throwA[IllegalArgumentException]
//    def s3_2 = model.moveRoom(groupRoom, Some(smartRoom)) must
//      throwA[IllegalArgumentException]
//    def s3 = s3_1 and s3_2
//    
//    def s4_1 = model.moveRoom(groupRoom, Some(groupRoom)) must
//      throwA[IllegalStateException]
//    def s4_2 = model.moveRoom(groupRoom, Some(groupRoom2)) must
//      throwA[IllegalStateException]
//    def s4 = s4_1 and s4_2
//  }
//  
//  val removeRoom = new WithService {
//    val room = UserExhibitRoom("room")
//    service.getParent(room) returns None
//    
//    model.removeRoom(room)
//    
//    def s1 = there was one(service).remove(room)
//  }
//  
//  class EventBase extends WithService {
//    import javax.swing.event.{TreeModelListener, TreeModelEvent}
//    
//    var lastInsertedEvent: Option[TreeModelEvent] = None
//    var lastRemovedEvent: Option[TreeModelEvent] = None
//    val listener = mock[TreeModelListener]
//    model.treeModel addTreeModelListener listener
//    listener.treeNodesInserted(any) answers { event =>
//      lastInsertedEvent = Some(event.asInstanceOf[TreeModelEvent])
//      event
//    }
//    listener.treeNodesRemoved(any) answers { event =>
//      lastRemovedEvent = Some(event.asInstanceOf[TreeModelEvent])
//      event
//    }
//    
//    val child1 = UserExhibitRoom("new child1")
//    val child2 = UserExhibitRoom("new child2")
//    val child3 = UserExhibitRoom("new child3", GroupRoom)
//    service.getChildren(None) returns List(child1, child2, child3)
//    service.getParent(child1) returns None
//    service.getParent(child2) returns None
//    service.getParent(child3) returns None
//    
//    val child3_1 = UserExhibitRoom("child3 child", GroupRoom)
//    service.getChildren(Some(child3)) returns List(child3_1)
//    service.getParent(child2) returns None
//    service.getParent(child3_1) returns Some(child3)
//    
//    model.pathForUserRooms foreach model.treeModel.getChildCount
//    model.treeModel.getChildCount(child3)
//  }
//  
//  val insertEvent = new EventBase {
//    // ユーザールートに要素が追加したことを想定
//    val newChild = UserExhibitRoom("new child")
//    service.getChildren(None) returns List(child1, newChild, child2, child3)
//    model.addRoom(BasicRoom, "test", None)
//    
//    val s1_1 = lastInsertedEvent.get.getPath must_== model.pathForUserRooms.toArray
//    val s1_2 = lastInsertedEvent.get.getChildIndices must_== Array(1)
//    val s1_3 = lastInsertedEvent.get.getChildren must_== Array(newChild)
//    def s1 = s1_1 and s1_2 and s1_3
//    
//    // child3 に要素が追加したことを想定
//    val newChild2 = UserExhibitRoom("new child2")
//    service.getChildren(Some(child3)) returns List(newChild2, child3_1)
//    model.addRoom(BasicRoom, "test", Some(child3))
//    
//    val s2_1 = lastInsertedEvent.get.getPath must_==
//      (model.pathForUserRooms :+ child3).toArray
//    val s2_2 = lastInsertedEvent.get.getChildIndices must_== Array(0)
//    val s2_3 = lastInsertedEvent.get.getChildren must_== Array(newChild2)
//    def s2 = s2_1 and s2_2 and s2_3
//    
//  }
//  
//  val removeEvent = new EventBase {
//    // ユーザールートから要素が削除されたことを想定
//    service.getChildren(None) returns List(child1, child3)
//    model.removeRoom(child2)
//    
//    val s1_1 = lastRemovedEvent.get.getPath must_== model.pathForUserRooms.toArray
//    val s1_2 = lastRemovedEvent.get.getChildIndices must_== Array(1)
//    val s1_3 = lastRemovedEvent.get.getChildren must_== Array(child2)
//    def s1 = s1_1 and s1_2 and s1_3
//    
//    // child3 から要素が削除されたことを想定
//    service.getChildren(Some(child3)) returns Nil
//    model.removeRoom(child3_1)
//    
//    val s2_1 = lastRemovedEvent.get.getPath must_== (model.pathForUserRooms :+ child3).toArray
//    val s2_2 = lastRemovedEvent.get.getChildIndices must_== Array(0)
//    val s2_3 = lastRemovedEvent.get.getChildren must_== Array(child3_1)
//    def s2 = s2_1 and s2_2 and s2_3
//  }
//  
//  val moveEvent = new EventBase {
//    // 移動を想定
//    service.getChildren(None) returns List(child2, child3)
//    service.getChildren(Some(child3)) returns List(child3_1, child1)
//    model.moveRoom(child1, Some(child3))
//    
//    val s1_1 = lastRemovedEvent.get.getPath must_==
//      model.pathForUserRooms.toArray
//    val s1_2 = lastRemovedEvent.get.getChildIndices must_== Array(0)
//    val s1_3 = lastRemovedEvent.get.getChildren must_== Array(child1)
//    val s1_4 = lastInsertedEvent.get.getPath must_==
//      (model.pathForUserRooms :+ child3).toArray
//    val s1_5 = lastInsertedEvent.get.getChildIndices must_== Array(1)
//    val s1_6 = lastInsertedEvent.get.getChildren must_== Array(child1)
//    def s1 = s1_1 and s1_2 and s1_3 and s1_4 and s1_5 and s1_6
//  }
}
