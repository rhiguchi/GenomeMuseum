package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor}
import java.io.{File, IOException}

import javax.swing.{JComponent, TransferHandler}
import DataFlavor.javaFileListFlavor
import TransferHandler.TransferSupport

import jp.scid.genomemuseum.model.{MuseumExhibit, MutableMuseumExhibitListModel, ExhibitRoomTransferData,
  MuseumExhibitListModel, DefaultExhibitRoomTransferData}

private[controller] object MuseumExhibitTransferHandler {
  import jp.scid.genomemuseum.model.{MuseumExhibitListModel}
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  
  /** 転送の部屋オブジェクトを返す。 */
  def getTransferRoomContents(ts: TransferSupport): MuseumExhibitListModel =
    ts.getTransferable.getTransferData(exhibitRoomDataFlavor).asInstanceOf[MuseumExhibitListModel]
    
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
/**
 * MuseumExhibit の転送ハンドラ。
 * 
 */
class MuseumExhibitListTransferHandler extends MuseumExhibitTransferHandler {
  import MuseumExhibitTransferHandler.getTransferRoomContents
  import ExhibitRoomTransferData.{dataFlavor => exhibitRoomDataFlavor}
  
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
  
  def canImportExhibits(target: MutableMuseumExhibitListModel, source: MuseumExhibitListModel) = {
    // 同一部屋へは展示物の転送をしない
    source.getRoom != target.getRoom
  }
  
  /**
   * 展示物オブジェクトの転入が可能かを調べる。
   */
  override def canImport(ts: TransferSupport) = {
    if (ts.isDataFlavorSupported(exhibitRoomDataFlavor)) {
      getExhibitTransferTarget(ts) match {
        case Some(room) => canImportExhibits(room, getTransferRoomContents(ts.getTransferable))
        case _ => false
      }
    }
    else {
      super.canImport(ts)
    }
  }
  
  /**
   * 展示物オブジェクトの転入 を試みる。
   */
  override def importData(ts: TransferSupport) = {
    if (ts.isDataFlavorSupported(exhibitRoomDataFlavor)) {
      getExhibitTransferTarget(ts) match {
        case Some(room) =>
          import collection.JavaConverters._
          getTransferRoomContents(ts.getTransferable).getValue.asScala.toList foreach room.add
          true
        case _ => false
      }
    }
    else {
      super.importData(ts)
    }
  }
  
  /**
   * 転送許可
   */
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
  
  override def createTransferable(c: JComponent): Transferable = {
    import collection.JavaConverters._
    
    controllerModel.flatMap(_.getRoom) match {
      case Some(room) =>
        val selections = selectedElements
        DefaultExhibitRoomTransferData(selections, room)
      case _ => super.createTransferable(c)
    }
  }
}
