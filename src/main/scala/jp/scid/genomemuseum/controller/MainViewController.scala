package jp.scid.genomemuseum.controller

import java.util.ResourceBundle
import java.io.{File, FileInputStream}
import jp.scid.genomemuseum.{view, model, GenomeMuseumGUI}
import view.{MainView, MainViewMenuBar}
import model.MuseumSourceModel
import javax.swing.JFrame
import scala.swing.Action


class MainViewController(
  parent: GenomeMuseumGUI,
  mainView: MainView,
  frameOfMainView: JFrame
) {
  val tableCtrl = new ExhibitTableController(mainView.dataTable,
    mainView.quickSearchField)
  val sourceModel = new MuseumSourceModel
  protected val transferHandler = new BioFileTransferHandler(parent)
  
  mainView.dataTableScroll.setTransferHandler(transferHandler)
  mainView.sourceList.setModel(sourceModel.treeModel)
  mainView.sourceList.setSelectionModel(sourceModel.treeSelectionModel)
  
  reloadResources()
  
  def showFrame() {
    frameOfMainView.pack
    frameOfMainView setLocationRelativeTo null
    frameOfMainView setVisible true
  }
  
  /** リソースを設定する */
  private def reloadResources() {
  }
  
  private def reloadResources(res: ResourceBundle) {
    val rm = new ResourceManager(res)
  }
}

class ResourceManager(res: ResourceBundle) {
  import collection.JavaConverters._
  import java.lang.Boolean.parseBoolean
  import javax.swing.KeyStroke.getKeyStroke
  
  def injectTo(action: Action, keyPrefix: String) {
    val resKeys = res.getKeys.asScala.filter(_.startsWith(keyPrefix))
    if (resKeys.isEmpty)
      throw new IllegalArgumentException(
        "No resource which starts with '%s' found.".format(keyPrefix))
    
    resKeys.foreach { resKey =>
      resKey.substring(keyPrefix.length) match {
        case ".title" => action.title = res.getString(resKey)
        case ".enabled" => action.enabled = parseBoolean(res.getString(resKey))
        case ".accelerator" =>
          action.accelerator = Some(getKeyStroke(res.getString(resKey)))
        case ".toolTip" => action.toolTip = res.getString(resKey)
        case _ => // TODO log warnings
          println("unsupported key: " + resKey)
      }
    }
  }
}

import javax.swing.{JComponent, TransferHandler}
import java.awt.datatransfer._
import scala.swing.Swing

/**
 * ファイル転送ハンドラ
 */
class BioFileTransferHandler(controller: GenomeMuseumGUI) extends TransferHandler {
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

