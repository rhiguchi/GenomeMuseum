package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor, UnsupportedFlavorException}
import java.io.{File, IOException}

import javax.swing.{table, JComponent, TransferHandler}
import table.TableModel
import DataFlavor.javaFileListFlavor
import TransferHandler.TransferSupport

import jp.scid.gui.control.ObjectControllerTransferHandler
import jp.scid.genomemuseum.model.{MuseumExhibit, ExhibitRoomModel,
  FreeExhibitRoomModel, MuseumExhibitService}

private[controller] object MuseumExhibitTransferHandler {
  /**
   * 転送データ作成オブジェクト
   */
  object TransferData {
    val dataFlavor =
      new DataFlavor(classOf[MuseumExhibitTransferHandler.TransferData],
        "MuseumExhibitTransferHandler.TransferData")
    
    /**
     * `TransferSupport` から展示物リスとデータを作成する
     */
    def unapply(t: Transferable) = t.isDataFlavorSupported(dataFlavor) match {
      case true =>
        val data = t.getTransferData(dataFlavor).asInstanceOf[TransferData]
        Some(data.tableModel, data.exhibitList)
      case false => None
    }
  }

  trait TransferData {
    def tableModel: TableModel
    def exhibitList: List[MuseumExhibit]
  }

  /**
   * MuseumExhibit 転送データ
   * 
   * 展示物オブジェクトと、展示物にファイルが設定されている時はファイルも転送される。
   * 文字列も転送可能であり、展示物の {@code toString()} から作成される。
   * 転送文字列の形式を変えたい時は、{@link #stringConverter} プロパティを変更する。
   * 
   * @param exhibits 転送する展示物。
   * @param sourceRoom 展示物が存在していた部屋。部屋からの転出ではない時は {@code None} 。
   */
  private case class TransferDataImpl(
      tableModel: TableModel,
      exhibitList: List[MuseumExhibit],
      fileList: List[File] = Nil)
      extends TransferData with Transferable {
    
    val getTransferDataFlavors = fileList.isEmpty match {
      case true => Array(TransferData.dataFlavor)
      case false => Array(TransferData.dataFlavor, javaFileListFlavor)
    }
    
    def getTransferData(flavor: DataFlavor) = flavor match {
      case TransferData.dataFlavor => this
      case `javaFileListFlavor` if fileList.nonEmpty =>
        import collection.JavaConverters._
        fileList.asJava: java.util.List[File]
      case _ => throw new UnsupportedFlavorException(flavor)
    }
    
    def isDataFlavorSupported(flavor: DataFlavor) =
      getTransferDataFlavors.contains(flavor)
  }
}

/**
 * MuseumExhibit の転送ハンドラ。
 * 
 */
class MuseumExhibitListTransferHandler extends ObjectControllerTransferHandler {
  import ObjectControllerTransferHandler.TransferData
  
  /** 親コントローラを指定して初期化 */
  def this(controller: MuseumExhibitListController) {
    this()
    exhibitController = Option(controller)
  }
  
  /** 親コントローラ */
  var exhibitController: Option[MuseumExhibitListController] = None
  
  /** 展示物オブジェクトのファイルを返す */
  override def getSourceFile(element: AnyRef) = element match {
    case exhibit: MuseumExhibit => exhibit.sourceFile getOrElse null
    case _ => null
  }
  
  /**
   * 展示物オブジェクトの転入が可能かを調べる。
   */
  override def canImport(ts: TransferSupport) = exhibitController.get.getModel match {
    case model: MuseumExhibitService => true
    case model: FreeExhibitRoomModel => true
    case _ => false
  }
  
  /** ファイルを転入 */
  override def importFile(rowIndex: Int, fileList: java.util.List[File]) = {
    import collection.JavaConverters._
    exhibitController.get.importFile(fileList.asScala.toList)
  }
  
  /** 転送オブジェクトを転入 */
  override def importTransferData(rowIndex: Int, fileList: TransferData) = {
    import collection.JavaConverters._
    
    exhibitController match {
      case Some(controller) => fileList.getSourceModel == controller.getModel match {
        case true => false
        case false =>
          val elements = fileList.getSelectedElements.asScala flatMap {
            case exhibit: MuseumExhibit => Some(exhibit)
            case TreePathLastObject(model) => model.getValue.asScala
            case _ => None
          }
          controller.addElements(elements.toList)
      }
      case None => false
    }
  }
  
  /** TreePath オブジェクトの最後の葉要素をExhibitRoomModelとして取得 */
  private object TreePathLastObject {
    import javax.swing.tree.TreePath
    
    def unapply(o: AnyRef): Option[ExhibitRoomModel] = o match {
      case path: TreePath => path.getLastPathComponent match {
        case model: ExhibitRoomModel => Some(model)
        case _ => None
      }
      case _ => None
    }
  }
  
  /**
   * 転送許可
   */
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
}
