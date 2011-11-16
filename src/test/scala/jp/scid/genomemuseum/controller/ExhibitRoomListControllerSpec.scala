package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import java.awt.datatransfer.{Transferable, DataFlavor, Clipboard}
import javax.swing.{JTree, TransferHandler}
import javax.swing.tree.{TreeModel, TreePath}

import jp.scid.genomemuseum.model.{UserExhibitRoomService, UserExhibitRoom, ExhibitRoom}
import UserExhibitRoom.RoomType._

class ExhibitRoomListControllerSpec extends Specification with Mockito {
  def is = "ExhibitRoomListController" ^
    "ビューにモデルが設定される" ! s1 ^
    "アクション" ^
      "addBasicRoomAction" ^
        "tree の編集開始コール" ! addBasicRoomAction.s1 ^
        "選択状態" ! addBasicRoomAction.s2 ^
      bt ^ "addGroupRoomAction" ^
        "tree の編集開始コール" ! addGroupRoomAction.s1 ^
        "選択状態" ! addGroupRoomAction.s2 ^
      bt ^ "addSamrtRoomAction" ^
        "tree の編集開始コール" ! addSamrtRoomAction.s1 ^
        "選択状態" ! addSamrtRoomAction.s2 ^
      bt ^ "removeSelectedUserRoomAction" ^
        "service の remove コール" ! removeSelectedUserRoomAction.s1 ^
        "ローカルソース選択" ! removeSelectedUserRoomAction.s2 ^
      bt ^ "ドラッグ＆ドロップ (TransferHandler)" ^
        "ExhibitRoomTransferData が受け入れ可能" ! dnd.s6 ^
        "転送の受付" ! dnd.s1 ^
        "service の setParent コール" ! dnd.s2 ^
        "JTree イベントディスパッチ" ! dnd.s3 ^
        "ドロップ先が不明の時はユーザールートに追加" ! dnd.s4 ^
        "ドロップ先が GroupRoom ではない時は importData が false" ! dnd.s5 ^
        "クリップボード転送" ! export.s1 ^
    bt ^ bt ^ "selectedRoom" ^
      "選択された部屋が格納" ! selectedRoom.s1 ^
      "ローカルソース格納" ! selectedRoom.s2 ^
      "ウェブソース格納" ! selectedRoom.s3 ^
      "選択解除するとローカルソースが格納" ! selectedRoom.s4 ^
      "ライブラリノードを選択するとローカルソースが格納" ! selectedRoom.s5 ^
      "ユーザールームルートを選択するとローカルソースが格納" ! selectedRoom.s6
 
  class TestBase {
    val view = spy(new JTree(null.asInstanceOf[TreeModel]))
    view setEditable true
    
    val testRoom = UserExhibitRoom("room")
    val room1 = UserExhibitRoom("room1", GroupRoom)
    val room1_1 = UserExhibitRoom("room1_1", BasicRoom)
    val room1_2 = UserExhibitRoom("room1_2", GroupRoom)
    val room1_2_1 = UserExhibitRoom("room1_2_1", SmartRoom)
    
    // UserExhibitRoomService モック作成
    val service = mock[UserExhibitRoomService]
    // ルート
    service.getChildren(None) returns List(testRoom, room1)
    service.getParent(any) returns None
    // 1 階層
    val parent1 = Some(room1)
    service.getChildren(parent1) returns List(room1_1, room1_2)
    service.getParent(room1_1) returns parent1
    service.getParent(room1_2) returns parent1
    // 2 階層
    val parent1_2 = Some(room1_2)
    service.getChildren(parent1_2) returns List(room1_2_1)
    service.getParent(room1_2_1) returns parent1_2
    
    service.addRoom(any, any, any) returns testRoom
    
    // コントローラ作成
    val ctrl = new ExhibitRoomListController(view)
    ctrl.userExhibitRoomService = service
    
    def structure = ctrl.sourceStructure
    def sourceListModel = ctrl.sourceListModel
  }
  
  def initialState = new TestBase
  
  def s1 = initialState.view.getModel must not beNull
  
  trait AddRoomSpec { this: TestBase =>
    val path = new TreePath(structure.pathToRoot(testRoom).toArray[Object])
    def s1 = there was one(view).startEditingAtPath(path)
    def s2 = view.getSelectionPath must_== path
  }
  
  val addBasicRoomAction = new TestBase with AddRoomSpec {
    ctrl.addBasicRoomAction()
  }
  
  val addGroupRoomAction = new TestBase with AddRoomSpec {
    ctrl.addGroupRoomAction()
  }
  
  val addSamrtRoomAction = new TestBase with AddRoomSpec {
    ctrl.addSamrtRoomAction()
  }
  
