package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler}
import javax.swing.tree.TreePath

import org.specs2._
import org.mockito.Matchers

import jp.scid.genomemuseum.model.{UserExhibitRoomService, UserExhibitRoom,
  MuseumExhibitService, UserExhibitRoomMock}
import UserExhibitRoom.RoomType._

class ExhibitRoomListControllerSpec extends Specification with mock.Mockito {
  private type Factory = UserExhibitRoomService => ExhibitRoomListController
  
  def is = "ExhibitRoomListController" ^
    "リソース" ^ resourcesSpec(createController) ^
    "JTree と結合" ^ canBindTree(createController) ^
    "ツリーモデル" ^ sourceListModelSpec(createController) ^
    "ツリー構造" ^ sourceStructureSpec(createController) ^
    "部屋の選択" ^ selectedRoomSpec(createController) ^
    "BasicRoom 追加" ^ canAddBasicRoom(createController) ^
    "GroupRoom 追加" ^ canAddGroupRoom(createController) ^
    "SmartRoom 追加" ^ canAddSmartRoom(createController) ^
    "部屋の削除" ^ canDeleteSelectedRoom(createController) ^
    "アクション" ^ actionsSpec(createController) ^
    "読み込み操作" ^ loadManagerSpec(createController) ^
    end
  
  def createController(roomService: UserExhibitRoomService) = {
    new ExhibitRoomListController(roomService)
  }
  
  private implicit def construct(f: Factory): ExhibitRoomListController = {
    createController(mockUserExhibitRoomService)
  }
  
  def resourcesSpec(f: Factory) =
    "basicRoom 名" ! resources(f).basicRoom ^
    "groupRoom 名" ! resources(f).groupRoom ^
    "smartRoom 名" ! resources(f).smartRoom ^
    bt
  
  def canBindTree(f: Factory) =
    "treeModel の適用" ! bindTree(f).treeModel ^
    "selectionModel の適用" ! bindTree(f).selectionModel ^
    "転送ハンドラの適用" ! bindTree(f).transferHandler ^
    "ドラッグ可能" ! bindTree(f).dragEnabled ^
    "dropMode" ! bindTree(f).dropMode ^
    "削除アクション設定" ! bindTree(f).actionMapDelete ^
    "開閉ハンドラが設定される" ! todo ^
    bt
  
  def sourceListModelSpec(f: Factory) =
    "ソースが設定される" ! sourceListModel(f).appliedSource ^
    "ローカルライブラリが選択されている" ! sourceListModel(f).localSourceSelected ^
    "無選択とすると、ローカルライブラリが選択される" ! sourceListModel(f).selectsLocalSourceOnSelectEmpty ^
    bt
  
  def selectedRoomSpec(f: Factory) =
    "最初はローカルライブラリ" ! selectedRoom(f).initial ^
    "sourceListModel の値を反映" ! selectedRoom(f).boundSourceListModel ^
    bt
  
  def sourceStructureSpec(f: Factory) =
    "roomSource が設定される" ! sourceStructure(f).userExhibitRoomSource ^
    "basicRoomDefaultName が設定される" ! sourceStructure(f).basicRoomDefaultName ^
    "groupRoomDefaultName が設定される" ! sourceStructure(f).groupRoomDefaultName ^
    "smartRoomDefaultName が設定される" ! sourceStructure(f).smartRoomDefaultName ^
    bt
  
  def canAddBasicRoom(f: Factory) =
    "roomService の addRoom を呼び出す" ! addBasicRoom(f).toRoomService ^
    "新しいノードを編集開始する" ! addBasicRoom(f).startEditing ^
    bt
  
  def canAddGroupRoom(f: Factory) =
    "roomService の addRoom を呼び出す" ! addGroupRoom(f).toRoomService ^
    "新しいノードを編集開始する" ! addGroupRoom(f).startEditing ^
    bt
  
  def canAddSmartRoom(f: Factory) =
    "roomService の addRoom を呼び出す" ! addSmartRoom(f).toRoomService ^
    "新しいノードを編集開始する" ! addSmartRoom(f).startEditing ^
    bt
  
  def canDeleteSelectedRoom(f: Factory) =
    "選択ノードが削除される" ! deleteSelectedRoom(f).deletesFromService ^
    "ローカルライブラリノードが選択される" ! deleteSelectedRoom(f).selectsLocalLibrary ^
    bt
  
  def actionsSpec(f: Factory) =
    "addBasicRoomAction" ! actions(f).addBasicRoom ^
    "addGroupRoomAction" ! actions(f).addGroupRoom ^
    "addSamrtRoomAction" ! actions(f).addSmartRoom ^
    "removeSelectedUserRoomAction" ! actions(f).deleteSelectedRoom ^
    "removeSelectedUserRoomAction は最初は使用不可" ! actions(f).deleteSelectedRoomNotEnabled ^
    bt
  
  def loadManagerSpec(f: Factory) =
    "初期状態は None" ! loadManager(f).init ^
    "設定と取得" ! loadManager(f).setAndGet ^
    bt
  
  def mockUserExhibitRoomService = {
    val service = mock[UserExhibitRoomService]
    service.getChildren(any) returns Iterable.empty
    service.getParent(any) returns None
    service
  }
  
  class TestBase(f: Factory) {
    val service = mockUserExhibitRoomService
    val room = UserExhibitRoomMock.of(SmartRoom)
    service.addRoom(any, anyString, any) returns room
    
