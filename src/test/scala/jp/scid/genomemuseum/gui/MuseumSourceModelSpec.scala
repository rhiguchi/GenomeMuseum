package jp.scid.genomemuseum.gui

import javax.swing.event.{TreeModelListener, TreeModelEvent}
import collection.mutable.Publisher
import collection.script.{Message, Include, Remove, Reset, Script, Update}

import org.specs2._
import jp.scid.gui.tree.DataTreeModel.Path
import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitRoom,
     UserExhibitRoomService, UserExhibitRoom, UserExhibitRoomServiceMock,
     UserExhibitRoomMock}
import UserExhibitRoom.RoomType._

class MuseumSourceModelSpec extends Specification with mock.Mockito {
  private type Factory = MuseumStructure => MuseumSourceModel
  
  def is = "MuseumSourceModel" ^
    "パスの取得" ^ pathsSpec(createModel) ^
    "部屋の追加" ^ canAddRoom(createModel) ^
    "部屋の移動" ^ canMoveRoom(createModel) ^
    "選択された部屋の削除" ^ canRemoveSelections(createModel) ^
    "追加イベントの発行" ^ canPublishInsertEvent(createModel) ^
    "移動イベントの発行" ^ canPublishMoveEvent(createModel) ^
    "削除イベントの発行" ^ canPublishRemoveEvent(createModel) ^
    "ソースの変化監視" ^ sourceEventSpec(createModel) ^
    end
  
  def createModel(structure: MuseumStructure) =
    new MuseumSourceModel(structure)
  
  def pathsSpec(f: Factory) =
    "ローカルライブラリのパス" ! pathFor(f).localLibrary ^
    "ユーザー部屋のパス" ! pathFor(f).userRooms ^
    bt
  
  def canAddRoom(f: Factory) =
    "ソースに追加" ! addRoom(f).toSource ^
    "選択された親に追加" ! addRoom(f).withParent ^
    bt

  def canMoveRoom(f: Factory) =
    "ソースに移動を委譲" ! moveRoom(f).bySource ^
    bt

  def canRemoveSelections(f: Factory) =
    "ソースに削除を委譲" ! removeSelections(f).bySource ^
    bt
  
  def canPublishInsertEvent(f: Factory) =
    "イベントを発行" ! treeNodesInserted(f).isPublished ^
    "パス要素" ! treeNodesInserted(f).eventPath ^
    "子インデックス" ! treeNodesInserted(f).eventIndices ^
    "子要素" ! treeNodesInserted(f).eventChildren ^
    bt
  
  def canPublishMoveEvent(f: Factory) =
    "イベントの発行" ! moveEvent(f).isPublished ^
    "パス要素" ! moveEvent(f).eventPath ^
    "子インデックス" ! moveEvent(f).eventIndices ^
    "子要素" ! moveEvent(f).eventChildren ^
    bt
  
  def canPublishRemoveEvent(f: Factory) =
    "イベントの発行" ! removeEvent(f).isPublished ^
    "パス要素" ! removeEvent(f).eventPath ^
    "子インデックス" ! removeEvent(f).eventIndices ^
    "子要素" ! removeEvent(f).eventChildren ^
    bt
  
  def sourceEventSpec(f: Factory) =
    "追加イベント" ! sourceEvent(f).include ^
    "更新イベント" ! sourceEvent(f).update ^
    "削除イベント" ! sourceEvent(f).remove ^
    bt
  
  class TestBase(f: Factory) {
    val service = UserExhibitRoomServiceMock.of()
    val source = spy(new MuseumStructure(service))
    val model = f(source)
  }
  
  /** ユーザー部屋のパス */
  def pathFor(f: Factory) = new TestBase(f) {
    def userRooms = model.pathForUserRooms must_== source.pathToRoot(source.userRoomsRoot)
    
    def localLibrary = model.pathForLocalLibrary must_== source.pathToRoot(source.localSource)
  }
  
