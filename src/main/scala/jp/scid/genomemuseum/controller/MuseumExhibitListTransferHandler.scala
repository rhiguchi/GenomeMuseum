package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor, UnsupportedFlavorException}
import java.io.{File, IOException}

import javax.swing.{JComponent, TransferHandler}

import jp.scid.genomemuseum.model.MuseumExhibit
import jp.scid.genomemuseum.gui.ExhibitTableModel

/**
 * MuseumExhibit の転送ハンドラ。
 */
private[controller] class MuseumExhibitListTransferHandler(
    private[controller] val tableModel: ExhibitTableModel) extends TransferHandler {
  import DataFlavor.javaFileListFlavor
  import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
  import MuseumExhibitListTransferHandler._
  
  private val importableFlavors = IndexedSeq(javaFileListFlavor, exhibitDataFlavor)
  
  var loadManager: Option[MuseumExhibitLoadManager] = None
  
  override def canImport(comp: JComponent, transferFlavors: Array[DataFlavor]) = {
    loadManager match {
      case None => false
      case Some(manager) =>
        transferFlavors.contains(javaFileListFlavor)
    }
  }
  
  override def importData(comp: JComponent, t: Transferable) = {
    import collection.JavaConverters._
    import util.control.Exception.catching
    
    if (t.isDataFlavorSupported(exhibitDataFlavor)) {
      false
    }
    else if (t.isDataFlavorSupported(javaFileListFlavor) && loadManager.nonEmpty) {
      catching(classOf[UnsupportedFlavorException], classOf[IOException]) either {
        t.getTransferData(javaFileListFlavor).asInstanceOf[java.util.List[File]]
      } match {
        case Right(files) => files match {
          case null => false
          case files => getAllFiles(files.asScala) match {
            case Nil => false
            case files =>
              loadManager.get.loadExhibits(tableModel, files)
              true
          }
        }
        case Left(e: Exception) =>
          loadManager.get.alertFailToTransfer(e)
          false
      }
    }
    else {
      false
    }
  }
  
  /**
   * 転送許可
   */
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY_OR_MOVE
  
  override def createTransferable(c: JComponent) = {
    tableModel.selections match {
      case Nil => null
      case selections => MuseumExhibitTransferData(selections)
    }
  }
  
}

private object MuseumExhibitListTransferHandler {
  
  /**
   * ディレクトリ内に含まれる全てのファイルを探索し、取得する。
   */
  private def getAllFiles(files: Seq[File]) = {
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
          else if (head.isFile) {
            accume += head
            collectFiles(tail, accume)
          }
          else if (head.isDirectory && !alreadyChecked(head)) {
            addCheckedDir(head)
            collectFiles(head.listFiles.toList ::: tail, accume)
          }
          else {
            collectFiles(tail, accume)
          }
        case Nil => accume.toList
      }
    }
    
    collectFiles(files.toList)
  }
}

/**
 * MuseumExhibit 行の転送データ
 */
private[controller] case class MuseumExhibitTransferData(
  exhibits: List[MuseumExhibit]
) extends Transferable {
  import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
  
  def getTransferDataFlavors(): Array[DataFlavor] = {
    return Array(exhibitDataFlavor)
  }
  
  def getTransferData(flavor: DataFlavor) = {
    flavor match {
      case `exhibitDataFlavor` =>
        this
      case _ => null
    }
  }
  
  def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
    case `exhibitDataFlavor` => true
    case _ => false
  }
}

private object MuseumExhibitTransferData {
  val dataFlavor = new DataFlavor(MuseumExhibitTransferData.getClass,
    "MuseumExhibitTransferData")
}
