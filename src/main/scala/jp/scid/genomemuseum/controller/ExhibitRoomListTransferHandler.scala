package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler, JComponent}
import javax.swing.tree.TreePath
import java.awt.datatransfer.{Transferable, DataFlavor}
import TransferHandler.TransferSupport

import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitMuseumSpace}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  GroupRoomContentsModel, MuseumExhibitListModel,
  MutableMuseumExhibitListModel}

private[controller] object ExhibitRoomListTransferHandler {
  object ExhibitRoomTransferData {
    val dataFlavor = new DataFlavor(classOf[MuseumExhibitListModel],
        "MuseumExhibitListModel")
    
    def unapply(ts: TransferSupport): Option[MuseumExhibitListModel] = {
      ts.isDataFlavorSupported(dataFlavor) match {
        case true =>
          val data = ts.getTransferable.getTransferData(dataFlavor)
            .asInstanceOf[MuseumExhibitListModel]
          Some(data)
        case false => None
      }
    }
  }
  
  /**
   * 部屋用転送オブジェクト
   */
  class ExhibitRoomTransferData(roomContents: MuseumExhibitListModel)
      extends MuseumExhibitTransferHandler.TransferData(roomContents) {
    override def getTransferDataFlavors(): Array[DataFlavor] =
      ExhibitRoomTransferData.dataFlavor +: super.getTransferDataFlavors
    
    override def getTransferData(flavor: DataFlavor) = flavor match {
      case ExhibitRoomTransferData.dataFlavor => roomContents
      case _ => super.getTransferData(flavor)
    }
    
    override def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
      case ExhibitRoomTransferData.dataFlavor => true
      case _ => super.isDataFlavorSupported(flavor)
    }
  }
}

/**
 * ExhibitRoomListController 用転送ハンドラ
 * 
 * {@code #sourceListModel} を指定することで、部屋の移動が可能になる。
 * @param loadManager 読み込み操作管理オブジェクト
 */
class ExhibitRoomListTransferHandler extends MuseumExhibitTransferHandler {
  import ExhibitRoomListTransferHandler._
  /** モデル */
  var structure: Option[MuseumStructure] = None
  
  var treeController: Option[ExhibitRoomListController] = None
  
  /** 展示物の転入が可能な部屋を返す */
  def getExhibitTransferTarget(ts: TransferSupport) = {
    getTargetRoomContents(ts) collect {
      case room: MutableMuseumExhibitListModel => room
    }
  }
  
  /**
   * 転入先の部屋オブジェクトを取得する。
   */
  protected[controller] def getTargetRoomContents(ts: TransferSupport): Option[MuseumExhibitListModel] = {
    ts.getComponent match {
      case tree: JTree =>
        val loc = ts.getDropLocation.getDropPoint
        
        None
//        tree.getPathForLocation(loc.x, loc.y) match {
//          case null => structure.flatMap(_.museumExhibitService)
//          case path => getContent(path)
//        }
      case _ => None
    }
  }
  
  /**
   * 部屋の転入操作の可能性を返す。
   */
  override def canImport(ts: TransferSupport) = ts match {
    case ExhibitRoomTransferData(transferData) =>
      getTargetRoomContents(ts) match {
        // GroupRoom はRoomを転入できる
        // LocalLibrary にも転入できる（GroupRoomContentsModel を実装する）
        case Some(groupRoom: GroupRoomContentsModel) =>
          transferData.userExhibitRoom map groupRoom.canAddChild getOrElse false
        // BasicRoom には MuseumExhibit を転入できる
        case Some(basicRoom: MutableMuseumExhibitListModel) => true 
        case _ => false
      }
    case _ => super.canImport(ts)
  }
  
  /** 転送オブジェクトを転入 */
//  override def importTransferData(rowIndex: Int, data: TransferData) = {
//    
//  }
  
  /**
   * 部屋の転入操作をする。
   */
  override def importData(ts: TransferSupport) = ts match {
    case ExhibitRoomTransferData(transferData) =>
      getTargetRoomContents(ts) match {
        case Some(groupRoom: GroupRoomContentsModel) =>
          transferData.userExhibitRoom filter groupRoom.canAddChild map
            { room => groupRoom.addChild(room); true } getOrElse false
        case Some(basicRoom: MutableMuseumExhibitListModel) =>
          transferData.exhibitList foreach basicRoom.add
          true
        case _ => false
      }
    case _ => super.importData(ts)
  }
  
  override def getSourceActions(c: JComponent) =
    TransferHandler.COPY
  
  
  /** TreePath オブジェクトの最後の葉要素をExhibitRoomModelとして取得 */
  private object TreePathLastObject {
    import javax.swing.tree.TreePath
    
    def unapply(o: AnyRef): Option[ExhibitMuseumSpace] = o match {
      case path: TreePath => path.getLastPathComponent match {
        case model: ExhibitMuseumSpace => Some(model)
        case _ => None
      }
      case _ => None
    }
  }
}
