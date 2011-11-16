package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor, UnsupportedFlavorException}
import java.io.{File, IOException}

import javax.swing.{JComponent, TransferHandler}

import jp.scid.genomemuseum.model.MuseumExhibit

/**
 * MuseumExhibit の転送ハンドラ。
 */
private class MuseumExhibitListTransferHandler(ctrl: MuseumExhibitListController) extends TransferHandler {
  import MuseumExhibitListController.TableSource._
  import DataFlavor.javaFileListFlavor
  import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
  import MuseumExhibitListTransferHandler._
  
  private val importableFlavors = IndexedSeq(javaFileListFlavor, exhibitDataFlavor)
  
  override def canImport(comp: JComponent, transferFlavors: Array[DataFlavor]) = {
    transferFlavors.intersect(importableFlavors).nonEmpty
  }
  
  override def importData(comp: JComponent, t: Transferable) = {
    import collection.JavaConverters._
    
    if (t.isDataFlavorSupported(exhibitDataFlavor)) {
      false
    }
    else if (t.isDataFlavorSupported(javaFileListFlavor)) {
      try {
        val files = t.getTransferData(javaFileListFlavor).asInstanceOf[java.util.List[File]]
        files match {
          case null => false
          case files =>
            val allFiles = getAllFiles(files.asScala)
            ctrl.loadBioFiles(allFiles)
        }
      }
      catch {
        case e: UnsupportedFlavorException =>
          e.printStackTrace
          false
        case e: IOException =>
          e.printStackTrace
          false
      }
    }
    else {
      false
    }
  }
  
  override def getSourceActions(c: JComponent) = {
    isTransferAllowed match {
      case true => TransferHandler.COPY_OR_MOVE
      case false => TransferHandler.NONE
    }
  }
  
  override def createTransferable(c: JComponent) = {
    if (isTransferAllowed) ctrl.localSourceTableModel.selections match {
      case Nil => null
      case selections => MuseumExhibitTransferData(selections)
    }
    else
      null
  }
  
  private def isTransferAllowed = ctrl.tableSource == LocalSource
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
private case class MuseumExhibitTransferData(
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