  /** 部屋の追加 */
  def addRoom(f: Factory) = new TestBase(f) {
    val room1, room2, room3 = UserExhibitRoomMock.of(BasicRoom)
    source.addRoom(BasicRoom, None) returns room1
    source.addRoom(GroupRoom, None) returns room2
    source.addRoom(SmartRoom, None) returns room3
    
    def ppath = model.pathForUserRooms
    
    def toSource = List(BasicRoom, GroupRoom, SmartRoom).map(model.addRoom) must_==
      List(ppath :+ room1, ppath :+ room2, ppath :+ room3)
    
    def withParent = {
      val parent1, parent2 = UserExhibitRoomMock.of(GroupRoom)
      
      source.addRoom(BasicRoom, Some(parent1)) returns room2
      source.addRoom(BasicRoom, Some(parent2)) returns room3
      
      model.selectPath(model.pathForUserRooms :+ parent1)
      val newPath1 = model.addRoom(BasicRoom)
      
      model.selectPath(model.pathForUserRooms :+ parent2 :+ room1)
      val newPath2 = model.addRoom(BasicRoom)
      
      Seq(newPath1, newPath2) must_== Seq(model.pathForUserRooms :+ parent1 :+ room2,
        model.pathForUserRooms :+ parent2 :+ room3)
    }
  }
  
  /** 部屋の追加 */
  def moveRoom(f: Factory) = new TestBase(f) {
    val parent1, parent2 = UserExhibitRoomMock.of(GroupRoom)
    doAnswer(arg => Unit).when(source).moveRoom(parent1, Some(parent2))
    
    def bySource = {
      model.moveRoom(parent1, Some(parent2))
      there was one(source).moveRoom(parent1, Some(parent2))
    }
  }
  
  /** 部屋の削除 */
  def removeSelections(f: Factory) = new TestBase(f) {
    val parent1, parent2 = UserExhibitRoomMock.of(GroupRoom)
    model.selectPath(model.pathForUserRooms :+ parent1 :+ parent2)
    model.removeSelections()
    
    def bySource = there was one(source).removeRoom(parent2) then
      one(source).removeRoom(any)
  }
  
  /** イベントスペック基本クラス */
  class EventTestBase(f: Factory) extends TestBase(f) {
    def treeModel = model.treeModel
    def pathUR = model.pathForUserRooms
    
    // イベント捕獲
    var events = Vector.empty[TreeModelEvent]
    val listener = mock[TreeModelListener]
    doAnswer(e => events = events :+ e.asInstanceOf[TreeModelEvent]).when(listener).treeNodesInserted(any)
    doAnswer(e => events = events :+ e.asInstanceOf[TreeModelEvent]).when(listener).treeNodesRemoved(any)
    treeModel.addTreeModelListener(listener)
    
    // 要素
    val gRoom = UserExhibitRoomMock.of(GroupRoom)
    val room1, room2 = UserExhibitRoomMock.of(BasicRoom)
  }
  
  /** 挿入操作イベント */
  def treeNodesInserted(f: Factory) = new EventTestBase(f) {
    // ツリー構造
    source.childrenFor(source.userRoomsRoot) returns List(gRoom)
    // モデルが読み込む
    (pathUR :+ gRoom) foreach treeModel.getChildCount
    // ソースの模擬動作
    source.addRoom(BasicRoom, None) answers { _ =>
      source.childrenFor(source.userRoomsRoot) returns List(gRoom, room1)
      room1
    }
    source.addRoom(SmartRoom, Some(gRoom)) answers { _ =>
      source.childrenFor(gRoom) returns List(room2)
      room2
    }
    
    // 挿入実行
    model.addRoom(BasicRoom)
    model.selectPath(pathUR :+ gRoom)
    model.addRoom(SmartRoom)
    
    def isPublished = events must haveSize(2)
    
    def eventPath = events.map(_.getPath.toSeq) must_==
      Seq(pathUR, pathUR :+ gRoom)
    
    def eventIndices = events.flatMap(_.getChildIndices.toSeq) must_== Seq(1, 0)
    
    def eventChildren = events.flatMap(_.getChildren.toSeq) must_== Seq(room1, room2)
  }
  
  /** 移動操作イベント */
  def moveEvent(f: Factory) = new EventTestBase(f) {
    // ツリー構造
    source.childrenFor(source.userRoomsRoot) returns List(room1, gRoom)
    source.childrenFor(gRoom) returns List(room2)
    // モデルが読み込む
    (model.pathForUserRooms :+ gRoom) foreach treeModel.getChildCount
    // ソースの模擬動作
    source.moveRoom(room1, Some(gRoom)) answers { _ =>
      source.childrenFor(source.userRoomsRoot) returns List(gRoom)
      source.childrenFor(gRoom) returns List(room1, room2)
    }
    source.moveRoom(room2, None) answers { _ =>
      source.childrenFor(source.userRoomsRoot) returns List(gRoom, room2)
      source.childrenFor(gRoom) returns List(room1)
    }
    
    // 移動実行
    model.moveRoom(room1, Some(gRoom))
    model.moveRoom(room2, None)
    
    def isPublished = events must haveSize(4)
    
    def eventPath = events.map(_.getPath.toSeq) must_==
      Seq(pathUR, pathUR :+ gRoom, pathUR :+ gRoom, pathUR)
    
    def eventIndices = events.flatMap(_.getChildIndices.toSeq) must_==
      Seq(0, 0, 1, 1)
    
    def eventChildren = events.flatMap(_.getChildren.toSeq) must_==
      Seq(room1, room1, room2, room2)
  }
  
