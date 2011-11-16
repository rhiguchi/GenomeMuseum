package jp.scid.genomemuseum.controller

import javax.swing.JTree

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
) {
  // ビュー
  
  /** 標準での選択部屋 */
  private def defaultSelectionPath = sourceListModel.pathForLocalLibrary
  
  /** ソースリストのツリー構造 */
  val sourceStructure = new MuseumStructure()
  /** ソースリストのモデル */
  private[controller] val sourceListModel = new MuseumSourceModel(sourceStructure)
  
  /** 現在選択されているパスモデル */
  val selectedRoom = new ValueHolder(defaultSelectionPath.last) {
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
  }
  
  // ドロップターゲット
  private[controller] var dropTarget: Option[ExhibitRoom] = None
  
  // コントローラ
  /** ツリーの展開を管理するハンドラ */
  private val expansionCtrl = new ExhibitRoomListExpansionController(view, sourceListModel)
  /** 転送ハンドラ */
  private val transferHandler = new ExhibitRoomListTransferHandler(this)
  
  // アクション
  /** BasicRoom 型の部屋を追加し、部屋名を編集開始状態にするアクション */
  val addBasicRoomAction = swing.Action("addBasicRoom") {
    val newRoom = sourceListModel.addUserRoomToSelectedPath(BasicRoom)
    startEditingRoom(newRoom)
  }
  
  /** GroupRoom 型の部屋を追加し、部屋名を編集開始状態にするアクション */
  val addGroupRoomAction = swing.Action("addGroupRoom") {
    val newRoom = sourceListModel.addUserRoomToSelectedPath(GroupRoom)
    startEditingRoom(newRoom)
  }
  
  /** SmartRoom 型の部屋を追加し、部屋名を編集開始状態にするアクション */
  val addSamrtRoomAction = swing.Action("addSmartRoom") {
    val newRoom = sourceListModel.addUserRoomToSelectedPath(SmartRoom)
    startEditingRoom(newRoom)
  }
  
  /** 選択中の UserExhibitRoom ノードを除去し、ローカルライブラリを選択するアクション */
  val removeSelectedUserRoomAction = swing.Action("deleteSelectedBox") {
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
  
  /** 部屋の名前を編集状態にする。 */
  private def startEditingRoom(room: UserExhibitRoom) {
    import DataTreeModel.convertPathToTreePath
    val path = sourceStructure.pathToRoot(room)
    view.startEditingAtPath(convertPathToTreePath(path))
  }
  
  /** ローカルライブラリを選択状態にする。 */
  private def selectLocalLibraryNode() =
    sourceListModel.selectPath(defaultSelectionPath)
  
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

import javax.swing.{TransferHandler, JComponent}
import java.awt.datatransfer.{Transferable, DataFlavor}

/**
 * ExhibitRoomListController 用転送ハンドラ
 */
private class ExhibitRoomListTransferHandler(ctrl: ExhibitRoomListController) extends TransferHandler {
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  
  override def canImport(comp: JComponent, transferFlavors: Array[DataFlavor]) = {
    transferFlavors.contains(exhibitRoomDataFlavor)
  }
  
  override def importData(comp: JComponent, t: Transferable) = {
    if (t.isDataFlavorSupported(exhibitRoomDataFlavor)) {
      ctrl.dropTarget match {
        case Some(target @ UserExhibitRoom.RoomType(GroupRoom)) =>
          val data = t.getTransferData(exhibitRoomDataFlavor).asInstanceOf[ExhibitRoomTransferData]
          ctrl.sourceListModel.moveRoom(data.room, Some(target))
          true
        case None =>
          val data = t.getTransferData(exhibitRoomDataFlavor).asInstanceOf[ExhibitRoomTransferData]
          ctrl.sourceListModel.moveRoom(data.room, None)
          true
        case _ =>
          false
      }
    }
    else {
      false
    }
  }
  
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY_OR_MOVE
  
  override def createTransferable(c: JComponent) = {
    ctrl.sourceListModel.selectedPath match {
      case Some(PathLastNode(room: UserExhibitRoom)) =>
        ExhibitRoomTransferData(room)
      case _ => null
    }
  }
  
  private object PathLastNode {
    def unapply[A](path: Path[A]): Option[A] =
      path.lastOption
  }
}

/**
 * 転送オブジェクト
 */
case class ExhibitRoomTransferData(
  room: UserExhibitRoom
) extends Transferable {
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  
  def getTransferDataFlavors(): Array[DataFlavor] = {
    return Array(exhibitRoomDataFlavor)
  }
  
  def getTransferData(flavor: DataFlavor) = {
    flavor match {
      case `exhibitRoomDataFlavor` =>
        this
      case _ => null
    }
  }
  
  def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
    case `exhibitRoomDataFlavor` => true
    case _ => false
  }
}

object ExhibitRoomTransferData {
  val dataFlavor = new DataFlavor(ExhibitRoomTransferData.getClass,
    "ExhibitRoomTransferData")
}
