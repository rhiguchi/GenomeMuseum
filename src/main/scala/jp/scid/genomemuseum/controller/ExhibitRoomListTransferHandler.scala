package jp.scid.genomemuseum.controller

import java.io.File
import javax.swing.{JTree, TransferHandler, JComponent}
import javax.swing.tree.{TreeModel, TreePath}
import java.awt.datatransfer.{Transferable, DataFlavor, UnsupportedFlavorException}
import TransferHandler.TransferSupport

import jp.scid.gui.control.ObjectControllerTransferHandler
import jp.scid.genomemuseum.model.{MuseumStructure, ExhibitMuseumSpace, MuseumSpace, FreeExhibitPavilion}
import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibit, MuseumExhibitService,
  ExhibitPavilionFloor, FreeExhibitRoomModel, ExhibitRoomModel}

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
    
    def apply(treeModel: TreeModel, pathList: PathList = Nil): TransferData =
      TransferDataImpl(treeModel, pathList)
  }
  
  /** ファイル転送オブジェクト */
  object FileListTransferData {
    private val dataFlavor = DataFlavor.javaFileListFlavor
    
    def unapply(ts: Transferable) = ts.isDataFlavorSupported(dataFlavor) match {
      case true =>
        import collection.JavaConverters._
        
        ts.getTransferData(dataFlavor).asInstanceOf[java.util.List[File]] match {
          case null => None
          case fileList => Some(fileList.asScala.toList)
        }
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
  
  /** 移動可能な展示室リストを返す */
  private[controller] def getExhibitMuseumSpace(pathList: PathList) =
    pathList map (_.last) collect { case e: ExhibitMuseumSpace => e }
  
  /** 展示物を保持した展示室リストを返す */
  private[controller] def getExhibitRoomModel(pathList: PathList) =
    pathList map (_.last) collect { case e: ExhibitRoomModel => e }
  
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
  private[controller] object TreePathLastObject {
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

/**
 * ExhibitRoomListController 用転送ハンドラ
 * 
 * {@code #sourceListModel} を指定することで、部屋の移動が可能になる。
 * @param loadManager 読み込み操作管理オブジェクト
 */
class ExhibitRoomListTransferHandler extends TransferHandler {
  import ExhibitRoomListTransferHandler._
  
  /** 親コントローラ */
  var treeController: Option[ExhibitRoomListController] = None
  
  def this(ctrl: ExhibitRoomListController) {
    this()
    
    this.treeController = Option(ctrl)
  }
  
  private def exhibitPavilion = treeController.flatMap(_.freeExhibitPavilion)
  
  /**
   * 部屋の転入操作の可能性を返す。
   */
  override def canImport(ts: TransferSupport) = ts.getTransferable match {
    case TransferData(model, pathList) => getTargetRoomContents(ts) match {
      // 部屋移動
      case floor: ExhibitPavilionFloor =>
        getExhibitMuseumSpace(pathList).forall(floor.canAddRoom)
      case None => exhibitPavilion match {
        case Some(pav) => getExhibitMuseumSpace(pathList).forall(pav.canAddRoom)
        case _ => false
      }
      // 展示物追加
      case room: ExhibitMuseumSpace with FreeExhibitRoomModel =>
        getExhibitRoomModel(pathList).filter(room.!=).nonEmpty
      case _ => false
    }
    case FileListTransferData(fileList) => getTargetRoomContents(ts) match {
      // 展示室と展示物サービスにはファイルを追加できる
      case Some(_: MuseumExhibitService) | Some(_: FreeExhibitRoomModel) => true
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
      case floor: ExhibitPavilionFloor =>
        getExhibitMuseumSpace(pathList).foreach(floor.addRoom)
        true
      case None => exhibitPavilion match {
        case Some(pav) =>
          getExhibitMuseumSpace(pathList).foreach(pav.addRoom)
          true
        case _ => false
      }
      // 展示物追加
      case TreePathLastObject(room: ExhibitMuseumSpace with FreeExhibitRoomModel) =>
        val exhibitList = getExhibitRoomModel(pathList).flatMap(_.exhibitList)
        exhibitList foreach room.add
        true
      case _ => false
    }
    case FileListTransferData(fileList) => getTargetRoomContents(ts) match {
      // 展示物サービスにも追加できる
      case Some(r: MuseumExhibitService) =>
        treeController map (_.importFile(fileList)) getOrElse false
      // 展示室にはファイルを追加できる
      case Some(target: FreeExhibitRoomModel) =>
        treeController map (_.importFile(fileList, target)) getOrElse false
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
}