  /** 削除操作イベント */
  def removeEvent(f: Factory) = new EventTestBase(f) {
    // ツリー構造
    source.childrenFor(source.userRoomsRoot) returns List(room1, gRoom)
    source.childrenFor(gRoom) returns List(room2)
    // モデルが読み込む
    (model.pathForUserRooms :+ gRoom) foreach treeModel.getChildCount
    // ソースの模擬動作
     doAnswer{ _ => source.childrenFor(source.userRoomsRoot) returns List(gRoom) }
       .when(source).removeRoom(room1)
     doAnswer{ _ => source.childrenFor(gRoom) returns Nil }
       .when(source).removeRoom(room2)
    
    // 削除実行
    model.selectPath(pathUR :+ room1)
    model.removeSelections()
    model.selectPath(pathUR :+ gRoom :+ room2)
    model.removeSelections()
    
    def isPublished = events must haveSize(2)
    
    def eventPath = events.map(_.getPath.toSeq) must_==
      Seq(pathUR, pathUR :+ gRoom)
    
    def eventIndices = events.flatMap(_.getChildIndices.toSeq) must_==
      Seq(0, 0)
    
    def eventChildren = events.flatMap(_.getChildren.toSeq) must_==
      Seq(room1, room2)
  }
  
  /** イベント発行が外部から可能な MuseumStructure */
  class PublishableMuseumStructure(r: UserExhibitRoomService) extends MuseumStructure(r) {
    override def publish(event: Message[ExhibitRoom]) {
      super.publish(event)
    }
  }
  
  /** ソースイベント */
  def sourceEvent(f: Factory) = new {
    // 要素
    val gRoom = UserExhibitRoomMock.of(GroupRoom)
    val room1, room2 = UserExhibitRoomMock.of(BasicRoom)
    
    // モデルが読み込む
    val service = UserExhibitRoomServiceMock.of()
    val source = spy(new PublishableMuseumStructure(service))
    val model = f(source)
    source.childrenFor(source.userRoomsRoot) returns List(gRoom)
    source.childrenFor(gRoom) returns List(room2)
    
    model.treeModel.getRoot
    (model.pathForUserRooms :+ gRoom) foreach model.treeModel.getChildCount
    
    // イベント捕獲
    var events = Vector.empty[TreeModelEvent]
    val listener = mock[TreeModelListener]
    doAnswer(e => events = events :+ e.asInstanceOf[TreeModelEvent]).when(listener).treeNodesInserted(any)
    doAnswer(e => events = events :+ e.asInstanceOf[TreeModelEvent]).when(listener).treeNodesRemoved(any)
    doAnswer(e => events = events :+ e.asInstanceOf[TreeModelEvent]).when(listener).treeNodesChanged(any)
    model.treeModel.addTreeModelListener(listener)
    
    source.childrenFor(source.userRoomsRoot) returns List(gRoom, room1)
    
    def include = {
      source.publish(new Include(room1))
      events.headOption.map(e => (e.getChildIndices.toSeq, e.getChildren.toSeq, e.getPath.toSeq)) must
        beSome((Seq(1), Seq(room1), model.pathForUserRooms))
    }
    
    def update = {
      source.publish(new Update(room2))
      events.headOption.map(e => (e.getChildIndices.toSeq, e.getChildren.toSeq, e.getPath.toSeq)) must
        beSome((Seq(0), Seq(room2), model.pathForUserRooms :+ gRoom))
    }
    
    def remove = {
      source.publish(new Remove(gRoom))
      events.headOption.map(e => (e.getChildIndices.toSeq, e.getChildren.toSeq, e.getPath.toSeq)) must
        beSome((Seq(0), Seq(gRoom), model.pathForUserRooms))
    }
  }
}
