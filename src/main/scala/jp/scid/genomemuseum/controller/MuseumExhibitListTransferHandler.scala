package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor, UnsupportedFlavorException}
import java.io.{File, IOException}

import javax.swing.{JComponent, TransferHandler}
import DataFlavor.javaFileListFlavor
import TransferHandler.TransferSupport

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitTransferData,
  UserExhibitRoom, ExhibitRoom}
import UserExhibitRoom.RoomType
import RoomType._
import jp.scid.genomemuseum.gui.ExhibitTableModel
import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}

/**
 * MuseumExhibit の転送ハンドラ。
 * 
 */
abstract class MuseumExhibitListTransferHandler extends TransferHandler {
  import MuseumExhibitListTransferHandler._
  
  override def importData(ts: TransferSupport) = {
    // 展示物オブジェクトの転入
    if (ts.isDataFlavorSupported(exhibitDataFlavor))
      getImportExhibitsFunction(getTargetRooom(ts), ts.getTransferable).map(_.apply).getOrElse(false)
    // ファイルの転入
    else if (ts.isDataFlavorSupported(javaFileListFlavor))
      getImportFilesFunction(getTargetRooom(ts), ts.getTransferable).map(_.apply).getOrElse(false)
    // その他は上位クラスに委譲
    else
      super.importData(ts)
  }
  
  override def canImport(ts: TransferSupport) = {
    // 展示物オブジェクトの転入
    if (ts.isDataFlavorSupported(exhibitDataFlavor))
      getImportExhibitsFunction(getTargetRooom(ts), ts.getTransferable).nonEmpty
    // ファイルの転入
    else if (ts.isDataFlavorSupported(javaFileListFlavor))
      getImportFilesFunction(getTargetRooom(ts), ts.getTransferable).nonEmpty
    // その他
    else
      super.canImport(ts)
  }
  
  /**
   * 展示物の転入操作をする関数を取得する。
   * 
   * 転入先や転出元のの条件で転送可能な展示物がない時は {@code None} が返る。
   */
  protected[controller] def getImportExhibitsFunction(
      targetRoomOp: Option[ExhibitRoom], t: Transferable): Option[() => Boolean] = {
    (targetRoomOp, t.getTransferData(exhibitDataFlavor)) match {
      case (Some(targetRoom @ RoomType(BasicRoom)), transferData: MuseumExhibitTransferData) =>
        // 転入先部屋が転出元と異なるときのみ転送する
        transferData.sourceRoom match {
          case Some(`targetRoom`) => None
          case _ =>
            Some(() => importExhibits(transferData.museumExhibits, targetRoom))
        }
      case _ => None
    }
  }
  
  /**
   * ファイル転入転送を行う関数を取得する。
   * 
   * 転入先や転出元のの条件で転送可能なファイルがない時は空の配列が返る。
   */
  protected[controller] def getImportFilesFunction(targetRoom: Option[ExhibitRoom],
      t: Transferable): Option[() => Boolean] = {
    import collection.JavaConverters._
    lazy val fileList = getAllFiles(t.getTransferData(javaFileListFlavor).asInstanceOf[java.util.List[File]].asScala)
    
    targetRoom match {
      case Some(room @ RoomType(BasicRoom)) =>
        Some(() => importFiles(fileList, Some(room)))
      case None =>
        Some(() => importFiles(fileList, None))
      case _ => None
    }
  }
  
  /**
   * 転送されたファイルを読み込む。
   * 
   * @param files ディレクトリではないパスリスト
   * @param targetRoom 転入先の部屋
   * @return 取り込みが正常に完了したら {@code true}
   */
  def importFiles(files: Seq[File], targetRoom: Option[UserExhibitRoom]): Boolean
  
  /**
   * 展示物の転入操作を行う。
   * 
   * @param exhibits 追加する展示物。
   * @param targetRoom 転入先
   * @return 取り込みが正常に完了したら {@code true}
   */
  def importExhibits(exhibits: Seq[MuseumExhibit], targetRoom: UserExhibitRoom): Boolean
  
  /**
   * 転入先の部屋を取得する。
   */
  protected[controller] def getTargetRooom(ts: TransferSupport): Option[ExhibitRoom]
  
  /**
   * 転送許可
   */
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
}

private[controller] object MuseumExhibitListTransferHandler {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[MuseumExhibitListTransferHandler])
  /**
   * ディレクトリ内に含まれる全てのファイルを探索し、取得する。
   */
  private[controller] def getAllFiles(files: Seq[File]) = {
    import collection.mutable.{Buffer, ListBuffer, HashSet}
    // 探索済みディレクトリ
    val checkedDirs = HashSet.empty[String]
    // ディレクトリがすでに探索済みであるか
    def alreadyChecked(dir: File) = checkedDirs contains dir.getCanonicalPath
    // 探索済みに追加
    def addCheckedDir(dir: File) = checkedDirs += dir.getCanonicalPath
    
    @annotation.tailrec
    def collectFiles(files: List[File], accume: Buffer[File] = ListBuffer.empty[File]): List[File] = {
      files match {
        case head :: tail =>
          if (head.isHidden) {
            collectFiles(tail, accume)
          }
          else if (head.isDirectory && !alreadyChecked(head)) {
            addCheckedDir(head)
            collectFiles(head.listFiles.toList ::: tail, accume)
          }
          else {
            accume += head
            collectFiles(tail, accume)
          }
        case Nil => accume.toList
      }
    }
    
    collectFiles(files.toList)
  }
}