  val removeSelectedUserRoomAction = new TestBase {
    val path = structure.pathToRoot(testRoom)
    sourceListModel.selectPath(path)
    ctrl.removeSelectedUserRoomAction()
    
    def s1 = there was one(service).remove(testRoom)
    def s2 = sourceListModel.selectedPath must_==
      Some(sourceListModel.pathForLocalLibrary)
  }
  
  val dnd = new TestBase {
    // イベント
    import javax.swing.event.{TreeModelListener, TreeModelEvent}
    
    var lastInsertedEvent: Option[TreeModelEvent] = None
    val listener = mock[TreeModelListener]
    listener.treeNodesInserted(any) answers { event =>
      lastInsertedEvent = Some(event.asInstanceOf[TreeModelEvent])
      event
    }
    view.getModel addTreeModelListener listener
    
    val room1_2TreePath = new TreePath(structure.pathToRoot(room1_2).toArray[Object])
    
    // 転送データ
    val dndObj = mock[Transferable]
    dndObj.getTransferDataFlavors returns Array(ExhibitRoomTransferData.dataFlavor)
    dndObj.isDataFlavorSupported(ExhibitRoomTransferData.dataFlavor) returns true
    dndObj.getTransferData(ExhibitRoomTransferData.dataFlavor) returns
      ExhibitRoomTransferData(testRoom)
    
    // ドロップ先設定
    ctrl.dropTarget = Some(room1_2)
    
    structure.pathToRoot(room1_2) foreach view.getModel.getChildCount
    service.getChildren(None) returns List(room1)
    service.getChildren(parent1_2) returns List(room1_2_1, testRoom)
    val importResult = transferHandler.importData(view, dndObj)
    val event1 = lastInsertedEvent
    lastInsertedEvent = None
    
    // ドロップ先なし
    ctrl.dropTarget = None
    service.getChildren(None) returns List(testRoom, room1_2_1, room1)
    dndObj.getTransferData(ExhibitRoomTransferData.dataFlavor) returns
      ExhibitRoomTransferData(room1_2_1)
    val importResult2 = transferHandler.importData(view, dndObj)
    val event2 = lastInsertedEvent
    
    // ドロップ先を BasicRoom
    ctrl.dropTarget = Some(room1_1)
    val importResult3 = transferHandler.importData(view, dndObj)
    
    // ドロップ先を SmartRoom
    ctrl.dropTarget = Some(room1_2_1)
    val importResult4 = transferHandler.importData(view, dndObj)
    
    def transferHandler = view.getTransferHandler
    
    def s6 = transferHandler.canImport(view, Array(ExhibitRoomTransferData.dataFlavor))
    
    def s1 = importResult must beTrue
    
    def s2 = there was one(service).setParent(testRoom, Some(room1_2))
    
    def s3_1 = event1 must beSome
    def s3_2 = event1.get.getTreePath must_== room1_2TreePath
    def s3_3 = event1.get.getChildren must_== Array(testRoom)
    def s3 = s3_1 and s3_2 and s3_3
    
    def s4_1 = there was one(service).setParent(room1_2_1, None)
    def s4_2 = event2 must beSome
    def s4_3 = event2.get.getPath.toList must_== sourceListModel.pathForUserRooms
    def s4 = s4_1 and s4_2 and s4_3
    
    def s5_1 = importResult3 must beFalse
    def s5_2 = importResult4 must beFalse
    def s5 = s5_1 and s5_2
  }
  
  val export = new TestBase {
    // 選択された行をクリップボード転送する
    sourceListModel.selectPath(structure.pathToRoot(testRoom))
    // クリップボード
    val testClipboard = new Clipboard("test")
    view.getTransferHandler.exportToClipboard(view, testClipboard, TransferHandler.MOVE)
    
    def s1 = testClipboard.getData(ExhibitRoomTransferData.dataFlavor) must
      beAnInstanceOf[ExhibitRoomTransferData]
  }
  
  val selectedRoom = new TestBase {
    import jp.scid.gui.tree.DataTreeModel.Path
    
    val roomSel = setlectPath(structure.pathToRoot(testRoom))
    val localSel = setlectPath(sourceListModel.pathForLocalLibrary)
    val websel = setlectPath(structure.pathToRoot(structure.webSource))
    val desel = {
      sourceListModel.deselect
      ctrl.selectedRoom()
    }
    val libSel = setlectPath(sourceListModel.pathForLibraries)
    val urSel = setlectPath(sourceListModel.pathForUserRooms)
    
    
    private def setlectPath(path: Path[ExhibitRoom]) = {
      sourceListModel.selectPath(path)
      ctrl.selectedRoom()
    }
    
    def s1 = roomSel must_== testRoom
    
    def s2 = libSel must_== structure.localSource
    
    def s3 = websel must_== structure.webSource
    
    def s4 = desel must_== structure.localSource
    
    def s5 = libSel must_== structure.localSource
    
    def s6 = urSel must_== structure.localSource
  }
}
