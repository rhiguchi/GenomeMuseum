package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler, JComponent}
import javax.swing.tree.{TreeModel, TreePath}
import java.awt.datatransfer.{Transferable, DataFlavor, UnsupportedFlavorException}
import TransferHandler.TransferSupport

import jp.scid.gui.control.ObjectControllerTransferHandler
import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitMuseumSpace, MuseumSpace}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit,
  GroupRoomContentsModel, ExhibitMuseumFloor, FreeExhibitRoomModel, ExhibitRoomModel}

private[controller] object ExhibitRoomListTransferHandler {
  private type PathList = List[IndexedSeq[MuseumSpace]]
  
  /** 転送オブジェクト生成 */
  object TransferData {
    val dataFlavor = new DataFlavor(classOf[ExhibitRoomListTransferHandler.TransferData],
        "ExhibitRoomListTransferHandler.TransferData")
    
    def unapply(ts: Transferable) = ts.isDataFlavorSupported(dataFlavor) match {
      case true =>
        val data = ts.getTransferData(dataFlavor).asInstanceOf[TransferData]
        Some(data.treeModel, data.pathList)
      case false => None
    }
  }
  
  /**
   * 部屋用転送オブジェクト
   */
  trait TransferData {
    def treeModel: TreeModel
    def pathList: PathList
  }
  
  /** 部屋用転送オブジェクト実装 */
  private case class TransferDataImpl(treeModel: TreeModel, pathList: PathList)
      extends TransferData with Transferable {
    override def getTransferDataFlavors() = Array(TransferData.dataFlavor)
    
    override def getTransferData(flavor: DataFlavor) = flavor match {
      case TransferData.dataFlavor => this
      case _ => throw new UnsupportedFlavorException(flavor)
    }
    
    override def isDataFlavorSupported(flavor: DataFlavor) = flavor match {
      case TransferData.dataFlavor => true
      case _ => false
    }
  }
}

/**
 * ExhibitRoomListController 用転送ハンドラ
 * 
 * {@code #sourceListModel} を指定することで、部屋の移動が可能になる。
 * @param loadManager 読み込み操作管理オブジェクト
 */
class ExhibitRoomListTransferHandler extends TransferHandler {
  def this(ctrl: ExhibitRoomListController) {
    this()
    
    this.treeController = Option(ctrl)
  }
  
  import ExhibitRoomListTransferHandler._
  /** ファイルの読み込み処理を行うモデル */
  var exhibitLoadManager: Option[MuseumExhibitLoadManager] = None
  
  /** 親コントローラ */
  var treeController: Option[ExhibitRoomListController] = None
  
  /**
   * 部屋の転入操作の可能性を返す。
   */
  override def canImport(ts: TransferSupport) = ts.getTransferable match {
    case TransferData(model, pathList) => getTargetRoomContents(ts) match {
      // 階層へは Room を転入できる
      case Some(floor: ExhibitMuseumFloor) =>
        getExhibitMuseumSpace(pathList).forall(floor.canAddRoom)
      // 自由展示室には展示物を転入できる
      case Some(target: ExhibitMuseumSpace with FreeExhibitRoomModel) =>
         !getExhibitMuseumSpace(pathList).contains(target)
      case _ => false
    }
    case _ => super.canImport(ts)
  }
  
  /**
   * 部屋の転入操作をする。
   */
  override def importData(ts: TransferSupport) = ts.getTransferable match {
    case TransferData(model, pathList) => getTargetRoomContents(ts) match {
      // 親を変更
      case Some(floor: ExhibitMuseumFloor) =>
        val roomList = getExhibitMuseumSpace(pathList)
        roomList foreach floor.addRoom
        roomList.nonEmpty
      // 展示物を追加
      case Some(target: ExhibitMuseumSpace with FreeExhibitRoomModel) =>
        val exhibitList = getExhibitMuseumSpace(pathList).flatMap(_.exhibitList)
        exhibitList foreach target.add
        exhibitList.nonEmpty
      case _ => false
    }
    case _ => super.importData(ts)
  }
  
  override def createTransferable(c: JComponent) = treeController.map(_.getTreeModel) match {
    case Some(treeModel) => c match {
      case tree: JTree => tree.getModel match {
        case `treeModel` =>
          import collection.JavaConverters._
          val pathList = treeController.map(_.getSelectedPathList.asScala)
            .getOrElse(Nil).map(_.asScala.toIndexedSeq).toList
          
          pathList.filter(_.last.isInstanceOf[ExhibitRoomModel]) match {
            case Nil => null
            case pathList => new TransferDataImpl(treeModel, pathList)
          }
        case _ => null
      }
    }
    case None => null
  }
  
  override def getSourceActions(c: JComponent) = TransferHandler.COPY
  
  /** 展示室リストを作成する */
  private def getExhibitMuseumSpace(pathList: PathList) =
    pathList map (_.last) collect { case e: ExhibitMuseumSpace => e }
  
  /**
   * 転入先の部屋オブジェクトを取得する。
   */
  protected[controller] def getTargetRoomContents(ts: TransferSupport) = ts.getComponent match {
    case tree: JTree =>
      val loc = ts.getDropLocation.getDropPoint
      TreePathLastObject unapply tree.getPathForLocation(loc.x, loc.y)
    case _ => None
  }
  
  /** TreePath オブジェクトの最後の葉要素をExhibitMuseumSpaceとして取得 */
  private object TreePathLastObject {
    import javax.swing.tree.TreePath
    
    def unapply(o: AnyRef): Option[MuseumSpace] = o match {
      case path: TreePath => path.getLastPathComponent match {
        case model: MuseumSpace => Some(model)
        case _ => None
      }
      case _ => None
    }
  }
}
