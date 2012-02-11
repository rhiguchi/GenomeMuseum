package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler, JComponent}
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor
import java.io.File

import jp.scid.genomemuseum.model.{MuseumExhibitService, ExhibitRoomTransferData}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  MuseumStructure, UserExhibitRoomService}
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
class ExhibitRoomListTransferHandler extends MuseumExhibitListTransferHandler {
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  /** sourceListModel を指定してハンドラを構築 */
  def this(sourceListModel: MuseumSourceModel) {
    this()
    this.sourceListModel = Option(sourceListModel)
  }
  
  /** ツリーを操作する対象のモデル */
  var sourceListModel: Option[MuseumSourceModel] = None
  
  /** ファイルの読み込み処理を行うモデル */
  var exhibitLoadManager: Option[MuseumExhibitLoadManager] = None
  
  override def canImport(ts: TransferSupport) = {
    if (ts.isDataFlavorSupported(exhibitRoomDataFlavor)) {
      getImportRoomFunction(getTargetRooom(ts), ts.getTransferable).nonEmpty
    }
    else {
      super.canImport(ts)
    }
  }
  
  /**
   * 部屋の転入操作をする関数を取得する。
   * 
   * 転入先や転出元のの条件で転送可能な展示物がない時は {@code None} が返る。
   */
  protected[controller] def getImportRoomFunction(
      targetRoomOp: Option[ExhibitRoom], t: Transferable): Option[() => Boolean] = {
    lazy val sourceRoom = t.getTransferData(exhibitRoomDataFlavor)
      .asInstanceOf[ExhibitRoomTransferData].userExhibitRoom
    
    def getFunction(dest: Option[UserExhibitRoom]) = canMove(sourceRoom, dest) match {
      case true => Some(() => moveUserExhibitRoom(sourceRoom, dest))
      case false => None
    }
    
    targetRoomOp match {
      case Some(targetRoom @ RoomType(GroupRoom)) => getFunction(Some(targetRoom))
      case None => getFunction(None)
      case _ => None
    }
  }
  
  override def importData(ts: TransferSupport) = {
    if (ts.isDataFlavorSupported(exhibitRoomDataFlavor)) {
      getImportRoomFunction(getTargetRooom(ts), ts.getTransferable).map(_.apply).getOrElse(false)
    }
    else {
      super.importData(ts)
    }
  }
  
  /**
   * 移動可能判定
   */
  def canMove(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) =
    sourceListModel.map(_.source.canMove(source, dest)).getOrElse(false)
  
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY_OR_MOVE
  
  
  /**
   * 部屋を移動する
   */
  def moveUserExhibitRoom(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) = {
    sourceListModel match {
      case Some(model) =>
        model.moveRoom(source, dest)
        true
      case None => false
    }
  }
    
  override def importExhibits(exhibits: Seq[MuseumExhibit], targetRoom: UserExhibitRoom) = {
    // TDOO
//    exhibitLoadManager match {
//      case Some(manager) =>
//        
//        exhibits map (_.asInstanceOf[service.ElementClass]) foreach
//          (e => service.addElement(targetRoom, e))
//        exhibits.nonEmpty
//      case _ => false
//    }
    false
  }
    
  override def importFiles(files: Seq[File], targetRoom: Option[UserExhibitRoom]) = {
    exhibitLoadManager match {
      case Some(loadManager) =>
        files foreach loadManager.loadExhibit
        files.nonEmpty
      case _ => false
    }
  }
  
  override def getTargetRooom(ts: TransferSupport): Option[ExhibitRoom] = {
    (ts.getComponent, sourceListModel) match {
      case (tree: JTree, Some(model)) if tree.getModel == model.treeModel =>
        val loc = ts.getDropLocation.getDropPoint
        tree.getPathForLocation(loc.x, loc.y) match {
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
    // TODO
//    (c, sourceListModel) match {
//      case (tree: JTree, Some(model)) if tree.getModel == model.treeModel => model.selectedPath.flatMap(_.lastOption) match {
//        case Some(room: UserExhibitRoom) => exhibitService match {
//          case Some(exhibitService) => ExhibitRoomTransferData(room, exhibitService)
//          case _ => null
//        }
//        case _ => null
//      }
//      case _ => null
//    }
    null: Transferable
  }
}
