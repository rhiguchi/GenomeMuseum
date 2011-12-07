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
  private val transferHandler = new ExhibitRoomListTransferHandler(this)
  
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
    val newRoom = sourceListModel.addUserRoomToSelectedPath(BasicRoom)
    startEditingRoom(newRoom)
  }
  
  /** GroupRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addGroupRoom")
  def addGroupRoom {
    val newRoom = sourceListModel.addUserRoomToSelectedPath(GroupRoom)
    startEditingRoom(newRoom)
  }
  
  /** SmartRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addSmartRoom")
  def addSmartRoom {
    val newRoom = sourceListModel.addUserRoomToSelectedPath(SmartRoom)
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

import javax.swing.{TransferHandler, JComponent}
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor

/**
 * ExhibitRoomListController 用転送ハンドラ
 */
private[controller] class ExhibitRoomListTransferHandler(ctrl: ExhibitRoomListController) extends TransferHandler {
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  import MuseumExhibitTransferData.{dataFlavor => exhibitListDataFlavor}
  
  override def canImport(ts: TransferSupport) = {
    val destPath = ctrl.getImportingTargetPath(ts)
    lazy val localLibNode = ctrl.sourceStructure.localSource
    
    // ノードの転送
    var transferred = ts.isDataFlavorSupported(exhibitRoomDataFlavor) match {
      case true =>
        val sourcePath = ts.getTransferable.getTransferData(exhibitRoomDataFlavor)
          .asInstanceOf[ExhibitRoomTransferData].transferPath
      
        destPath.isEmpty match {
          case true => sourcePath.lastOption.map(_.isInstanceOf[UserExhibitRoom]).getOrElse(false)
          case false => ctrl.canMove(sourcePath, destPath)
        }
      case false => false
    }
    // 展示物の転送
    transferred = if (transferred) true
    else ts.isDataFlavorSupported(exhibitListDataFlavor) match {
      case true => destPath.lastOption match {
        case Some(UserExhibitRoom.RoomType(BasicRoom)) => true
        case _ => false
      }
      case false => false
    }
    // ファイルの転送
    transferred = if (transferred) true
    else ts.isDataFlavorSupported(javaFileListFlavor) match {
      case true => destPath.lastOption match {
        case Some(UserExhibitRoom.RoomType(BasicRoom)) => true
        case Some(`localLibNode`) => true
        case _ => false
      }
      case false => false
    }
    
    transferred
  }
  
  override def importData(ts: TransferSupport) = {
    val destPath = ctrl.getImportingTargetPath(ts)
    lazy val localLibNode = ctrl.sourceStructure.localSource
    
    if (ts.isDataFlavorSupported(exhibitRoomDataFlavor)) {
      val sourcePath = ts.getTransferable.getTransferData(exhibitRoomDataFlavor)
        .asInstanceOf[ExhibitRoomTransferData].transferPath
      val newPath = destPath.isEmpty match {
        case true => ctrl.movePath(sourcePath, ctrl.sourceStructure.pathToRoot(ctrl.sourceStructure.localSource))
        case false => ctrl.movePath(sourcePath, destPath)
      }
      newPath.nonEmpty
    }
    else if (ts.isDataFlavorSupported(exhibitListDataFlavor)) {
      destPath.lastOption match {
        case Some(room @ UserExhibitRoom.RoomType(BasicRoom)) =>
//          val exhibits = ts.getTransferable.getTransferData(exhibitListDataFlavor)
//            .asInstanceOf[ExhibitRoomTransferData].exhibits
//          ctrl.addExhibitsTo(room.asInstanceOf[UserExhibitRoom], exhibits)
          true
        case _ => false
      }
    }
    else if (ts.isDataFlavorSupported(javaFileListFlavor)) {
      // TODO
//      destPath.lastOption match {
//        case Some(room @ UserExhibitRoom.RoomType(BasicRoom)) =>
//          ctrl.importExhibitFilesTo()
//        case Some(`localLibNode`) =>
//        case _ => false
      false
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
        ExhibitRoomTransferDataImpl(room)
      case _ => null
    }
  }
  
  private object PathLastNode {
    def unapply[A](path: Path[A]): Option[A] =
      path.lastOption
  }
}

object ExhibitRoomTransferData {
  val dataFlavor = new DataFlavor(ExhibitRoomTransferData.getClass,
    "ExhibitRoomTransferData")
}

trait ExhibitRoomTransferData {
  def transferPath: IndexedSeq[ExhibitRoom]
}

/**
 * 転送オブジェクト
 */
case class ExhibitRoomTransferDataImpl(
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
  
  def transferPath: IndexedSeq[ExhibitRoom] = IndexedSeq.empty
}
