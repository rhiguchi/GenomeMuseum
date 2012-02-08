package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTree, JComponent}
import javax.swing.TransferHandler.TransferSupport

import org.jdesktop.application.Action

import jp.scid.gui.{ValueHolder, tree, event}
import jp.scid.gui.control.TreeController
import jp.scid.gui.model.TreeSource
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
 */
class ExhibitRoomListController extends TreeController[ExhibitRoom, MuseumStructure] {
  
  private val ctrl = GenomeMuseumController(this);
  // モデル
  /** BasicRoom 規定名リソース */
  def basicRoomDefaultNameResource = ctrl.getResource("basicRoom.defaultName")
  /** GroupRoom 規定名リソース */
  def groupRoomDefaultNameResource = ctrl.getResource("groupRoom.defaultName")
  /** SmartRoom 規定名リソース */
  def smartRoomDefaultNameResource = ctrl.getResource("smartRoom.defaultName")
  
  /** 読み込みマネージャの取得 */
  def exhibitLoadManager = transferHandler.exhibitLoadManager
  
  /** 読み込みマネージャの設定 */
  def exhibitLoadManager_=(manager: Option[MuseumExhibitLoadManager]) {
    transferHandler.exhibitLoadManager = manager
  }
  
  // コントローラ
  /** 転送ハンドラ */
  lazy val transferHandler = new ExhibitRoomListTransferHandler()
  
  // アクション
  /** {@link addBasicRoom} のアクション */
  val addBasicRoomAction = ctrl.getAction("addBasicRoom")
  /** {@link addGroupRoom} のアクション */
  val addGroupRoomAction = ctrl.getAction("addGroupRoom")
  /** {@link addSmartRoom} のアクション */
  val addSamrtRoomAction = ctrl.getAction("addSmartRoom")
  /** {@link deleteSelectedRoom} のアクション */
  val removeSelectedUserRoomAction = ctrl.getAction("deleteSelectedRoom")
  
  // ノード削除アクションの使用可不可
  private val deleteActionEnabledHandler = EventListHandler(getSelectedNodes) { nodes =>
    removeSelectedUserRoomAction.enabled = nodes.find(_.isInstanceOf[UserExhibitRoom]).nonEmpty
  }
  
  /** BasicRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addBasicRoom")
  def addBasicRoom() = addRoom(BasicRoom)
  
  /** GroupRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addGroupRoom")
  def addGroupRoom() = addRoom(GroupRoom)
  
  /** SmartRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addSmartRoom")
  def addSmartRoom() = addRoom(SmartRoom)
  
  /** 選択中の UserExhibitRoom ノードを除去する */
  @Action(name="deleteSelectedRoom")
  def deleteSelectedRoom {
    removeSelections()
  }
  
  /**
   * 新しいモデルには初期部屋名を設定する。
   */
  override protected def processPropertyChange(model: MuseumStructure, property: String) {
    super.processPropertyChange(model, property)
    
    model.basicRoomDefaultName = basicRoomDefaultNameResource()
    model.groupRoomDefaultName = groupRoomDefaultNameResource()
    model.smartRoomDefaultName = smartRoomDefaultNameResource()
  }
  
  /**
   * JTree と操作を結合する。
   */
  override def bindTree(tree: JTree) {
    super.bindTree(tree)
    
    tree setTransferHandler transferHandler
    tree.setDragEnabled(true)
    tree.setDropMode(javax.swing.DropMode.ON)
    
    // アクションバインド
    tree.getActionMap.put("delete", removeSelectedUserRoomAction.peer)
    
    /** ツリーの展開を管理するハンドラ */
//    new ExhibitRoomListExpansionController(tree, sourceListModel).update()
  }
  
  /**
   * 部屋をサービスに追加する。
   * 
   * @param roomType 部屋の種類
   * @return 新しい部屋までのパス
   * @see UserExhibitRoom
   */
  protected def addRoom(roomType: RoomType) {
    val parent = selectedElementList.headOption match {
      case Some(parent: UserExhibitRoom) => Some(parent)
      case _ => None
    }
    val newRoom = getModel.addRoom(roomType, parent)
    startEditingForElement(newRoom)
  }
  
  /**
   * 新しい親へ移動する
   * @param element 移動する要素
   * @param newParent 異動先となる親要素。ルート項目にする時は None 。
   * @throws IllegalArgumentException 指定した親が GroupRoom ではない時
   * @throws IllegalStateException 指定した親が要素自身か、子孫である時
   */
  protected def moveRoom(element: UserExhibitRoom, newParent: Option[UserExhibitRoom]) {
    getModel.moveRoom(element, newParent)
  }
  
  /**
   * 部屋を親から削除する。
   * {@code room} に子要素が存在する時は、その要素もサービスから除外される。
   * @param room 削除する要素
   */
  protected def removeSelections() {
    import collection.JavaConverters._
    selectedElementList.foreach {
      case room: UserExhibitRoom =>
        getModel.removeRoom(room)
      case _ =>
    }
  }
  
  /** ローカルライブラリノードへのパス */
//  def pathForLocalLibrary: Path[ExhibitRoom] = source.pathToRoot(source.localSource)
  
  /** ローカルライブラリノードを選択状態にする */
//  def selectPathLocalLibrary() {
//    selectPath(pathForLocalLibrary)
//  }
  
  private def selectedElementList = {
    import collection.JavaConverters._
    getSelectedNodes.asScala
  }
}

object EventListHandler {
  import ca.odell.glazedlists.EventList
  import jp.scid.gui.control.ListHandler
  
  def apply[E](eventList: EventList[E])(function: Seq[E] => Unit): ListHandler[E] = {
    val handler = new ListHandler[E] {
      override def processValueChange(modelList: java.util.List[E]) {
        import collection.JavaConverters._
        function apply modelList.asScala
      }
    }
    handler setModel eventList
    handler
  }
}

class EventListHandler[E] {
  
}

private object ExhibitRoomListController {
}

