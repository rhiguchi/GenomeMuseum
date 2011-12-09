package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor, UnsupportedFlavorException}
import java.io.{File, IOException}

import javax.swing.{JComponent, TransferHandler}
import DataFlavor.javaFileListFlavor

import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitStorage, MuseumExhibitTransferData,
  UserExhibitRoom}
import jp.scid.genomemuseum.gui.ExhibitTableModel
import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}

/**
 * MuseumExhibit の転送ハンドラ。
 * @param tableModel ハンドラが適用されているコンポーネントと結合しているモデル。
 */
private[controller] class MuseumExhibitListTransferHandler(
    private[controller] val tableModel: ExhibitTableModel) extends TransferHandler {
  import MuseumExhibitListTransferHandler._
  
  private val importableFlavors = IndexedSeq(javaFileListFlavor, exhibitDataFlavor)
  
  /** 読み込み実行クラス */
  var loadManager: Option[MuseumExhibitLoadManager] = None
  
  var transferExhibits: Seq[MuseumExhibit] = Nil
  
  var sourceRoom: Option[UserExhibitRoom] = None
  
  override def canImport(comp: JComponent, transferFlavors: Array[DataFlavor]) = {
    logger.debug("canImport")
    loadManager match {
      case None => false
      case Some(manager) =>
        transferFlavors.contains(javaFileListFlavor)
    }
  }
  
  override def importData(comp: JComponent, t: Transferable) = {
    logger.debug("読み込み応答")
    
    if (t.isDataFlavorSupported(exhibitDataFlavor)) {
      logger.trace("ExhibitData フレーバー読み込み")
      // TODO 展示物転送処理
      false
    }
    else if (t.isDataFlavorSupported(javaFileListFlavor) && loadManager.nonEmpty) {
      import collection.JavaConverters._
      import util.control.Exception.catching
      
      logger.trace("ファイルフレーバー読み込み")
      
      try {
        val files = t.getTransferData(javaFileListFlavor) match {
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
      catch {
        case e: Exception =>
          scala.swing.Swing.onEDT {
            loadManager.get.alertFailToTransfer(e)
          }
          false
      }
    }
    else {
      logger.trace("対応するフレーバー無し")
      false
    }
  }
  
  /**
   * 転送許可
   */
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
  
  override def createTransferable(c: JComponent) = {
    MuseumExhibitTransferData(transferExhibits, sourceRoom, loadManager.get.museumExhibitStorage.get)
  }
  
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
