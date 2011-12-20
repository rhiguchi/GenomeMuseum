package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import java.awt.datatransfer.{Transferable, DataFlavor, Clipboard}
import javax.swing.{JTree, TransferHandler}
import javax.swing.tree.{TreeModel, TreePath}

import jp.scid.genomemuseum.model.{UserExhibitRoomService, UserExhibitRoom, ExhibitRoom,
  UserExhibitRoomServiceSpec}
import UserExhibitRoom.RoomType._

import GenomeMuseumControllerSpec.spyApplicationActionHandler

class ExhibitRoomListControllerSpec extends Specification with Mockito {
  private type Factory = (ApplicationActionHandler, JTree) => ExhibitRoomListController
  
  def is = "ExhibitRoomListController" ^
//    "ビュー" ^
//      "JTree" ^ viewSpec(controllerOf) ^
//    bt ^
//    "プロパティ設定" ^ propertiesSpec(controllerOf) ^
//    "選択された部屋" ^ selectedRoomSpec(controllerOf) ^
//    "BasicRoom 追加" ^ canAddBasicRoom(controllerOf) ^
//    "GroupRoom 追加" ^ canAddGroupRoom(controllerOf) ^
//    "SmartRoom 追加" ^ canAddSmartRoom(controllerOf) ^
//    "アクション" ^ actionsSpec(controllerOf) ^
    end
  
