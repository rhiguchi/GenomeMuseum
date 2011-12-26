package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler}
import javax.swing.tree.TreePath

import org.specs2._
import org.mockito.Matchers

import jp.scid.genomemuseum.model.{UserExhibitRoomService, UserExhibitRoom,
  MuseumExhibitService, UserExhibitRoomMock}
import UserExhibitRoom.RoomType._

class ExhibitRoomListControllerSpec extends Specification with mock.Mockito {
  private type Factory = (UserExhibitRoomService, JTree) => ExhibitRoomListController
  
  def is = "ExhibitRoomListController" ^
    "ビュー - ツリー" ^ viewSpec(createController) ^
    "ツリーモデル" ^ sourceListModelSpec(createController) ^
    "ツリー構造" ^ sourceStructureSpec(createController) ^
    "BasicRoom 追加" ^ canAddBasicRoom(createController) ^
    "GroupRoom 追加" ^ canAddGroupRoom(createController) ^
    "SmartRoom 追加" ^ canAddSmartRoom(createController) ^
    "部屋の削除" ^ canDeleteSelectedRoom(createController) ^
    "アクション" ^ actionsSpec(createController) ^
    "読み込み操作" ^ loadManagerSpec(createController) ^
    "展示物サービスプロパティ" ^ exhibitServiceSpec(createController) ^
    end
  
  def createController(roomService: UserExhibitRoomService, tree: JTree) = {
    new ExhibitRoomListController(roomService, tree)
  }
  
  def viewSpec(f: Factory) =
    "モデルが設定される" ! viewTree(f).appliedModel ^
    "ドラッグ可能" ! viewTree(f).isDraggable ^
    "転送ハンドラが設定される" ! viewTree(f).hasTransferHandler ^
    "ローカルライブラリが選択されている" ! viewTree(f).localSourceSelected ^
    "無選択とすると、ローカルライブラリが選択される" ! viewTree(f).selectsLocalSourceOnSelectEmpty ^
    bt
  
  def sourceListModelSpec(f: Factory) =
    "ソースが設定される" ! sourceListModel(f).appliedSource ^
    bt
  
  def sourceStructureSpec(f: Factory) =
    "roomSource が設定される" ! sourceStructure(f).userExhibitRoomSource ^
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
    bt
  
  def loadManagerSpec(f: Factory) =
    "初期状態は None" ! loadManager(f).init ^
    "設定と取得" ! loadManager(f).setAndGet ^
    bt
  
  def exhibitServiceSpec(f: Factory) =
    "初期状態は None" ! exhibitService(f).init ^
    "設定と取得" ! exhibitService(f).setAndGet ^
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
    val ctrl = f(service, tree)
  }
  
  // ツリービュー
  def viewTree(f: Factory) = new TestBase(f) {
    def appliedModel = tree.getModel must_== ctrl.sourceListModel.treeModel
    
    def isDraggable = tree.getDragEnabled must beTrue
    
    def hasTransferHandler = tree.getTransferHandler must_== ctrl.transferHandler
    
    def localSourceSelected = tree.getSelectionPath must_== new TreePath(
      ctrl.sourceListModel.pathForLocalLibrary.toArray[Object])
    
    def selectsLocalSourceOnSelectEmpty = {
      tree.clearSelection
      tree.getSelectionPath must_== new TreePath(
        ctrl.sourceListModel.pathForLocalLibrary.toArray[Object])
    }
  }
  
  // ツリーモデル
  def sourceListModel(f: Factory) = new TestBase(f) {
    def appliedSource = ctrl.sourceListModel.treeSource must_== ctrl.sourceStructure
  }
  
  // 構造
  def sourceStructure(f: Factory) = new TestBase(f) {
    def userExhibitRoomSource = ctrl.sourceStructure.roomService must_== service
  }
  
  // BasicRoom 追加
  def addBasicRoom(f: Factory) = new TestBase(f) {
    ctrl.addBasicRoom()
    
    def toRoomService = there was one(service).addRoom(Matchers.eq(BasicRoom), anyString, any)
    
    def startEditing = there was one(tree).startEditingAtPath(new TreePath(
      (ctrl.sourceListModel.pathForUserRooms :+ room).toArray[Object]))
  }
  
  // GroupRoom 追加
  def addGroupRoom(f: Factory) = new TestBase(f) {
    ctrl.addGroupRoom()
    
    def toRoomService = there was one(service).addRoom(Matchers.eq(GroupRoom), anyString, any)
    
    def startEditing = there was one(tree).startEditingAtPath(new TreePath(
      (ctrl.sourceListModel.pathForUserRooms :+ room).toArray[Object]))
  }
  
  // SmartRoom 追加
  def addSmartRoom(f: Factory) = new TestBase(f) {
    ctrl.addSmartRoom()
    
    def toRoomService = there was one(service).addRoom(Matchers.eq(SmartRoom), anyString, any)
    
    def startEditing = there was one(tree).startEditingAtPath(new TreePath(
      (ctrl.sourceListModel.pathForUserRooms :+ room).toArray[Object]))
  }
  
  // 部屋削除
  def deleteSelectedRoom(f: Factory) = new TestBase(f) {
    ctrl.deleteSelectedRoom
    
    def deletesFromService = todo
    def selectsLocalLibrary = ctrl.selectedRoom() must_== ctrl.sourceStructure.localSource
  }
  
  // アクション
  def actions(f: Factory) = new TestBase(f) {
    def addBasicRoom = ctrl.addBasicRoomAction.name must_== "addBasicRoom"
    def addGroupRoom = ctrl.addGroupRoomAction.name must_== "addGroupRoom"
    def addSmartRoom = ctrl.addSamrtRoomAction.name must_== "addSmartRoom"
    def deleteSelectedRoom = ctrl.removeSelectedUserRoomAction
      .name must_== "deleteSelectedRoom"
  }
  
  // loadManager プロパティ
  def loadManager(f: Factory) = new TestBase(f) {
    def init = ctrl.loadManager must beNone
    def setAndGet = {
      val mng = mock[MuseumExhibitLoadManager]
      ctrl.loadManager = Some(mng)
      ctrl.loadManager must beSome(mng)
    }
  }
  
  // exhibitService プロパティ 
  def exhibitService(f: Factory) = new TestBase(f) {
    def init = ctrl.exhibitService must beNone
    def setAndGet = {
      val service = mock[MuseumExhibitService]
      ctrl.exhibitService = Some(service)
      ctrl.exhibitService must beSome(service)
    }
  }
}
