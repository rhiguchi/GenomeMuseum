package jp.scid.genomemuseum.model

import java.beans.{PropertyChangeListener, PropertyChangeEvent}

import collection.mutable.Publisher
import collection.script.{Message, Include, Update, Remove}

import ca.odell.glazedlists.{EventList, GlazedLists, BasicEventList, FunctionList}

import jp.scid.gui.model.TreeSource
import UserExhibitRoom.RoomType
import RoomType._

/**
 * GenomeMuseum のバイオデータファイルのまとまり一覧（部屋）の構造。
 * 
 * バイオデータファイルはローカルで管理する物と、NCBIからアクセス可能なリモートで管理されているものがある。
 * ローカルで管理されるファイルは、利用者の要望に応じてグループ分けができる。
 * 
 */
class MuseumStructure extends TreeSource[MuseumSpace] with PropertyChangeObservable {
  import MuseumStructure._
  
  // プロパティ
  /** {@code BasicRoom} 型の部屋を作成するときの標準の名前 */
  var basicRoomDefaultName = "New BasicRoom"
  /** {@code GroupRoom} 型の部屋を作成するときの標準の名前 */
  var groupRoomDefaultName = "New GroupRoom"
  /** {@code SmartRoom} 型の部屋を作成するときの標準の名前 */
  var smartRoomDefaultName = "New SmartRoom"
  
  /** 自由展示棟のサービス */
  private var currentFreeExhibitPavilion: Option[FreeExhibitPavilion] = None
  
  /** ローカルライブラリ用展示物サービス */
  private var exhibitService: Option[MuseumExhibitService] = None
  
  // 規定ノード
  /** ローカルソースの要素を取得 */
  val localSource = MuseumControlFloor("Local")
  /** Web ソースの要素を取得 */
  val webSource = MuseumControlFloor("NCBI")
  /** ライブラリーカテゴリの要素 */
  val sourcesRoot = MuseumControlFloor("Main Pavilions")
  /** ユーザー部屋カテゴリの要素 */
  val userRoomsRoot = MuseumControlFloor("User Rooms")
  /** ルート要素 */
  val root = MuseumControlFloor("Museum Gate", sourcesRoot)
  
  /** 展示物サービスを取得する */
  def localManagedPavilion = exhibitService
  
  /**
   * 展示物サービスを設定する。
   */
  def localManagedPavilion_=(exhibitService: Option[MuseumExhibitService]) {
    // TODO add list
    this.exhibitService = exhibitService
  }
  
  /** 展示物サービスを取得する */
  def freeExhibitPavilion = currentFreeExhibitPavilion
  
  /**
   * 展示物サービスを設定する。
   */
  def freeExhibitPavilion_=(exhibitService: Option[FreeExhibitPavilion]) {
    // TODO add list
    this.currentFreeExhibitPavilion = exhibitService
  }
  
  /** 部屋データリストから展示室モデルリストを作成する */
  protected def createUserExhibitRoom(roomSourceList: java.util.List[UserExhibitRoom]) = {
//    val function = new ExhibitRoomModelFunction()
  }
  
  /**
   * 部屋の中身を取得する
   */
//  def getContent(room: UserExhibitRoom) =
//    room.exhibitListModel(userExhibitRoomService.get)

  def getChildren(parent: MuseumSpace): java.util.List[MuseumSpace] = parent match {
    // 階層の時は部屋を返す
    case floor: MuseumFloor => floor.childRoomList.asInstanceOf[java.util.List[MuseumSpace]]
    // 該当が無い時は Nil
    case _ => java.util.Collections.emptyList[MuseumSpace]
  }
  
  /** 末端要素であるか */
  def isLeaf(space: MuseumSpace) = space match {
    case floor: MuseumFloor => true
    case _ => false
  }
  
  override def getValue(): MuseumSpace = root
  
  def setValue(room: MuseumSpace) {
    // TODO
  }
  
  /**
   * MuseumSpace の値を更新する。
   */
  protected def updateNodeValue(room: MuseumSpace, newValue: AnyRef) = room match {
    case room: ExhibitMuseumSpace => newValue match {
      case name: String => room.name = name
      case _ =>
    }
    case _ =>
  }
  
  /** 値の更新 */
  def update(path: IndexedSeq[ExhibitRoom], newValue: AnyRef): Unit = path.lastOption match {
//    case Some(element: ExhibitRoom) => updateNodeValue(element, newValue)
    case None =>
  }
  
  /**
   * ルート要素までのパスを取得
   * @todo 実装
   */
  def pathToRoot(node: ExhibitRoom): IndexedSeq[MuseumSpace] = {
    import collection.mutable.Buffer
    
//    def getParent(node: ExhibitRoom, path: List[ExhibitRoom] = Nil): List[ExhibitRoom] = {
//      node match {
//        case floor: MuseumFloor => floor.parent match {
//          case Some(parent) => getParent(parent, floor :: path)
//          case None => node :: path // return value
//        }
//        case room: UserExhibitRoom =>
//          val parent = userExhibitRoomService.flatMap(_.getParent(room)).getOrElse(userRoomsRoot)
//          getParent(parent, room :: path)
//      }
//    }
    
//    getParent(node).toIndexedSeq
    IndexedSeq.empty
  }
  
  /**
   * ローカルソースまでのパス
   */
  def pathForLoalSource = pathToRoot(localSource)
  
  /**
   * 部屋をサービスに追加する。
   * 
   * @param roomType 部屋の種類
   * @param parent 親要素
   * @return 追加に成功した場合、そのオブジェクトが返る。
   * @see UserExhibitRoom
   */
  def addRoom(roomType: RoomType, parent: ExhibitMuseumFloor): ExhibitRoomModel = {
    val name = roomType match {
      case BasicRoom => basicRoomDefaultName
      case GroupRoom => groupRoomDefaultName
      case SmartRoom => smartRoomDefaultName
    }
    
    freeExhibitPavilion.get.addRoom(roomType, name, parent)
  }
  
  /**
   * 部屋を削除する
   */
  def removeRoom(room: ExhibitMuseumSpace) = freeExhibitPavilion foreach (_.removeRoom(room))
  
  private def fireChildrenChange(parent: ExhibitRoom) {
    val event = new TreeSource.MappedPropertyChangeEvent(this, "children", parent, null, null)
    firePropertyChange(event)
  }
}

import java.beans.PropertyChangeEvent

object MuseumStructure {
  import collection.JavaConverters._
  
  /**
   * 管理階層
   */
  class MuseumControlFloor(
    /** このノードの名前 */
    var name: String,
    /** このノードの子要素 */
    val childRoomList: EventList[MuseumSpace] = new BasicEventList
  ) extends MuseumFloor {
    /** 親要素 */
    var parent: Option[MuseumFloor] = None
    
    def addElement(element: MuseumControlFloor) {
      childRoomList.add(element)
      element.parent = Some(this)
    }
    
    override def toString = name
  }
  
  private object MuseumControlFloor {
    def apply(name: String, children: MuseumSpace*): MuseumControlFloor =
      new MuseumControlFloor(name, GlazedLists.eventListOf(children: _*))
  }
}
