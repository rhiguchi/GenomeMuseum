package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler}
import javax.swing.tree.TreePath

import org.specs2._
import org.mockito.Matchers

import jp.scid.genomemuseum.model.{UserExhibitRoomService, UserExhibitRoom,
  MuseumExhibitService, UserExhibitRoomMock, MuseumStructure, MuseumFloor}
import UserExhibitRoom.RoomType._

class ExhibitRoomListControllerSpec extends Specification with mock.Mockito {
  def is = "ExhibitRoomListController" ^
    "リソース" ^ resourcesSpec(createController) ^
    "プロパティ" ^ propertiesSpec(createController) ^
    "JTree と結合" ^ canBindTree(createController) ^
    "ツリー構造" ^ sourceStructureSpec(createController) ^
    "部屋の選択" ^ selectedRoomSpec(createController) ^
    "BasicRoom 追加" ^ canAddBasicRoom(createController) ^
    "GroupRoom 追加" ^ canAddGroupRoom(createController) ^
    "SmartRoom 追加" ^ canAddSmartRoom(createController) ^
    "部屋の削除" ^ canDeleteSelectedRoom(createController) ^
    "アクション" ^ actionsSpec(createController) ^
    "転送ハンドラ" ^ transferHandlerSpec(createController) ^
    end
  
  def createController() = new ExhibitRoomListController()
  
  def resourcesSpec(f: => ExhibitRoomListController) =
    "basicRoom 名" ! resources(f).basicRoom ^
    "groupRoom 名" ! resources(f).groupRoom ^
    "smartRoom 名" ! resources(f).smartRoom ^
    bt
  
  def propertiesSpec(f: => ExhibitRoomListController) =
    "exhibitLoadManager 初期値" ! properties(f).exhibitLoadManagerInit ^
    "exhibitLoadManager 設定" ! properties(f).exhibitLoadManager ^
    bt
  
  def canBindTree(f: => ExhibitRoomListController) =
    "treeModel の適用" ! bindTree(f).treeModel ^
    "selectionModel の適用" ! bindTree(f).selectionModel ^
    "転送ハンドラの適用" ! bindTree(f).transferHandler ^
    "ドラッグ可能" ! bindTree(f).dragEnabled ^
    "dropMode" ! bindTree(f).dropMode ^
    "削除アクション設定" ! bindTree(f).actionMapDelete ^
    "開閉ハンドラが設定される" ! todo ^
    bt
  
  def selectedRoomSpec(f: => ExhibitRoomListController) =
//    "最初はローカルライブラリ" ! selectedRoom(f).initial ^
//    "sourceListModel の値を反映" ! selectedRoom(f).boundSourceListModel ^
    bt
  
  def sourceStructureSpec(f: => ExhibitRoomListController) =
//    "roomSource が設定される" ! sourceStructure(f).userExhibitRoomSource ^
//    "basicRoomDefaultName が設定される" ! sourceStructure(f).basicRoomDefaultName ^
//    "groupRoomDefaultName が設定される" ! sourceStructure(f).groupRoomDefaultName ^
//    "smartRoomDefaultName が設定される" ! sourceStructure(f).smartRoomDefaultName ^
    bt
  
  def canAddBasicRoom(f: => ExhibitRoomListController) =
    "モデルに BasicRoom を追加" ! addBasicRoom(f).toModel ^
    "新しいノードを編集開始する" ! addBasicRoom(f).startEditing ^
    bt
  
  def canAddGroupRoom(f: => ExhibitRoomListController) =
    "モデルに GroupRoom を追加" ! addGroupRoom(f).toModel ^
    "新しいノードを編集開始する" ! addGroupRoom(f).startEditing ^
    bt
  
  def canAddSmartRoom(f: => ExhibitRoomListController) =
    "モデルに SmartRoom を追加" ! addSmartRoom(f).toModel ^
    "新しいノードを編集開始する" ! addSmartRoom(f).startEditing ^
    bt
  
