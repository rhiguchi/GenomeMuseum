package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler, JComponent}
import javax.swing.tree.TreePath
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor
import java.io.File

import jp.scid.genomemuseum.model.{MuseumExhibitService, ExhibitRoomTransferData}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  MuseumStructure, UserExhibitRoomService, GroupRoomContentsModel, MuseumExhibitListModel,
  MutableMuseumExhibitListModel}
import jp.scid.genomemuseum.gui.MuseumSourceModel
import UserExhibitRoom.RoomType
import RoomType._
import MuseumExhibitListTransferHandler.getAllFiles

/**
 * ExhibitRoomListController 用転送ハンドラ
 * 
 * {@code #sourceListModel} を指定することで、部屋の移動が可能になる。
 * @param loadManager 読み込み操作管理オブジェクト
 */
class ExhibitRoomListTransferHandler extends TransferHandler {
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  
  /** ツリーを操作する対象のモデル */
  var museumStructure: Option[MuseumStructure] = None
  
  /** ファイルの読み込み処理を行うモデル */
  var exhibitLoadManager: Option[MuseumExhibitLoadManager] = None
  
  /**
   * パスのコンテンツを取得する
   */
  protected def getPathRoomContents(path: TreePath): Option[MuseumExhibitListModel] = {
    val room = path.getLastPathComponent match {
      case room: ExhibitRoom => Some(room)
      case _ => None
    }
    room.flatMap(room => museumStructure.flatMap(_.getRoomContents(room)))
  }
  
  /**
   * 転入先の部屋オブジェクトを取得する。
   */
  def getTargetRoomContents(ts: TransferSupport): Option[MuseumExhibitListModel] = {
    ts.getComponent match {
      case tree: JTree =>
        val loc = ts.getDropLocation.getDropPoint
        
        tree.getPathForLocation(loc.x, loc.y) match {
          case null =>
            museumStructure.flatMap(str => str.getRoomContents(str.localSource))
          case path => getPathRoomContents(path)
        }
      case _ => None
    }
  }
  
  /** 転送の部屋オブジェクトを返す。 */
  private def getTransferRoomContents(ts: TransferSupport): MuseumExhibitListModel =
    ts.getTransferable.getTransferData(exhibitRoomDataFlavor).asInstanceOf[MuseumExhibitListModel]
  
  /** 転送するファイルを返す。 */
  private def getTransferFiles(ts: TransferSupport): List[File] = {
    import collection.JavaConverters._
    ts.getTransferable.getTransferData(javaFileListFlavor).asInstanceOf[java.util.List[File]].asScala.toList
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
    // ファイルの転入
    else if (ts.isDataFlavorSupported(javaFileListFlavor)) {
      targetRoom match {
        // BasicRoom には転入できる
        // LocalLibrary にも転入できる（MutableMuseumExhibitListModel を実装する）
        case Some(basicRoom: MutableMuseumExhibitListModel) => true
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
    // ファイルの転入
    else if (ts.isDataFlavorSupported(javaFileListFlavor) && exhibitLoadManager.nonEmpty) {
      targetRoom match {
        case Some(basicRoom: MutableMuseumExhibitListModel) =>
          val fileList = getTransferFiles(ts)
          fileList.foreach(file => exhibitLoadManager.get.loadExhibit(basicRoom, file))
          true
        case _ => false
      }
    }
    else {
      super.importData(ts)
    }
  }
  
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY_OR_MOVE
  
  override def createTransferable(c: JComponent): Transferable = {
    c match {
      case tree: JTree =>
        val contentsOp = tree.getSelectionPath match {
          case null => None
          case path => getPathRoomContents(path)
        }
        
        contentsOp collect { case t: Transferable => t } getOrElse null
      case _ => null
    }
  }
}
