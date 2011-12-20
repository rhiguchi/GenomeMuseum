package jp.scid.genomemuseum.controller

import javax.swing.{TransferHandler, JComponent}
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor
import java.io.File

import jp.scid.genomemuseum.model.{MuseumExhibitService, ExhibitRoomTransferData}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom,
  MuseumStructure, UserExhibitRoomService}
import jp.scid.genomemuseum.gui.MuseumSourceModel
import UserExhibitRoom.RoomType
import RoomType._
import MuseumExhibitListTransferHandler.getAllFiles

/**
 * ExhibitRoomListController 用転送ハンドラ
 */
abstract class ExhibitRoomListTransferHandler extends MuseumExhibitListTransferHandler {
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  
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
  def canMove(source: UserExhibitRoom, dest: Option[UserExhibitRoom]): Boolean
  
  /**
   * 部屋を移動する
   */
  def moveUserExhibitRoom(source: UserExhibitRoom, dest: Option[UserExhibitRoom]): Boolean
  
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY_OR_MOVE
}
