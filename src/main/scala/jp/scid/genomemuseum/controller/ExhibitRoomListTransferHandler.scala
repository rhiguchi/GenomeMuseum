package jp.scid.genomemuseum.controller

import javax.swing.{TransferHandler, JComponent}
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor
import java.io.File

import jp.scid.genomemuseum.model.{MuseumExhibitService, MuseumExhibitTransferData}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom,
  MuseumStructure, UserExhibitRoomService}
import jp.scid.genomemuseum.gui.MuseumSourceModel
import UserExhibitRoom.RoomType
import RoomType._
import MuseumExhibitListTransferHandler.getAllFiles

/**
 * ExhibitRoomListController 用転送ハンドラ
 */
private[controller] class ExhibitRoomListTransferHandler(sourceListModel: MuseumSourceModel) extends TransferHandler {
  import MuseumExhibitTransferData.{dataFlavor => exhibitListDataFlavor}
  
  var exhibitService: Option[MuseumExhibitService] = None
  /** 読み込み実行クラス */
  var loadManager: Option[MuseumExhibitLoadManager] = None
  
  override def canImport(ts: TransferSupport) = {
    val destRoom = getImportingTarget(ts)
    lazy val localLibNode = sourceListModel.pathForLocalLibrary.last
    
    // ノードの転送
    var transferred = (ts.isDataFlavorSupported(exhibitListDataFlavor), ts.getTransferable) match {
      case (true, t: MuseumExhibitTransferData) => destRoom match {
        case None => t.sourceRoom.nonEmpty
        case Some(destRoom: UserExhibitRoom) => t.sourceRoom match {
          case Some(`destRoom`) => false
          case sourceRoom => destRoom.roomType match {
            case BasicRoom => true
            case GroupRoom => sourceRoom.nonEmpty && !isDescendant(destRoom, sourceRoom.get)
            case _ => false
          }
        }
        case _ => false
      }
    }
    // ファイルの転送
    transferred = if (transferred) true
    else ts.isDataFlavorSupported(javaFileListFlavor) match {
      case true => destRoom match {
        case Some(UserExhibitRoom.RoomType(BasicRoom)) => true
        case Some(`localLibNode`) => true
        case _ => false
      }
      case false => false
    }
    
    transferred match {
      case true => true
      case false => super.canImport(ts)
    }
  }
  
  override def importData(ts: TransferSupport) = {
    val target = getImportingTarget(ts)
    lazy val localLibNode = sourceListModel.pathForLocalLibrary.last
    
    // 部屋と展示物の転入
    if (ts.isDataFlavorSupported(exhibitListDataFlavor)) {
      ts.getTransferable.getTransferData(exhibitListDataFlavor) match {
        case transferData: MuseumExhibitTransferData => target match {
          case Some(room: UserExhibitRoom) =>
            importExhibitData(transferData, Some(room))
          case None =>
            importExhibitData(transferData, None)
          case _ => false
        }
        case _ =>
          false
      }
    }
    // ファイルの転入
    else if (ts.isDataFlavorSupported(javaFileListFlavor) && loadManager.nonEmpty) {
      import collection.JavaConverters._
      
      val files = ts.getTransferable.getTransferData(javaFileListFlavor) match {
        case null => Nil
        case data => data.asInstanceOf[java.util.List[File]].asScala
      }
      getAllFiles(files) match {
        case Nil => false
        case files =>
          // 読み込みマネージャへ処理を委譲
          files foreach loadManager.get.loadExhibit
          true
      }
    }
    else {
      false
    }
  }
  
  /**
   * 部屋の転入
   */
  private def importExhibitData(transferData: MuseumExhibitTransferData, target: Option[UserExhibitRoom]): Boolean = {
    target match {
      // BasicRoom への転入
      case Some(target @ RoomType(BasicRoom)) => transferData.sourceRoom match {
        case Some(`target`) => false
        case _ => exhibitService match {
          case Some(service) =>
            transferData.museumExhibits foreach
              (e => service.addElement(target, e.asInstanceOf[service.ElementClass]))
            true
          case None => false
        }
      }
      // 部屋移動
      case Some(RoomType(GroupRoom)) | None => transferData.sourceRoom match {
        case Some(sourceRoom) =>
          val targetRoom = target.getOrElse(sourceListModel.pathForUserRooms.last)
          sourceListModel.isDescendant(sourceRoom, targetRoom) match {
            case false =>
              sourceListModel.moveRoom(sourceRoom, target)
              true
            case true => false
          }
        case None => false
      }
      case _ => false
    }
  }
  
  protected[controller] def getImportingTarget(ts: TransferSupport): Option[ExhibitRoom] = {
    None
  }
  
  protected[controller] def isDescendant(maybe: UserExhibitRoom, room: UserExhibitRoom): Boolean = {
    false
  }
  
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY_OR_MOVE
  
  override def createTransferable(c: JComponent) = {
    sourceListModel.selectedPath.map(_.lastOption) match {
      case Some(room: UserExhibitRoom) =>
        val exhibits = exhibitService.map(_.getExhibits(room)).getOrElse(Nil)
        MuseumExhibitTransferData(exhibits, Some(room), loadManager.get.museumExhibitStorage.get)
      case _ => null
    }
  }
  
  /**
   * 移動可能判定
   */
  protected[controller] def canMove(source: IndexedSeq[ExhibitRoom], dest: IndexedSeq[ExhibitRoom]) = {
    (source.headOption, dest.lastOption) match {
      case (Some(_: UserExhibitRoom), Some(UserExhibitRoom.RoomType(GroupRoom))) =>
        !dest.startsWith(source)
      case _ => false
    }
  }
}