package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor, UnsupportedFlavorException}
import java.io.{File, IOException}

import javax.swing.{table, JTable, JComponent, TransferHandler}
import table.TableModel
import DataFlavor.javaFileListFlavor
import TransferHandler.TransferSupport

import jp.scid.gui.control.ObjectControllerTransferHandler
import jp.scid.genomemuseum.model.{MuseumExhibit, ExhibitRoomModel, ExhibitMuseumSpace,
  FreeExhibitRoomModel, MuseumExhibitService}

object MuseumExhibitListTransferHandler {
  /**
   * 転送データ作成オブジェクト
   */
  object TransferData {
    val dataFlavor =
      new DataFlavor(classOf[MuseumExhibitListTransferHandler.TransferData],
        "MuseumExhibitListTransferHandler.TransferData")
    
    /**
     * `TransferSupport` から展示物リスとデータを作成する
     */
    def unapply(t: Transferable) = t.isDataFlavorSupported(dataFlavor) match {
      case true =>
        val data = t.getTransferData(dataFlavor).asInstanceOf[TransferData]
        Some(data.model, data.exhibitList)
      case false => None
    }
    
    def apply(model: ExhibitRoomModel, exhibitList: List[MuseumExhibit] = Nil): TransferData =
      TransferDataImpl(model, exhibitList)
  }

  trait TransferData {
    def model: ExhibitRoomModel
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
      model: ExhibitRoomModel,
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
class MuseumExhibitListTransferHandler extends TransferHandler {
  import MuseumExhibitListTransferHandler._
  import ExhibitRoomListTransferHandler.{TransferData => TreeTransferData, getExhibitRoomModel,
    FileListTransferData}
  
  private type ImportableRoom = ExhibitMuseumSpace with FreeExhibitRoomModel
  
  /** 親コントローラを指定して初期化 */
  def this(controller: MuseumExhibitListController) {
    this()
    exhibitController = Option(controller)
  }
  
  /** 親コントローラ */
  var exhibitController: Option[MuseumExhibitListController] = None
  
  /**
   * 部屋の転入操作の可能性を返す。
   */
  override def canImport(ts: TransferSupport) = ts.getTransferable match {
    // 転送データ
    case TransferData(model, elements) => controllerModel match {
      case room: ImportableRoom => room != model
      case _ => false
    }
    // ツリーノード
    case TreeTransferData(_, pathList) => controllerModel match {
      case room: ImportableRoom => getExhibitRoomModel(pathList).forall(room.!=)
      case _ => false
    }
    // ファイル
    case t => if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) controllerModel match {
      case _: ImportableRoom | _: MuseumExhibitService => true
      case _ => false
    }
    else super.canImport(ts)
  }
  
  /** 転送オブジェクトを転入 */
  override def importData(ts: TransferSupport) = ts.getTransferable match {
    // 転送データ
    case TransferData(_, elements) => controllerModel match {
      case room: ImportableRoom => elements.foreach(room.add); true
      case _ => false
    }
    // ツリーノード
    case TreeTransferData(_, pathList) => controllerModel match {
      case room: ImportableRoom =>
        val exhibitList = getExhibitRoomModel(pathList).flatMap(_.exhibitList)
        exhibitList foreach room.add
        true
      case _ => false
    }
    // ファイル
    case FileListTransferData(files) => controllerModel match {
      case _: ImportableRoom | _: MuseumExhibitService =>
        exhibitController map (_.importFile(files)) getOrElse false
      case _ => false
    }
  }
  
  /** 転送オブジェクトの作成 */
  override def createTransferable(c: JComponent) = c match {
    case table: JTable if controllerTableModel == table.getModel => 
      import collection.JavaConverters._
      
      val selections = exhibitController.get.getSelectedElements.asScala.toList
      val files = selections.flatMap(_.sourceFile) 
      
      TransferDataImpl(controllerModel, selections, files)
    case _ => null
  }
  
  /** コントローラの現在のモデルを返す */
  private[controller] def controllerModel() = exhibitController.get.getModel
  
  /** コントローラのテーブル用モデルを返す */
  private[controller] def controllerTableModel(): TableModel = exhibitController.get.getTableModel
  
  /**
   * 転送許可
   */
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
}
