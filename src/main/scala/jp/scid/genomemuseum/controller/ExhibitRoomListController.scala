package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTree, JComponent}
import javax.swing.TransferHandler.TransferSupport

import org.jdesktop.application.Action

import jp.scid.gui.{ValueHolder, tree, event}
import tree.DataTreeModel
import DataTreeModel.Path
import event.DataTreePathsSelectionChanged
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  MuseumStructure, UserExhibitRoomService, ExhibitRoomTransferData}
import jp.scid.genomemuseum.gui.MuseumSourceModel
import UserExhibitRoom.RoomType
import RoomType._

/**
 * 『部屋』の一覧をツリー上に表示し、また『部屋』の追加、編集、削除を行う操作オブジェクト。
 * 
 * 
 */
class ExhibitRoomListController(
  application: ApplicationActionHandler,
  view: JTree
) extends GenomeMuseumController(application) {
  // ビュー
  
  private def roomService = museumSchema.userExhibitRoomService
  /** 標準での選択部屋 */
  private def defaultSelectionPath = sourceListModel.pathForLocalLibrary
  
  /** ソースリストのツリー構造 */
  val sourceStructure = new MuseumStructure()
  sourceStructure.userExhibitRoomSource = museumSchema.userExhibitRoomService
  sourceStructure.basicRoomDefaultName = getResourceString("basicRoom.defaultName")
  sourceStructure.groupRoomDefaultName = getResourceString("groupRoom.defaultName")
  sourceStructure.smartRoomDefaultName = getResourceString("smartRoom.defaultName")
  
  /** ソースリストのモデル */
  private[controller] val sourceListModel = new MuseumSourceModel(sourceStructure)
  
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
  
  // コントローラ
  /** ツリーの展開を管理するハンドラ */
  private val expansionCtrl = new ExhibitRoomListExpansionController(view, sourceListModel)
  /** 転送ハンドラ */
  val transferHandler: ExhibitRoomListTransferHandler = new MyTransferHandler
  
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
    val roomPath = sourceListModel.addRoom(BasicRoom)
    startEditingRoom(roomPath)
  }
  
  /** GroupRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addGroupRoom")
  def addGroupRoom {
    val roomPath = sourceListModel.addRoom(GroupRoom)
    startEditingRoom(roomPath)
  }
  
  /** SmartRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addSmartRoom")
  def addSmartRoom {
    val roomPath = sourceListModel.addRoom(SmartRoom)
    startEditingRoom(roomPath)
  }
  
  /** 選択中の UserExhibitRoom ノードを除去し、ローカルライブラリを選択する */
  @Action(name="deleteSelectedRoom")
  def deleteSelectedRoom {
    sourceListModel.removeSelections()
    sourceListModel.selectPathLocalLibrary()
  }
  
  /** アクション状態を更新 */
  private def updateActionAvailability() {
    // ノード削除アクションの使用可不可
    removeSelectedUserRoomAction.enabled = selectedRoom().isInstanceOf[UserExhibitRoom]
  }
  
  /** 部屋の名前を編集状態にする。 */
  private def startEditingRoom(roomPath: Path[ExhibitRoom]) {
    import DataTreeModel.convertPathToTreePath
    view.startEditingAtPath(convertPathToTreePath(roomPath))
  }
  
  private class MyTransferHandler extends ExhibitRoomListTransferHandler {
    private def exhibitService = museumSchema.museumExhibitService
    
    override def canMove(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) =
      sourceStructure.canMove(source, dest)
    
    override def moveUserExhibitRoom(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) = {
      sourceListModel.moveRoom(source, dest)
      true
    }
    
    override def importFiles(files: Seq[File], targetRoom: Option[UserExhibitRoom]) = {
      files foreach loadManager.loadExhibit
      true
    }
    
    override def importExhibits(exhibits: Seq[MuseumExhibit], targetRoom: UserExhibitRoom) = {
      val service = exhibitService
      exhibits map (_.asInstanceOf[service.ElementClass]) foreach
        (e => service.addElement(targetRoom, e))
      true
    }
    
    override def getTargetRooom(ts: TransferSupport): Option[ExhibitRoom] = {
      ts.getComponent match {
        case `view` =>
          val loc = ts.getDropLocation.getDropPoint
          view.getPathForLocation(loc.x, loc.y) match {
            case null => None
            case path => path.getLastPathComponent match {
              case room: ExhibitRoom => Some(room)
              case _ => None
            }
          }
        case _ => None
      }
    }
    
    override def createTransferable(c: JComponent) = {
      c match {
        case `view` => selectedRoom() match {
          case room: UserExhibitRoom => ExhibitRoomTransferData(room, exhibitService)
          case _ => null
        }
        case _ => null
      }
    }
  }
  
  // モデル結合
  ExhibitRoomListController.bind(view, this)
  sourceListModel.sourceListSelectionMode = true
  expansionCtrl.update()
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