  def createController(handler: ApplicationActionHandler, tree: JTree) = {
    new ExhibitRoomListController(handler, tree)
  }
  
//  
//  def controllerOf(application: ApplicationActionHandler, view: JTree) =
//    new ExhibitRoomListController(application, view)
//  
//  def viewSpec(f: Factory) =
//    "ツリーモデルが設定される" ! tree(f).appliesModel ^
//    "ドラッグ可能" ! tree(f).dragEnabled ^
//    "転送ハンドラ設定" ! tree(f).transferHandler ^
//    bt
//  
//  def propertiesSpec(f: Factory) =
//    "データサービスの設定と取得" ! properties(f).userExhibitRoomService ^
//    "データサービスがソースリストモデルに適用" ! properties(f).userExhibitRoomServiceToModel ^
//    bt
//  
//  def selectedRoomSpec(f: Factory) =
//    "設定と取得" ! selectedRoom(f).appliesValue ^
//    bt
//  
//  def canAddBasicRoom(f: Factory) =
//    "サービスの addRoom を呼び出す" ! addBasicRoom(f).callsAddRoom ^
//    "選択されている GroupRoom が親となる" ! addBasicRoom(f).onParent ^
//    "選択部屋が GroupRoom では無い時は親の GroupRoom が親" ! addBasicRoom(f).onAncestor ^
//    "選択部屋が ExhibitRoom の時は親が None" ! addBasicRoom(f).noParent ^
//    "名前がすでに存在する時は 1 が後につく" ! addBasicRoom(f).newName ^
//    "名前がすでに存在する時の連番増加する" ! addBasicRoom(f).newNameIterate ^
//    "編集開始状態となる" ! addBasicRoom(f).startEditing ^
//    bt
//  
//  def canAddGroupRoom(f: Factory) =
//    "サービスの addRoom を呼び出す" ! addGroupRoom(f).callsAddRoom ^
//    "選択されている GroupRoom が親となる" ! addGroupRoom(f).onParent ^
//    "選択部屋が GroupRoom では無い時は親の GroupRoom が親" ! addGroupRoom(f).onAncestor ^
//    "選択部屋が ExhibitRoom の時は親が None" ! addGroupRoom(f).noParent ^
//    "名前がすでに存在する時は 1 が後につく" ! addGroupRoom(f).newName ^
//    "名前がすでに存在する時の連番増加する" ! addGroupRoom(f).newNameIterate ^
//    bt
//  
//  def canAddSmartRoom(f: Factory) =
//    "サービスの addRoom を呼び出す" ! addSmartRoom(f).callsAddRoom ^
//    "選択されている GroupRoom が親となる" ! addSmartRoom(f).onParent ^
//    "選択部屋が GroupRoom では無い時は親の GroupRoom が親" ! addSmartRoom(f).onAncestor ^
//    "選択部屋が ExhibitRoom の時は親が None" ! addSmartRoom(f).noParent ^
//    "名前がすでに存在する時は 1 が後につく" ! addSmartRoom(f).newName ^
//    "名前がすでに存在する時の連番増加する" ! addSmartRoom(f).newNameIterate ^
//    bt
//  
//  def actionsSpec(f: Factory) =
//    "BasicRoom 追加アクション" ! actions(f).addBasicRoom ^
//    bt
//  
//  // ファクトリーメソッド
//  def exhibitRoomOf(name: String) = {
//    val room = mock[ExhibitRoom]
//    room.name returns name
//    room
//  }
//  
//  def userRoomOf(roomType: RoomType) = {
//    val room = mock[UserExhibitRoom]
//    room.roomType returns roomType
//    room
//  }
//  
//  // テストクラス
//  class TestBase(f: Factory) {
//    val tree = spy(new JTree())
//    val ctrl = f(spyApplicationActionHandler, tree)
//    
//    lazy val service = UserExhibitRoomServiceSpec.makeMock(mock[UserExhibitRoomService])
//    
//    def sourceListModel = ctrl.sourceListModel
//  }
//  
//  class DataServiceReady(f: Factory) extends TestBase(f) {
//    ctrl.userExhibitRoomService = service
//  }
//  
//  def tree(f: Factory) = new TestBase(f) {
//    def appliesModel = tree.getModel must_== sourceListModel.treeModel
//    def dragEnabled = tree.getDragEnabled must beTrue
//    def transferHandler = tree.getTransferHandler must beAnInstanceOf[ExhibitRoomListTransferHandler]
//  }
//  
//  // プロパティ
//  def properties(f: Factory) = new DataServiceReady(f) {
//    def userExhibitRoomService = ctrl.userExhibitRoomService must_== service
//    
//    def userExhibitRoomServiceToModel =
//      ctrl.sourceListModel.dataService must_== service
//  }
//  
//  def selectedRoom(f: Factory) = new TestBase(f) {
//    val roomMock = mock[ExhibitRoom]
//    
//    def appliesValue = {
//      ctrl.selectedRoom := roomMock
//      ctrl.selectedRoom() must_== roomMock
//    }
//  }
//  
//  abstract class AddRoomTestBase(f: Factory, roomType: RoomType) extends DataServiceReady(f) {
//    val node = userRoomOf(SmartRoom)
//    val parent = userRoomOf(GroupRoom)
//    val roomAdded = userRoomOf(roomType)
//    service.getParent(node) returns Some(parent)
//    service.addRoom(any, any, any) returns roomAdded
//    
//    service.nameExists("defname") returns true
//    List("a", "a 1", "a 2") foreach (s => service.nameExists(s) returns true)
//    
//    def defaultName: String
//    def defaultName_=(name: String)
//    
//    def action(): Unit
//    
//    def callsAddRoom = there was addRoomCalledAfterAction()
//    
//    def addRoomCalledAfterAction(name: String = defaultName, parent: Option[UserExhibitRoom] = None) = {
//      action()
//      one(service).addRoom(roomType, name, parent)
//    }
//    
//    def onParent = {
//      ctrl.selectedRoom := parent
//      there was addRoomCalledAfterAction(parent = Some(parent))
//    }
//    
//    def onAncestor = {
//      ctrl.selectedRoom := node
//      there was addRoomCalledAfterAction(parent = Some(parent))
//    }
//    
//    def noParent = {
//      ctrl.selectedRoom := mock[ExhibitRoom]
//      there was addRoomCalledAfterAction()
//    }
//    
//    def newName = {
//      defaultName = "defname"
//      there was addRoomCalledAfterAction("defname 1")
//    }
//    
//    def newNameIterate = {
//      defaultName = "a"
//      there was addRoomCalledAfterAction("a 3")
//    }
//    
//    def startEditing = todo
//  }
// 
//  // アクション
//  // BasicRoom
//  def addBasicRoom(f: Factory) = new AddRoomTestBase(f, BasicRoom) {
//    def defaultName = sourceListModel.basicRoomDefaultName
//    
//    def defaultName_=(name: String) = sourceListModel.basicRoomDefaultName = name
//    
//    def action = ctrl.addBasicRoom
//  }
//  
//  // GroupRoom
//  def addGroupRoom(f: Factory) = new AddRoomTestBase(f, GroupRoom) {
//    def defaultName = sourceListModel.groupRoomDefaultName
//    
//    def defaultName_=(name: String) = sourceListModel.groupRoomDefaultName = name
//    
//    def action = ctrl.addGroupRoom
//  }
//  
//  // SmartRoom
//  def addSmartRoom(f: Factory) = new AddRoomTestBase(f, SmartRoom) {
//    def defaultName = sourceListModel.smartRoomDefaultName
//    
//    def defaultName_=(name: String) = sourceListModel.smartRoomDefaultName = name
//    
//    def action = ctrl.addSmartRoom
//  }
//  
//  def actions(f: Factory) = new {
////    val tree = spy(new JTree())
////    val ctrl = spy(f(tree))
//    
//    def addBasicRoom = {
//      todo
////      ctrl.addBasicRoomAction must not beNull
//    }
//  }
}
