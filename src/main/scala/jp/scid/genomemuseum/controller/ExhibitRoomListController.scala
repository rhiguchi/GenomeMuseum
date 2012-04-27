package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTree, JComponent}
import javax.swing.tree.TreePath

import org.jdesktop.application.Action

import jp.scid.gui.control.TreeController
import jp.scid.gui.control.tree.TreeExpansionController
import jp.scid.genomemuseum.model.{UserExhibitRoom, MuseumStructure, MuseumSpace,
  ExhibitMuseumSpace, ExhibitPavilionFloor, FreeExhibitRoomModel, FreeExhibitPavilion}
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
  
  /** BasicRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addBasicRoom")
  def addBasicRoom() = addRoom(BasicRoom)
  
  /** GroupRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addGroupRoom")
  def addGroupRoom() = addRoom(GroupRoom)
  
  /** SmartRoom 型の部屋を追加し、部屋名を編集開始状態にする */
  @Action(name="addSmartRoom")
  def addSmartRoom() = addRoom(SmartRoom)
  
  /**
   * 新しいモデルには初期部屋名を設定する。
   */
  override def setModel(model: MuseumStructure) {
    super.setModel(model)
    
    Option(model) foreach { model =>
      model.basicRoomDefaultName = basicRoomDefaultNameResource()
      model.groupRoomDefaultName = groupRoomDefaultNameResource()
      model.smartRoomDefaultName = smartRoomDefaultNameResource()
    }
  }
  
  /**
   * JTree と操作を結合する。
   */
  override def bindTree(tree: JTree) {
    super.bindTree(tree)
    
    tree setTransferHandler transferHandler
    tree.setDragEnabled(true)
    tree.setDropMode(javax.swing.DropMode.ON)
    
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
      case Seq(parent: ExhibitPavilionFloor, _*) => parent
      case Seq(_, parent: ExhibitPavilionFloor, _*) => parent
      case _ => getModel.freeExhibitPavilion.get
    }
    val newRoom = getModel.addRoom(roomType, parent)
    val newRoomPath = getModel.pathToRoot(newRoom)
    
    selectPath(newRoomPath.asJava)
    editPath(newRoomPath.asJava)
  }
  
  override def isSelectable(path: java.util.List[MuseumSpace]) =
    path.size > 2
  
  override def isDeletable(path: java.util.List[MuseumSpace]) = path.get(path.size - 1) match {
    case _: ExhibitMuseumSpace => true
    case _ => false
  }
  
  /** ファイルを読み込み */
  def importFile(files: List[File]) =
    exhibitLoadManager.map(_.loadExhibit(files)).map(_.nonEmpty).getOrElse(false)
  
  /** 部屋へファイルを読み込み */
  def importFile(files: List[File], room: FreeExhibitRoomModel) =
    exhibitLoadManager.map(_.loadExhibit(files, room)).map(_.nonEmpty).getOrElse(false)
  
  /** 自由展示棟を取得 */
  private[controller] def freeExhibitPavilion =
    Option(getModel).flatMap(_.freeExhibitPavilion)
  
  private def selectedPathList: List[IndexedSeq[MuseumSpace]] = {
    import collection.JavaConverters._
    
    getSelectedPathList.asScala.toList.map(_.asScala.toIndexedSeq)
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
