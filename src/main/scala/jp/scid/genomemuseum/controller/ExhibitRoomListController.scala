package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTree, JComponent}
import javax.swing.TransferHandler.TransferSupport

import org.jdesktop.application.Action

import jp.scid.gui.{ValueHolder, tree, event}
import tree.DataTreeModel
import DataTreeModel.Path
import event.{ValueChange, DataTreePathsSelectionChanged}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  MuseumStructure, UserExhibitRoomService, ExhibitRoomTransferData, MuseumExhibitService}
import jp.scid.genomemuseum.gui.MuseumSourceModel
import UserExhibitRoom.RoomType
import RoomType._

/**
 * 『部屋』の一覧をツリー上に表示し、また『部屋』の追加、編集、削除を行う操作オブジェクト。
 * 
 * @param roomService 部屋のモデル
 * @param loadManager ファイルの読み込み操作管理
 */
class ExhibitRoomListController(
  private[controller] val roomService: UserExhibitRoomService,
  private[controller] val loadManager: MuseumExhibitLoadManager
) extends GenomeMuseumController {
  // モデル
  /** BasicRoom 規定名リソース */
  def basicRoomDefaultNameResource = getResource("basicRoom.defaultName")
  /** GroupRoom 規定名リソース */
  def groupRoomDefaultNameResource = getResource("groupRoom.defaultName")
  /** SmartRoom 規定名リソース */
  def smartRoomDefaultNameResource = getResource("smartRoom.defaultName")
  
  /** ソースリストのツリー構造 */
  lazy val sourceStructure: MuseumStructure = new MuseumStructure(roomService) {
    // リソースの適用
    basicRoomDefaultName = basicRoomDefaultNameResource()
    groupRoomDefaultName = groupRoomDefaultNameResource()
    smartRoomDefaultName = smartRoomDefaultNameResource()
  }
  
  /** ソースリストのモデル */
  lazy val sourceListModel: MuseumSourceModel = new MuseumSourceModel(sourceStructure) {
    reactions += {
      case DataTreePathsSelectionChanged(_, _, newPaths) => newPaths.headOption match {
        case None => selectPathLocalLibrary()
        case Some(selection) =>
          selectedRoom := selection.last.asInstanceOf[ExhibitRoom]
          // ノード削除アクションの使用可不可
          removeSelectedUserRoomAction.enabled = selectedRoom().isInstanceOf[UserExhibitRoom]
      }
    }
    
    selectPathLocalLibrary()
    sourceListSelectionMode = true
  }
  
  /** 読み込みマネージャの取得 */
  def exhibitLoadManager = transferHandler.exhibitLoadManager
  
  /** 読み込みマネージャの設定 */
  def exhibitLoadManager_=(manager: Option[MuseumExhibitLoadManager]) {
    transferHandler.exhibitLoadManager = manager
  }
  
  /** 現在選択されているパスモデル */
  lazy val selectedRoom = new ValueHolder[ExhibitRoom](sourceStructure.localSource)
  /** 編集を開始するためのトリガーモデル */
  private lazy val nodeEditTrigger = new ValueHolder[Path[ExhibitRoom]](Path.empty)
  
  // コントローラ
  /** 転送ハンドラ */
  lazy val transferHandler = new ExhibitRoomListTransferHandler(sourceListModel)
  
  // アクション
  /** {@link addBasicRoom} のアクション */
  val addBasicRoomAction = getAction("addBasicRoom")
  /** {@link addGroupRoom} のアクション */
  val addGroupRoomAction = getAction("addGroupRoom")
  /** {@link addSmartRoom} のアクション */
  val addSamrtRoomAction = getAction("addSmartRoom")
  /** {@link deleteSelectedRoom} のアクション */
  val removeSelectedUserRoomAction = {
    val action = getAction("deleteSelectedRoom")
    action.enabled = false
    action
  }
  
  /** BasicRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addBasicRoom")
  def addBasicRoom() {
    nodeEditTrigger := sourceListModel.addRoom(BasicRoom)
  }
  
  /** GroupRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addGroupRoom")
  def addGroupRoom() {
    nodeEditTrigger := sourceListModel.addRoom(GroupRoom)
  }
  
  /** SmartRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addSmartRoom")
  def addSmartRoom() {
    nodeEditTrigger := sourceListModel.addRoom(SmartRoom)
  }
  
  /** 選択中の UserExhibitRoom ノードを除去する */
  @Action(name="deleteSelectedRoom")
  def deleteSelectedRoom {
    sourceListModel.removeSelections()
  }
  
  /**
   * JTree と操作を結合する。
   */
  def bindTree(tree: JTree) {
    DataTreeModel.bind(tree, sourceListModel)
    tree setTransferHandler transferHandler
    tree.setDragEnabled(true)
    tree.setDropMode(javax.swing.DropMode.ON)
    
    // アクションバインド
    tree.getActionMap.put("delete", removeSelectedUserRoomAction.peer)
    
    /** ツリーの展開を管理するハンドラ */
    new ExhibitRoomListExpansionController(tree, sourceListModel).update()
    
    // ノード編集用トリガー
    nodeEditTrigger.reactions += {
      case ValueChange(_, _, newPath: Path[_]) =>
        newPath.lastOption match {
          case Some(room: UserExhibitRoom) =>
            val treePath = DataTreeModel.convertPathToTreePath(newPath)
            tree.startEditingAtPath(treePath)
          case _ =>
        }
        nodeEditTrigger := Path.empty
    }
  }
  
  
}

private object ExhibitRoomListController {
}