    val tree = spy(new JTree)
    val ctrl = f(service)
  }
  
  // リソース
  def resources(ctrl: ExhibitRoomListController) = new {
    def basicRoom = ctrl.basicRoomDefaultNameResource.key must_== "basicRoom.defaultName"
    def groupRoom = ctrl.groupRoomDefaultNameResource.key must_== "groupRoom.defaultName"
    def smartRoom = ctrl.smartRoomDefaultNameResource.key must_== "smartRoom.defaultName"
  }
  
  // 結合
  def bindTree(ctrl: ExhibitRoomListController) = new {
    val tree = spy(new JTree)
    ctrl.bindTree(tree)
    
    def treeModel = there was one(tree).setModel(ctrl.sourceListModel.treeModel)
    def selectionModel = there was one(tree).setSelectionModel(ctrl.sourceListModel.selectionModel)
    def transferHandler = there was one(tree).setTransferHandler(ctrl.transferHandler)
    def dragEnabled = there was one(tree).setDragEnabled(true)
    def dropMode = there was one(tree).setDropMode(javax.swing.DropMode.ON)
    def actionMapDelete = tree.getActionMap.get("delete") must_==
      ctrl.removeSelectedUserRoomAction.peer
  }
  
  // ツリーモデル
  def sourceListModel(ctrl: ExhibitRoomListController) = new {
    def localSourceSelected = ctrl.sourceListModel.selectedPath must
      beSome(ctrl.sourceListModel.pathForLocalLibrary)
    
    def appliedSource = ctrl.sourceListModel.treeSource must_== ctrl.sourceStructure
    
    def selectsLocalSourceOnSelectEmpty = {
      ctrl.sourceListModel.selectPaths(Nil)
      localSourceSelected
    }
  }
  
  // 選択部屋モデル
  def selectedRoom(ctrl: ExhibitRoomListController) = new {
    def initial = ctrl.selectedRoom() must_== ctrl.sourceStructure.localSource
    
    def boundSourceListModel = {
      val room = UserExhibitRoomMock.of(BasicRoom)
      ctrl.sourceListModel.selectPath(ctrl.sourceListModel.pathForUserRooms :+ room)
      ctrl.selectedRoom() must_== room
    }
  }
  
  // 構造
  def sourceStructure(f: Factory) = new TestBase(f) {
    def str = ctrl.sourceStructure
    def userExhibitRoomSource = str.roomService must_== service
    
    def basicRoomDefaultName = str.basicRoomDefaultName must_== ctrl.basicRoomDefaultNameResource()
    def groupRoomDefaultName = str.groupRoomDefaultName must_== ctrl.groupRoomDefaultNameResource()
    def smartRoomDefaultName = str.smartRoomDefaultName must_== ctrl.smartRoomDefaultNameResource()
  }
  
  // BasicRoom 追加
  def addBasicRoom(f: Factory) = new TestBase(f) {
    ctrl.bindTree(tree)
    ctrl.addBasicRoom()
    
    def toRoomService = there was one(service).addRoom(Matchers.eq(BasicRoom), anyString, any)
    
    def startEditing = there was one(tree).startEditingAtPath(new TreePath(
      (ctrl.sourceListModel.pathForUserRooms :+ room).toArray[Object]))
  }
  
  // GroupRoom 追加
  def addGroupRoom(f: Factory) = new TestBase(f) {
    ctrl.bindTree(tree)
    ctrl.addGroupRoom()
    
    def toRoomService = there was one(service).addRoom(Matchers.eq(GroupRoom), anyString, any)
    
    def startEditing = there was one(tree).startEditingAtPath(new TreePath(
      (ctrl.sourceListModel.pathForUserRooms :+ room).toArray[Object]))
  }
  
  // SmartRoom 追加
  def addSmartRoom(f: Factory) = new TestBase(f) {
    ctrl.bindTree(tree)
    ctrl.addSmartRoom()
    
    def toRoomService = there was one(service).addRoom(Matchers.eq(SmartRoom), anyString, any)
    
    def startEditing = there was one(tree).startEditingAtPath(new TreePath(
      (ctrl.sourceListModel.pathForUserRooms :+ room).toArray[Object]))
  }
  
  // 部屋削除
  def deleteSelectedRoom(ctrl: ExhibitRoomListController) = new {
    ctrl.deleteSelectedRoom
    
    def deletesFromService = todo
    def selectsLocalLibrary = ctrl.selectedRoom() must_== ctrl.sourceStructure.localSource
  }
  
  // アクション
  def actions(ctrl: ExhibitRoomListController) = new {
    def addBasicRoom = ctrl.addBasicRoomAction.name must_== "addBasicRoom"
    def addGroupRoom = ctrl.addGroupRoomAction.name must_== "addGroupRoom"
    def addSmartRoom = ctrl.addSamrtRoomAction.name must_== "addSmartRoom"
    def deleteSelectedRoom = ctrl.removeSelectedUserRoomAction
      .name must_== "deleteSelectedRoom"
    def deleteSelectedRoomNotEnabled = ctrl.removeSelectedUserRoomAction.enabled must beFalse
  }
  
  // loadManager プロパティ
  def loadManager(ctrl: ExhibitRoomListController) = new {
    def init = ctrl.loadManager must beNone
    def setAndGet = {
      val mng = mock[MuseumExhibitLoadManager]
      ctrl.loadManager = Some(mng)
      ctrl.loadManager must beSome(mng)
    }
  }
}
