package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor}
import java.io.{File, IOException}

import javax.swing.{JComponent, TransferHandler}
import DataFlavor.javaFileListFlavor
import TransferHandler.TransferSupport

import jp.scid.genomemuseum.model.{MuseumExhibit, MutableMuseumExhibitListModel, RoomContentExhibits,
  ExhibitRoomTransferData, MuseumExhibitListModel, UserExhibitRoom}

private[controller] object MuseumExhibitTransferHandler {
  import jp.scid.genomemuseum.model.{MuseumExhibitListModel}
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  
  /** 転送の部屋オブジェクトを返す。 */
  def getTransferRoomContents(t: Transferable): MuseumExhibitListModel =
    t.getTransferData(exhibitRoomDataFlavor).asInstanceOf[MuseumExhibitListModel]
  
  /** 転送するファイルを返す。 */
  def getTransferFiles(ts: TransferSupport): List[File] = {
    import collection.JavaConverters._
    ts.getTransferable.getTransferData(javaFileListFlavor).asInstanceOf[java.util.List[File]].asScala.toList
  }
}

abstract class MuseumExhibitTransferHandler extends TransferHandler {
  import MuseumExhibitTransferHandler.{getTransferFiles}
  
  /** ファイルの読み込み処理を行うモデル */
  var exhibitLoadManager: Option[MuseumExhibitLoadManager] = None
  
  /** 展示物の転入が可能な部屋を返す */
  protected[controller] def getExhibitTransferTarget(ts: TransferSupport)
    : Option[MutableMuseumExhibitListModel]
  
  /**
   * ファイルの転入を調べる。
   */
  override def canImport(ts: TransferSupport) = {
    if (ts.isDataFlavorSupported(javaFileListFlavor) && exhibitLoadManager.nonEmpty) {
      getExhibitTransferTarget(ts).nonEmpty
    }
    else {
      // その他は上位クラスに委譲
      super.canImport(ts)
    }
  }
  
  /**
   * ファイルの転入を行う。
   */
  override def importData(ts: TransferSupport) = {
    if (ts.isDataFlavorSupported(javaFileListFlavor) && exhibitLoadManager.nonEmpty)
      getExhibitTransferTarget(ts) match {
        case Some(room) =>
          val fileList = getTransferFiles(ts)
          fileList.foreach(file => exhibitLoadManager.get.loadExhibit(room, file))
          true
        case _ => false
      }
    else {
      // その他は上位クラスに委譲
      super.importData(ts)
    }
  }
}

private[controller] object MuseumExhibitListTransferHandler {
  
  private[controller] object RoomContentExhibitsTransferData {
    val dataFlavor =
      new DataFlavor(RoomContentExhibits.getClass, "RoomContentExhibits")
    
    def unapply(ts: TransferSupport): Option[RoomContentExhibits] = {
      ts.isDataFlavorSupported(dataFlavor) match {
        case true =>
          val data = ts.getTransferable.getTransferData(dataFlavor)
            .asInstanceOf[RoomContentExhibits]
          Some(data)
        case false => None
      }
    }
  }

  private[controller] class RoomContentExhibitsTransferData(contents: RoomContentExhibits) extends Transferable {
    def getTransferDataFlavors(): Array[DataFlavor] =
      Array(RoomContentExhibitsTransferData.dataFlavor, javaFileListFlavor)
    
    def getTransferData(flavor: DataFlavor) = flavor match {
      case RoomContentExhibitsTransferData.dataFlavor => contents
      case `javaFileListFlavor` =>
        import collection.JavaConverters._
        val files = contents.exhibitList flatMap (_.sourceFile)
        files.asJava: java.util.List[File]
      case _ => null
    }
    
    def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
      case RoomContentExhibitsTransferData.dataFlavor => true
      case `javaFileListFlavor` => true
      case _ => false
    }
  }
}

/**
 * MuseumExhibit の転送ハンドラ。
 * 
 */
class MuseumExhibitListTransferHandler extends MuseumExhibitTransferHandler {
  import MuseumExhibitListTransferHandler.RoomContentExhibitsTransferData
  
  def this(controller: MuseumExhibitListController) {
    this()
    exhibitController = Option(controller)
  }
  
  /** 親コントローラ */
  var exhibitController: Option[MuseumExhibitListController] = None
  
  /** コントローラの現在のモデルを返す */
  private def controllerModel: Option[MuseumExhibitListModel] = exhibitController.flatMap(c => Option(c.getModel))
  
  /** 親コントローラで現在選択されている要素リストを返す */
  private def selectedElements = {
    import collection.JavaConverters._
    exhibitController.map(_.getSelectedElements.asScala.toList).getOrElse(Nil)
  }
  
  /**
   * 転入可能な部屋を取得する
   */
  def getExhibitTransferTarget(ts: TransferSupport): Option[MutableMuseumExhibitListModel] = {
    controllerModel collect {
      case model: MutableMuseumExhibitListModel => model
    }
  }
  
  private[controller] def canImportExhibits(target: MutableMuseumExhibitListModel, source: Option[UserExhibitRoom]) = {
    // 同一部屋とLocalLibraryへは展示物の転送をしない
    target.getRoom.nonEmpty && target.getRoom != source
  }
  
  /**
   * 展示物オブジェクトの転入が可能かを調べる。
   */
  override def canImport(ts: TransferSupport) = ts match {
    case RoomContentExhibitsTransferData(contents) => getExhibitTransferTarget(ts) match {
      case Some(room) => canImportExhibits(room, contents.userExhibitRoom)
      case _ => false
    }
    case _ => super.canImport(ts)
  }
  
  /**
   * 展示物オブジェクトの転入 を試みる。
   */
  override def importData(ts: TransferSupport) = ts match {
    case RoomContentExhibitsTransferData(contents) => getExhibitTransferTarget(ts) match {
      case Some(room) =>
        contents.exhibitList foreach room.add
        true
      case _ => false
    }
    case _ => super.importData(ts)
  }
  
  /**
   * 転送許可
   */
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
  
  /**
   * 選択展示物から転送オブジェクトを作成
   */
  override def createTransferable(c: JComponent): Transferable = {
    import collection.JavaConverters._
    
    controllerModel match {
      case Some(room) =>
        val contents = RoomContentExhibits(selectedElements, room.getRoom)
        new RoomContentExhibitsTransferData(contents)
      case _ => super.createTransferable(c)
    }
  }
}
