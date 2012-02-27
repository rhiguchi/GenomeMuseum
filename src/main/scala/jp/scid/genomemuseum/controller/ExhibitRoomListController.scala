package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTree, JComponent}
import javax.swing.tree.TreePath

import org.jdesktop.application.Action

import jp.scid.gui.control.{TreeController, TreeExpansionController}
import jp.scid.genomemuseum.model.{UserExhibitRoom, MuseumStructure, MuseumSpace,
  ExhibitMuseumFloor, FreeExhibitRoomModel}
import UserExhibitRoom.RoomType
import RoomType._

/**
 * 『部屋』の一覧をツリー上に表示し、また『部屋』の追加、編集、削除を行う操作オブジェクト。
 */
class ExhibitRoomListController extends TreeController[MuseumSpace, MuseumStructure] {
  /** ファイルの読み込み処理を行うコントローラ */
  var exhibitLoadManager: Option[MuseumExhibitLoadManager] = None
  
  def this(structure: MuseumStructure) {
    this()
    
    setModel(structure)
  }
  
  private val ctrl = GenomeMuseumController(this);
  // モデル
  /** BasicRoom 規定名リソース */
  def basicRoomDefaultNameResource = ctrl.getResource("basicRoom.defaultName")
  /** GroupRoom 規定名リソース */
  def groupRoomDefaultNameResource = ctrl.getResource("groupRoom.defaultName")
  /** SmartRoom 規定名リソース */
  def smartRoomDefaultNameResource = ctrl.getResource("smartRoom.defaultName")
  
  // コントローラ
  /** 転送ハンドラ */
  val transferHandler = new ExhibitRoomListTransferHandler(this)
  
  private[controller] val expansionController = TreeExpansionController.newConstantDepthExpansionController(2)
  
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
  private val deleteActionEnabledHandler = EventListHandler(getSelectedPathList) { nodes =>
//    removeSelectedUserRoomAction.enabled = nodes.find(_.isInstanceOf[UserExhibitRoom]).nonEmpty
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
  override def setModel(model: MuseumStructure) {
    super.setModel(model)
    
    Option(model) foreach { model =>
      model.basicRoomDefaultName = basicRoomDefaultNameResource()
      model.groupRoomDefaultName = groupRoomDefaultNameResource()
      model.smartRoomDefaultName = smartRoomDefaultNameResource()
      
      selectLocalSource()
    }
  }
  
  /** ローカルソースを選択する */
  def selectLocalSource() = {
//    import collection.JavaConverters._
//    Option(getModel).foreach(model => setlectPathAsList(model.pathForLoalSource.asJava))
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
    expansionController.bind(tree)
  }
  
  /**
   * 部屋をサービスに追加する。
   * 
   * @param roomType 部屋の種類
   * @return 新しい部屋までのパス
   * @see UserExhibitRoom
   */
  protected def addRoom(roomType: RoomType) {
    import collection.JavaConverters._
    val parent = selectedPathList.headOption.map(_.reverse).getOrElse(IndexedSeq.empty) match {
      case Seq(parent: ExhibitMuseumFloor, _*) => Some(parent)
      case Seq(_, parent: ExhibitMuseumFloor, _*) => Some(parent)
      case _ => None
    }
    val newRoom = getModel.addRoom(roomType, parent)
    val newRoomPath = getModel.pathToRoot(newRoom)
    
    setlectPathAsList(newRoomPath.asJava)
    startEditingForElement(newRoom)
  }
  
  /**
   * 部屋を親から削除する。
   * {@code room} に子要素が存在する時は、その要素もサービスから除外される。
   * @param room 削除する要素
   */
  protected def removeSelections() {
//    selectedElementList.foreach {
//      case room: UserExhibitRoom =>
//        getModel.removeRoom(room)
//      case _ =>
//    }
  }
  
  /** ファイルを読み込み */
  def importFile(files: List[File]) =
    exhibitLoadManager.flatMap(_.loadExhibit(files)).nonEmpty
  
  /** 部屋へファイルを読み込み */
  def importFile(files: List[File], room: FreeExhibitRoomModel) =
    exhibitLoadManager.flatMap(_.loadExhibit(files, room)).nonEmpty
  
  private def selectedPathList: List[IndexedSeq[MuseumSpace]] = {
    import collection.JavaConverters._
    
    val list = getSelectedPathList
    list.getReadWriteLock.readLock.lock()
    val scalaPathList = try list.asScala.toList
    finally list.getReadWriteLock.readLock.unlock()
    
    scalaPathList.map(_.asScala.toIndexedSeq)
  }
}

object EventListHandler {
  import ca.odell.glazedlists.EventList
  import jp.scid.gui.control.ListHandler
  import collection.mutable
  
  def apply[E](eventList: EventList[E])(function: mutable.Buffer[E] => Unit): ListHandler[E] = {
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
