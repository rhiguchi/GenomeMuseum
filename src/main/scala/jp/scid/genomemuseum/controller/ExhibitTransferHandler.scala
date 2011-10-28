package jp.scid.genomemuseum.controller

import java.io.File

import javax.swing.{JComponent, TransferHandler}
import java.awt.datatransfer._
import scala.swing.Swing

/**
 * ファイル転送ハンドラ
 */
class ExhibitTransferHandler(controller: MainViewController) extends TransferHandler {
  import java.awt.datatransfer.DataFlavor
  
  override def canImport(comp: JComponent, flavors: Array[DataFlavor]): Boolean = {
    flavors contains DataFlavor.javaFileListFlavor
  }
  
  /**
   * データの読み込み
   */
  override def importData(comp: JComponent, t: Transferable): Boolean = {
    import java.{util => ju}
    import collection.JavaConverters._
    try {
      val paths = t.getTransferData(DataFlavor.javaFileListFlavor).asInstanceOf[ju.List[File]]
      val files = searchFiles(paths.asScala)
      Swing.onEDT {
        controller.loadBioFile(files)
      }
      files.nonEmpty
    }
    catch {
      case e: UnsupportedFlavorException =>
        e.printStackTrace
        false
      case e: java.io.IOException =>
        e.printStackTrace
        false
    }
  }
  
  protected def searchFiles(files: Seq[File]) = {
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