  def canDeleteSelectedRoom(f: => ExhibitRoomListController) =
    "選択ノードが削除される" ! deleteSelectedRoom(f).deletesFromService ^
//    "ローカルライブラリノードが選択される" ! deleteSelectedRoom(f).selectsLocalLibrary ^
    bt
  
  def actionsSpec(f: => ExhibitRoomListController) =
    "addBasicRoomAction" ! actions(f).addBasicRoom ^
    "addGroupRoomAction" ! actions(f).addGroupRoom ^
    "addSamrtRoomAction" ! actions(f).addSmartRoom ^
    "removeSelectedUserRoomAction" ! actions(f).deleteSelectedRoom ^
    "removeSelectedUserRoomAction は最初は使用不可" ! actions(f).deleteSelectedRoomNotEnabled ^
    bt
  
  def transferHandlerSpec(f: => ExhibitRoomListController) =
    "ExhibitRoomListTransferHandler インスタンス" ! transferHandler(f).instance ^
//    "loadManager を利用" ! transferHandler(f).loadManager ^
    "sourceListModel を利用" ! transferHandler(f).sourceListModel ^
    bt
  
  def mockUserExhibitRoomService() = {
    val service = mock[UserExhibitRoomService]
    service.getParent(any) returns None
    service
  }
  
  class TestBase(f: ExhibitRoomListController) {
    val service = mockUserExhibitRoomService
    val room = UserExhibitRoomMock.of(SmartRoom)
    service.addRoom(any, anyString, any) returns room
    
    val tree = spy(new JTree)
    val ctrl = f
  }
  
  // リソース
  def resources(ctrl: ExhibitRoomListController) = new {
    def basicRoom = ctrl.basicRoomDefaultNameResource.key must_== "basicRoom.defaultName"
    def groupRoom = ctrl.groupRoomDefaultNameResource.key must_== "groupRoom.defaultName"
    def smartRoom = ctrl.smartRoomDefaultNameResource.key must_== "smartRoom.defaultName"
  }
  
  // プロパティ
  def properties(ctrl: ExhibitRoomListController) = new {
    def exhibitLoadManagerInit = ctrl.exhibitLoadManager must beNone
    def exhibitLoadManager = {
      val manager = mock[MuseumExhibitLoadManager]
      ctrl.exhibitLoadManager = Some(manager)
      ctrl.exhibitLoadManager must beSome(manager)
    }
  }
  
  // 結合
  def bindTree(ctrl: ExhibitRoomListController) = new {
    val tree = spy(new JTree)
    ctrl.bindTree(tree)
    
    def treeModel = there was one(tree).setModel(ctrl.getTreeModel())
    def selectionModel = there was one(tree).setSelectionModel(ctrl.getTreeSelectionModel())
    def transferHandler = there was one(tree).setTransferHandler(ctrl.transferHandler)
    def dragEnabled = there was one(tree).setDragEnabled(true)
    // モックでチェックしようとすると、例外が発生するのでプロパティから検証
    def dropMode = tree.getDropMode must_== javax.swing.DropMode.ON
    def actionMapDelete = tree.getActionMap.get("delete") must_==
      ctrl.removeSelectedUserRoomAction.peer
  }
  

  
  // 選択部屋モデル
//  def selectedRoom(ctrl: ExhibitRoomListController) = new {
//    def initial = ctrl.selectedRoom() must_== ctrl.sourceStructure.localSource
//    
//    def boundSourceListModel = {
//      val room = UserExhibitRoomMock.of(BasicRoom)
//      ctrl.sourceListModel.selectPath(ctrl.sourceListModel.pathForUserRooms :+ room)
//      ctrl.selectedRoom() must_== room
//    }
//  }
  
