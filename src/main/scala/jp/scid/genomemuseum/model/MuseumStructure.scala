package jp.scid.genomemuseum.model

import jp.scid.gui.tree.EditableTreeSource
import UserExhibitRoom.RoomType._

/**
 * ExhibitRoom のツリーのモデル
 */
class MuseumStructure extends EditableTreeSource[ExhibitRoom] {
  import MuseumStructure._
  
  /** データソース */
  private var currentUserExhibitRoomSource: Option[TreeDataService[UserExhibitRoom]] = None
  
  /** ローカルファイルのソース要素 */
  private val localSourceFloor = MuseumFloor("Local")
  /** NCBI ソース要素 */
  private val webSourceFloor = MuseumFloor("NCBI")
  /** ソースを含めた要素 */
  private val sourcesFloor = MuseumFloor("Libraries", localSourceFloor, webSourceFloor)
  /** ユーザー部屋のルートノード */
  private val userRoomsFloor = MuseumFloor("User Rooms")
  /** ルート要素 */
  private val museum = MuseumFloor("Museum", sourcesFloor, userRoomsFloor)
  
  /** ルートオブジェクト */
  def root: ExhibitRoom = museum
  
  /** ローカルソースの要素を取得 */
  def localSource: ExhibitRoom = localSourceFloor
  
  /** Web ソースの要素を取得 */
  def webSource: ExhibitRoom = webSourceFloor
  
  /** ユーザー設定部屋のルート要素 */
  def userRoomsRoot: ExhibitRoom = userRoomsFloor
  
  /** ソースのルート要素 */
  def sourcesRoot: ExhibitRoom = sourcesFloor
  
  /** 子要素 */
  def childrenFor(parent: ExhibitRoom) = {
    if (isLeaf(parent)) Nil
    else parent match {
      // ユーザー設定部屋ルートの時は、サービスからのルート要素取得して返す
      case `userRoomsFloor` =>
        getUserRoomChildren(None)
      
      // ユーザー設定部屋の時は、サービスから子要素を取得して返す
      case parent: UserExhibitRoom =>
        getUserRoomChildren(Some(parent))
      
      // MuseumFloor の時は、メソッドから子要素を返す
      case parent: MuseumFloor =>
        parent.children
      
      // 該当が無い時は Nil
      case _ => Nil
    }
  }
  
  /** 末端要素であるか */
  def isLeaf(room: ExhibitRoom) = room match {
    case room: UserExhibitRoom => room.roomType != GroupRoom
    case `userRoomsFloor` => false
    case floor: MuseumFloor => floor.children.isEmpty
    case _ => throw new IllegalArgumentException(
      "node %s is not valid ExhibitRoom".format(room))
  }
  
  /**
   * UserExhibitRoom の値を更新し、サービスへ更新を通知する。
   */
  protected def update(element: UserExhibitRoom, newValue: AnyRef) {
    newValue match {
      case value: String => element.name = value
      case _ =>
    }
    userExhibitRoomSource.save(element)
  }
  
  /** 値の更新 */
  def update(path: IndexedSeq[ExhibitRoom], newValue: AnyRef) = path match {
    case Seq(`museum`, `userRoomsFloor`, userRoomPath @ _*) => userRoomPath.lastOption match {
      case Some(element: UserExhibitRoom) => 
        update(element, newValue)
      case None =>
        throw new IllegalArgumentException("updating is not allowed")
      }
    case _ =>
      throw new IllegalArgumentException("updating is not allowed")
  }
  
  /**
   * ルート要素までのパスを取得
   */
  def pathToRoot(node: ExhibitRoom): IndexedSeq[ExhibitRoom] = {
    import collection.mutable.Buffer
    
    def getParent(node: ExhibitRoom, path: List[ExhibitRoom] = Nil): List[ExhibitRoom] = {
      node match {
        case floor: MuseumFloor => floor.parent match {
          case Some(parent) => getParent(parent, floor :: path)
          case None => node :: path // return value
        }
        case room: UserExhibitRoom =>
          val parent = userExhibitRoomSource.getParent(room).getOrElse(userRoomsRoot)
          getParent(parent, room :: path)
      }
    }
    
    getParent(node).toIndexedSeq
  }
  
  /**
   * ユーザー設定部屋のデータソースを取得
   */
  def userExhibitRoomSource = currentUserExhibitRoomSource.get
  
  /**
   * ユーザー設定部屋のデータソースを設定
   */
  def userExhibitRoomSource_=(newSource: TreeDataService[UserExhibitRoom]) {
    currentUserExhibitRoomSource = Option(newSource)
  }
  
  /** UserExhibitRoom の子要素を取得するショートカット */
  private def getUserRoomChildren(parent: Option[UserExhibitRoom]) =
    userExhibitRoomSource.getChildren(parent).toList
}

object MuseumStructure {
  private case class MuseumFloor(
    var name: String,
    children: List[MuseumFloor] = Nil
  ) extends ExhibitRoom {
    var parent: Option[MuseumFloor] = None
    
    override def toString = name
  }
  
  private object MuseumFloor {
    def apply(name: String, children: MuseumFloor*): MuseumFloor = {
      val floor = MuseumFloor(name, children.toList)
      children foreach (_.parent = Some(floor))
      floor
    }
  }
}
