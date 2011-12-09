package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.JTree
import javax.swing.TransferHandler.TransferSupport

import org.jdesktop.application.Action

import jp.scid.gui.{ValueHolder, tree, event}
import tree.DataTreeModel
import DataTreeModel.Path
import event.DataTreePathsSelectionChanged
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom,
  MuseumStructure, UserExhibitRoomService}
import jp.scid.genomemuseum.gui.MuseumSourceModel
import UserExhibitRoom.RoomType._

private[controller] class ExhibitRoomListController(
  view: JTree
) extends GenomeMuseumController {
  // ビュー
  
  /** 標準での選択部屋 */
  private def defaultSelectionPath = sourceListModel.pathForLocalLibrary
  
  /** ソースリストのツリー構造 */
  val sourceStructure = new MuseumStructure()
  /** ソースリストのモデル */
  private[controller] val sourceListModel = new MuseumSourceModel(sourceStructure)
  sourceListModel.basicRoomDefaultName = getResourceString("basicRoom.defaultName")
  sourceListModel.groupRoomDefaultName = getResourceString("groupRoom.defaultName")
  sourceListModel.smartRoomDefaultName = getResourceString("smartRoom.defaultName")
  
  /** 現在選択されているパスモデル */
  val selectedRoom = new ValueHolder(defaultSelectionPath.last) {
    selectLocalLibraryNode()
    
    // 選択状態を更新するリアクション
    sourceListModel.reactions += {
      case DataTreePathsSelectionChanged(_, newPaths, _) =>
      newPaths.headOption match {
        case None => selectLocalLibraryNode()
        case Some(selection) =>
          this := selection.last.asInstanceOf[ExhibitRoom]
          updateActionAvailability()
      }
    }
  
    /** ローカルライブラリを選択状態にする。 */
    private def selectLocalLibraryNode() =
      sourceListModel.selectPath(defaultSelectionPath)
  }
  
  // ドロップターゲット
  private[controller] var dropTarget: Option[ExhibitRoom] = None
  /** 転送元オブジェクト */
  private[controller] var transferSource: Option[ExhibitRoom] = None
  
  // コントローラ
  /** ツリーの展開を管理するハンドラ */
  private val expansionCtrl = new ExhibitRoomListExpansionController(view, sourceListModel)
  /** 転送ハンドラ */
  private val transferHandler = new ExhibitRoomListTransferHandler(sourceListModel)
  
  // アクション
  /** {@link addBasicRoom} のアクション */
  val addBasicRoomAction = getAction("addBasicRoom")
  /** {@link addGroupRoom} のアクション */
  val addGroupRoomAction = getAction("addGroupRoom")
  /** {@link addSmartRoom} のアクション */
  val addSamrtRoomAction = getAction("addSmartRoom")
  /** {@link deleteSelectedRoom} のアクション */
  val removeSelectedUserRoomAction = getAction("deleteSelectedRoom")
  
  /** BasicRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addBasicRoom")
  def addBasicRoom {
    val newRoom = sourceListModel.addUserExhibitRoom(BasicRoom, findInsertParent)
    startEditingRoom(newRoom)
  }
  
  /** GroupRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addGroupRoom")
  def addGroupRoom {
    val newRoom = sourceListModel.addUserExhibitRoom(GroupRoom, findInsertParent)
    startEditingRoom(newRoom)
  }
  
  /** SmartRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addSmartRoom")
  def addSmartRoom {
    val newRoom = sourceListModel.addUserExhibitRoom(SmartRoom, findInsertParent)
    startEditingRoom(newRoom)
  }
  
  /** 選択中の UserExhibitRoom ノードを除去し、ローカルライブラリを選択する */
  @Action(name="deleteSelectedRoom")
  def deleteSelectedRoom {
    sourceListModel.removeSelectedUserRoom()
    sourceListModel.selectPath(sourceListModel.pathForLocalLibrary)
  }
  
  // 公開プロパティ
  /** sourceListModel のデータソース取得 */
  def userExhibitRoomService: UserExhibitRoomService = sourceListModel.dataService
  
  /** sourceListModel のデータソース設定 */
  def userExhibitRoomService_=(newService: UserExhibitRoomService) {
    sourceListModel.dataService = newService
    expansionCtrl.update()
  }
  
  /** アクション状態を更新 */
  private def updateActionAvailability() {
    // ノード削除アクションの使用可不可
    removeSelectedUserRoomAction.enabled = selectedRoom().isInstanceOf[UserExhibitRoom]
  }
  
  /** 部屋作成時の親となるの部屋を返す */
  private def findInsertParent = selectedRoom() match {
    case room: UserExhibitRoom =>
      sourceStructure.pathToRoot(room).reverse.collectFirst{
        case e: UserExhibitRoom if e.roomType == GroupRoom => e}
    case _ => None
  }
  
  /** 部屋の名前を編集状態にする。 */
  private def startEditingRoom(room: UserExhibitRoom) {
    import DataTreeModel.convertPathToTreePath
    val path = sourceStructure.pathToRoot(room)
    view.startEditingAtPath(convertPathToTreePath(path))
  }
  
  private type RoomPath = IndexedSeq[ExhibitRoom]
  
  /** 転入先となるパスを取得する */
  protected[controller] def getImportingTargetPath(ts: TransferSupport): RoomPath = {
    IndexedSeq.empty
  }
  
  /** パスが移動可能であるか */
  protected[controller] def canMove(source: RoomPath, dest: RoomPath) = {
      // GroupRoom で転送元が自身もしくは祖先ではない、もしくはルートに移動なら許可
    false
  }
  
  /** パスを移動 */
  protected[controller] def movePath(source: RoomPath, dest: RoomPath): Option[RoomPath] = {
      // GroupRoom で転送元が自身もしくは祖先ではない、もしくはルートに移動なら許可
    None
  }
  
  /** 展示物をファイルから読み込み */
  protected[controller] def importExhibitFilesTo(room: UserExhibitRoom, files: List[File]) {
    // TODO
  }
  
  
  
  // モデル結合
  ExhibitRoomListController.bind(view, this)
  sourceListModel.sourceListSelectionMode = true
}

private object ExhibitRoomListController {
  private def bind(view: JTree, ctrl: ExhibitRoomListController) {
    DataTreeModel.bind(view, ctrl.sourceListModel)
    view setTransferHandler ctrl.transferHandler
    view.setDragEnabled(true)
    
    // アクションバインド
    view.getActionMap.put("delete", ctrl.removeSelectedUserRoomAction.peer)
  }
}