  // 構造
//  def sourceStructure(f: Factory) = new TestBase(f) {
//    def str = ctrl.sourceStructure
////    def userExhibitRoomSource = str.roomService must_== service
//    
//    def basicRoomDefaultName = str.basicRoomDefaultName must_== ctrl.basicRoomDefaultNameResource()
//    def groupRoomDefaultName = str.groupRoomDefaultName must_== ctrl.groupRoomDefaultNameResource()
//    def smartRoomDefaultName = str.smartRoomDefaultName must_== ctrl.smartRoomDefaultNameResource()
//  }
  
  class AddRoomSpec(ctrl: ExhibitRoomListController) {
    val model = mock[MuseumStructure]
    val newRoom, root = mock[MuseumFloor]
    model.getValue returns root
    model.isLeaf(root) returns false
    model.getChildren(root) returns java.util.Arrays.asList(newRoom)
//    model.addRoom(any, any) returns newRoom
    model.pathToRoot(newRoom) returns Vector(root, newRoom)
    model.pathForLoalSource returns Vector.empty
    
    ctrl setModel model
    
    val tree = spy(new JTree)
    tree.startEditingAtPath(any) answers {_ => }
    ctrl.bindTree(tree)
  }
  
  // BasicRoom 追加
  def addBasicRoom(ctrl: ExhibitRoomListController) = new AddRoomSpec(ctrl) {
    ctrl.addBasicRoom()
    
    def toModel = there was one(model).addRoom(BasicRoom, None)
    
    def startEditing =
      there was one(tree).startEditingAtPath(new TreePath(Array[Object](root, newRoom)))
  }
  
  // GroupRoom 追加
  def addGroupRoom(ctrl: ExhibitRoomListController) = new AddRoomSpec(ctrl) {
    ctrl.addGroupRoom()
    
    def toModel = there was one(model).addRoom(GroupRoom, None)
    
    def startEditing =
      there was one(tree).startEditingAtPath(new TreePath(Array[Object](root, newRoom)))
  }
  
  // SmartRoom 追加
  def addSmartRoom(ctrl: ExhibitRoomListController) = new AddRoomSpec(ctrl) {
    ctrl.addSmartRoom()
    
    def toModel = there was one(model).addRoom(SmartRoom, None)
    
    def startEditing =
      there was one(tree).startEditingAtPath(new TreePath(Array[Object](root, newRoom)))
  }
  
  // 部屋削除
  def deleteSelectedRoom(ctrl: ExhibitRoomListController) = new {
    val model = mock[MuseumStructure]
    model.pathForLoalSource returns Vector.empty
    ctrl setModel model
    
    val room1, room2, room3 = mock[UserExhibitRoom]
//    Seq(room1, room2) foreach ctrl.getSelectedNodes.add
    ctrl.deleteSelectedRoom
    
    def deletesFromService = todo
//      there was one(model).removeRoom(room1) then one(model).removeRoom(room2) then
//        no(model).removeRoom(room3)
    
    def selectsLocalLibrary = todo // ctrl.selectedRoom() must_== ctrl.sourceStructure.localSource
  }
  
  // アクション
  def actions(ctrl: ExhibitRoomListController) = new {
    def addBasicRoom = ctrl.addBasicRoomAction.name must_== "addBasicRoom"
    def addGroupRoom = ctrl.addGroupRoomAction.name must_== "addGroupRoom"
    def addSmartRoom = ctrl.addSamrtRoomAction.name must_== "addSmartRoom"
    def deleteSelectedRoom =
      ctrl.removeSelectedUserRoomAction.name must_== "deleteSelectedRoom"
    def deleteSelectedRoomNotEnabled = ctrl.removeSelectedUserRoomAction.enabled must beFalse
  }
  
  // 転送ハンドラ
  def transferHandler(ctrl: ExhibitRoomListController) = new {
    def instance = ctrl.transferHandler must beAnInstanceOf[ExhibitRoomListTransferHandler] 
//    def loadManager = ctrl.transferHandler.loadManager must_== ctrl.loadManager
    def sourceListModel = todo //ctrl.transferHandler.sourceListModel must beSome(ctrl.sourceListModel)
  }
}
