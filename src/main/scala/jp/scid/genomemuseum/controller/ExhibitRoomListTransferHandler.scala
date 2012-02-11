package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler, JComponent}
import javax.swing.tree.TreePath
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport

import jp.scid.genomemuseum.model.{MuseumExhibitService, ExhibitRoomTransferData}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  GroupRoomContentsModel, MuseumExhibitListModel,
  MutableMuseumExhibitListModel}

/**
 * ExhibitRoomListController 用転送ハンドラ
 * 
 * {@code #sourceListModel} を指定することで、部屋の移動が可能になる。
 * @param loadManager 読み込み操作管理オブジェクト
 */
class ExhibitRoomListTransferHandler extends MuseumExhibitTransferHandler {
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  import MuseumExhibitTransferHandler.getTransferRoomContents
  
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
  override def canImport(ts: TransferSupport) = {
    lazy val targetRoom = getTargetRoomContents(ts)
    
    // 部屋の転入
    if (ts.isDataFlavorSupported(exhibitRoomDataFlavor)) {
      val contentsModel = getTransferRoomContents(ts)
      
      targetRoom match {
        // GroupRoom はRoomを転入できる
        // LocalLibrary にも転入できる（GroupRoomContentsModel を実装する）
        case Some(groupRoom: GroupRoomContentsModel) =>
          groupRoom.canMove(contentsModel)
        // BasicRoom には MuseumExhibit を転入できる
        case Some(basicRoom: MutableMuseumExhibitListModel) =>
          true
        case _ => false
      }
    }
    else {
      super.canImport(ts)
    }
  }
  
  /**
   * 部屋の転入操作をする。
   */
  override def importData(ts: TransferSupport) = {
    lazy val targetRoom = getTargetRoomContents(ts)
    
    // 部屋の転入
    if (ts.isDataFlavorSupported(exhibitRoomDataFlavor)) {
      val contentsModel = getTransferRoomContents(ts)
      
      targetRoom match {
        case Some(groupRoom: GroupRoomContentsModel) if groupRoom.canMove(contentsModel) =>
          groupRoom.moveRoom(contentsModel)
          true
        case Some(basicRoom: MutableMuseumExhibitListModel) =>
          import collection.JavaConverters._
          
          contentsModel.getValue.asScala.toList foreach basicRoom.add
          true
        case _ => false
      }
    }
    else {
      super.importData(ts)
    }
  }
  
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
  
  override def createTransferable(c: JComponent): Transferable = {
    c match {
      case tree: JTree =>
        val contentsOp = tree.getSelectionPath match {
          case null => None
          case path => controller.flatMap(_.getContent(path))
        }
        
        contentsOp collect { case t: Transferable => t } getOrElse null
      case _ => super.createTransferable(c)
    }
  }
}
