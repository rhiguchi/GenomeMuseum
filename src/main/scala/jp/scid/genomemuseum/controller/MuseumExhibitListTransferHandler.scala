package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor}
import java.io.{File, IOException}

import javax.swing.{JComponent, TransferHandler}
import DataFlavor.javaFileListFlavor
import TransferHandler.TransferSupport

import jp.scid.gui.control.ObjectControllerTransferHandler
import jp.scid.genomemuseum.model.{MuseumExhibit, ExhibitRoomModel, MutableMuseumExhibitListModel, RoomContentExhibits,
  MuseumExhibitListModel, UserExhibitRoom, FreeExhibitRoomModel}

private[controller] object MuseumExhibitTransferHandler {
  /**
   * 転送データ作成オブジェクト
   */
  object TransferData {
    val dataFlavor =
      new DataFlavor(classOf[RoomContentExhibits], "RoomContentExhibits")
    
    /**
     * `TransferSupport` から展示物リスとデータを作成する
     */
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
  class TransferData(contents: RoomContentExhibits) extends Transferable {
    def this(exhibits: List[MuseumExhibit], room: Option[UserExhibitRoom]) {
      this(RoomContentExhibitsImpl(exhibits, room))
    }
    
    def getTransferDataFlavors(): Array[DataFlavor] =
      Array(TransferData.dataFlavor, javaFileListFlavor)
    
    def getTransferData(flavor: DataFlavor) = flavor match {
      case TransferData.dataFlavor => contents
      case `javaFileListFlavor` =>
        import collection.JavaConverters._
        val files = contents.exhibitList flatMap (_.sourceFile)
        files.asJava: java.util.List[File]
      case _ => null
    }
    
    def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
      case TransferData.dataFlavor => true
      case `javaFileListFlavor` => true
      case _ => false
    }
  }
  
  /**
   * 単純実装
   */
  private case class RoomContentExhibitsImpl(
    exhibitList: List[MuseumExhibit],
    userExhibitRoom: Option[UserExhibitRoom]
  ) extends RoomContentExhibits
}

/**
 * ファイルと展示物の転送ハンドラ
 */
abstract class MuseumExhibitTransferHandler extends TransferHandler {
  import MuseumExhibitTransferHandler.TransferData
  
  /** ファイルの読み込み処理を行うモデル */
  var exhibitLoadManager: Option[MuseumExhibitLoadManager] = None
  
  /** 展示物の転入が可能な部屋を返す */
  protected[controller] def getExhibitTransferTarget(ts: TransferSupport)
    : Option[MutableMuseumExhibitListModel]
  
  private[controller] def canImportExhibits(target: MutableMuseumExhibitListModel, source: Option[UserExhibitRoom]) = {
    // 同一部屋とLocalLibraryへは展示物の転送をしない
    target.userExhibitRoom.nonEmpty && target.userExhibitRoom != source
  }

  /**
   * 展示物オブジェクトの転入が可能かを調べる。
   */
  override def canImport(ts: TransferSupport) = ts match {
    case TransferData(contents) => getExhibitTransferTarget(ts) match {
      case Some(room) => canImportExhibits(room, contents.userExhibitRoom)
      case _ => false
    }
    case _ => if (ts.isDataFlavorSupported(javaFileListFlavor) && exhibitLoadManager.nonEmpty) {
      // ファイルの転入を調べる。
      getExhibitTransferTarget(ts).nonEmpty
    }
    else {
      // その他は上位クラスに委譲
      super.canImport(ts)
    }
  }
  
  /**
   * 展示物オブジェクトの転入 を試みる。
   */
  override def importData(ts: TransferSupport) = ts match {
    case TransferData(contents) => getExhibitTransferTarget(ts) match {
      case Some(room) =>
        contents.exhibitList foreach room.add
        true
      case _ => false
    }
    case _ => if (ts.isDataFlavorSupported(javaFileListFlavor) && exhibitLoadManager.nonEmpty)
      // ファイルの転入を行う。
      getExhibitTransferTarget(ts) match {
        case Some(room) =>
          val fileList = getTransferFiles(ts)
//          fileList.foreach(file => exhibitLoadManager.get.loadExhibit(room, file))
          true
        case _ => false
      }
    else {
      // その他は上位クラスに委譲
      super.importData(ts)
    }
  }
  
  /** 転送するファイルを返す。 */
  protected[controller] def getTransferFiles(ts: TransferSupport): List[File] = {
    import collection.JavaConverters._
    ts.getTransferable.getTransferData(javaFileListFlavor).asInstanceOf[java.util.List[File]].asScala.toList
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
            case TreePathLastObject(model) => Nil // todo model.exhibitList
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
