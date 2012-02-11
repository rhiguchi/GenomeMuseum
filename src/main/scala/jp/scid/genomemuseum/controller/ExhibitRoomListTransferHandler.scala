package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler, JComponent}
import javax.swing.tree.TreePath
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport

import jp.scid.genomemuseum.model.{MuseumExhibitService}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  GroupRoomContentsModel, MuseumExhibitListModel,
  MutableMuseumExhibitListModel}

object ExhibitRoomListTransferHandler {
  object ExhibitRoomTransferData {
    val dataFlavor = new DataFlavor(classOf[MuseumExhibitListModel],
        "MuseumExhibitListModel")
    
    def unapply(ts: TransferSupport): Option[MuseumExhibitListModel] = {
      ts.isDataFlavorSupported(dataFlavor) match {
        case true =>
          val data = ts.getTransferable.getTransferData(dataFlavor)
            .asInstanceOf[MuseumExhibitListModel]
          Some(data)
        case false => None
      }
    }
  }
  
  /**
   * 部屋用転送オブジェクト
   */
  class ExhibitRoomTransferData(roomContents: MuseumExhibitListModel)
      extends MuseumExhibitListTransferHandler.RoomContentExhibitsTransferData(roomContents) {
    override def getTransferDataFlavors(): Array[DataFlavor] =
      ExhibitRoomTransferData.dataFlavor +: super.getTransferDataFlavors
    
    override def getTransferData(flavor: DataFlavor) = flavor match {
      case ExhibitRoomTransferData.dataFlavor => roomContents
      case _ => super.getTransferData(flavor)
    }
    
    override def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
      case ExhibitRoomTransferData.dataFlavor => true
      case _ => super.isDataFlavorSupported(flavor)
    }
  }
}

/**
 * ExhibitRoomListController 用転送ハンドラ
 * 
 * {@code #sourceListModel} を指定することで、部屋の移動が可能になる。
 * @param loadManager 読み込み操作管理オブジェクト
 */
class ExhibitRoomListTransferHandler extends MuseumExhibitTransferHandler {
  import ExhibitRoomListTransferHandler._
  
  def this(controller: ExhibitRoomListController) {
    this()
    this.controller = Option(controller)
  }
  
  /** ツリーを操作する対象のコントローラ */
  var controller: Option[ExhibitRoomListController] = None
  
  /** 展示物の転入が可能な部屋を返す */
  def getExhibitTransferTarget(ts: TransferSupport) = {
    getTargetRoomContents(ts) collect {
      case room: MutableMuseumExhibitListModel => room
    }
  }
  
  /**
   * 転入先の部屋オブジェクトを取得する。
   */
  def getTargetRoomContents(ts: TransferSupport): Option[MuseumExhibitListModel] = {
    ts.getComponent match {
      case tree: JTree =>
        val loc = ts.getDropLocation.getDropPoint
        
        tree.getPathForLocation(loc.x, loc.y) match {
          case null => controller.flatMap(_.getLocalLibraryContent)
          case path => controller.flatMap(_.getContent(path))
        }
      case _ => None
    }
  }
  
  /**
   * 部屋の転入操作の可能性を返す。
   */
  override def canImport(ts: TransferSupport) = ts match {
    case ExhibitRoomTransferData(transferData) =>
      getTargetRoomContents(ts) match {
        // GroupRoom はRoomを転入できる
        // LocalLibrary にも転入できる（GroupRoomContentsModel を実装する）
        case Some(groupRoom: GroupRoomContentsModel) =>
          transferData.userExhibitRoom map groupRoom.canAddChild getOrElse false
        // BasicRoom には MuseumExhibit を転入できる
        case Some(basicRoom: MutableMuseumExhibitListModel) => true 
        case _ => false
      }
    case _ => super.canImport(ts)
  }
  
  /**
   * 部屋の転入操作をする。
   */
  override def importData(ts: TransferSupport) = ts match {
    case ExhibitRoomTransferData(transferData) =>
      getTargetRoomContents(ts) match {
        case Some(groupRoom: GroupRoomContentsModel) =>
          transferData.userExhibitRoom filter groupRoom.canAddChild map
            { room => groupRoom.addChild(room); true } getOrElse false
        case Some(basicRoom: MutableMuseumExhibitListModel) =>
          transferData.exhibitList foreach basicRoom.add
          true
        case _ => false
      }
    case _ => super.importData(ts)
  }
  
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
  
  override def createTransferable(c: JComponent): Transferable = c match {
    case tree: JTree =>
      val contentsOp = tree.getSelectionPath match {
        case null => None
        case path => controller.flatMap(_.getContent(path))
      }
      
      contentsOp.map(c => new ExhibitRoomTransferData(c)) getOrElse null
    case _ => super.createTransferable(c)
  }
}
