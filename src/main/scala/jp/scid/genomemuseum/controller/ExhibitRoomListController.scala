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
  MuseumStructure, UserExhibitRoomService, ExhibitRoomTransferData, MuseumExhibitService}
import jp.scid.genomemuseum.gui.MuseumSourceModel
import UserExhibitRoom.RoomType
import RoomType._

/**
 * 『部屋』の一覧をツリー上に表示し、また『部屋』の追加、編集、削除を行う操作オブジェクト。
 * 
 * @param roomService 部屋のモデル
 * @param view ツリー
 * @param loadManager ファイルの読み込み操作管理
 */
class ExhibitRoomListController(
  roomService: UserExhibitRoomService,
  view: JTree
) extends GenomeMuseumController {
  /**
   * 指定したモデルとビューからこのコントローラを作成する。
   * 
   * @param roomService 部屋のモデル
   * @param view ツリー
   * @param loadManager ファイルの読み込み操作管理
   * @param exhibitService 展示物の管理
   */
  def this(roomService: UserExhibitRoomService, view: JTree, loadManager: MuseumExhibitLoadManager,
      exhibitService: MuseumExhibitService) {
    this(roomService, view)
    this.loadManager = Option(loadManager)
    this.exhibitService = Option(exhibitService)
  }
  
  // モデル
  /** ソースリストのツリー構造 */
  val sourceStructure = new MuseumStructure(roomService)
  /** ソースリストのモデル */
  val sourceListModel = new MuseumSourceModel(sourceStructure)
  /** 現在選択されているパスモデル */
  val selectedRoom = new ValueHolder[ExhibitRoom](sourceStructure.localSource)
  
  // プロパティ
  /** 転入操作に用いられる、読み込み操作管理オブジェクト */
  var loadManager: Option[MuseumExhibitLoadManager] = None
  /** 転入操作に用いられる、展示物管理オブジェクト */
  var exhibitService: Option[MuseumExhibitService] = None
  
  // コントローラ
  /** ツリーの展開を管理するハンドラ */
  private val expansionCtrl = new ExhibitRoomListExpansionController(view, sourceListModel)
  /** 転送ハンドラ */
  protected[controller] val transferHandler: ExhibitRoomListTransferHandler = new MyTransferHandler
  
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
  def addBasicRoom() {
    val roomPath = sourceListModel.addRoom(BasicRoom)
    startEditingRoom(roomPath)
  }
  
  /** GroupRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addGroupRoom")
  def addGroupRoom() {
    val roomPath = sourceListModel.addRoom(GroupRoom)
    startEditingRoom(roomPath)
  }
  
  /** SmartRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addSmartRoom")
  def addSmartRoom() {
    val roomPath = sourceListModel.addRoom(SmartRoom)
    startEditingRoom(roomPath)
  }
  
  /** 選択中の UserExhibitRoom ノードを除去する */
  @Action(name="deleteSelectedRoom")
  def deleteSelectedRoom {
    sourceListModel.removeSelections()
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
    override def canMove(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) =
      sourceStructure.canMove(source, dest)
    
    override def moveUserExhibitRoom(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) = {
      sourceListModel.moveRoom(source, dest)
      true
    }
    
    override def importFiles(files: Seq[File], targetRoom: Option[UserExhibitRoom]) = {
      loadManager.foreach(m => files foreach m.loadExhibit)
      true
    }
    
    override def importExhibits(exhibits: Seq[MuseumExhibit], targetRoom: UserExhibitRoom) = {
      exhibitService.map { service =>
        exhibits map (_.asInstanceOf[service.ElementClass]) foreach
          (e => service.addElement(targetRoom, e))
        true
      }
      .getOrElse(false)
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
          case room: UserExhibitRoom => exhibitService match {
            case Some(exhibitService) => ExhibitRoomTransferData(room, exhibitService)
            case None => null
          }
          case _ => null
        }
        case _ => null
      }
    }
  }
  
  /** モデルと結合する */
  private def bindModels() {
    ExhibitRoomListController.bind(view, this)
    sourceListModel.sourceListSelectionMode = true
    expansionCtrl.update()
    
    // 選択部屋を保持するモデル
    sourceListModel.reactions += {
      case DataTreePathsSelectionChanged(_, _, newPaths) => newPaths.headOption match {
        case None => sourceListModel.selectPathLocalLibrary()
        case Some(selection) =>
          selectedRoom := selection.last.asInstanceOf[ExhibitRoom]
          updateActionAvailability()
      }
    }
  
    /** ローカルライブラリを選択状態にする。 */
    sourceListModel.selectPathLocalLibrary()
      
    // リソースの適用
    sourceStructure.basicRoomDefaultName = resourceMap.getString("basicRoom.defaultName")
    sourceStructure.groupRoomDefaultName = resourceMap.getString("groupRoom.defaultName")
    sourceStructure.smartRoomDefaultName = resourceMap.getString("smartRoom.defaultName")
  }
  
  bindModels()
}

private object ExhibitRoomListController {
  import javax.swing.DropMode
  private def bind(view: JTree, ctrl: ExhibitRoomListController) {
    DataTreeModel.bind(view, ctrl.sourceListModel)
    view setTransferHandler ctrl.transferHandler
    view.setDragEnabled(true)
    view.setDropMode(DropMode.ON)
    
    // アクションバインド
    view.getActionMap.put("delete", ctrl.removeSelectedUserRoomAction.peer)
  }
}

