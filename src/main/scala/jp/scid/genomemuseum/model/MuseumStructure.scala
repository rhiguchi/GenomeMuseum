package jp.scid.genomemuseum.model

import jp.scid.gui.tree.EditableTreeSource
import UserExhibitRoom.RoomType
import RoomType._

/**
 * ExhibitRoom のツリーのモデル
 */
class MuseumStructure extends EditableTreeSource[ExhibitRoom] {
  import MuseumStructure._
  
  var basicRoomDefaultName = "New BasicRoom"
  var groupRoomDefaultName = "New GroupRoom"
  var smartRoomDefaultName = "New SmartRoom"
  
  /** データソース */
  private var currentUserExhibitRoomSource: Option[UserExhibitRoomService] = None
  
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
  def userExhibitRoomSource_=(newSource: UserExhibitRoomService) {
    currentUserExhibitRoomSource = Option(newSource)
  }
  
  /** UserExhibitRoom の子要素を取得するショートカット */
  private def getUserRoomChildren(parent: Option[UserExhibitRoom]) =
    userExhibitRoomSource.getChildren(parent).toList
  
  
  /**
   * 部屋をサービスに追加する。
   * 
   * @param roomType 部屋の種類
   * @param parent 親要素
   * @return 追加に成功した場合、そのオブジェクトが返る。
   * @throws IllegalArgumentException
   *         {@code parent#roomType} が {@code GroupRoom} 以外の時
   * @see UserExhibitRoom
   */
  def addRoom(roomType: RoomType, parent: Option[UserExhibitRoom]): UserExhibitRoom = {
    parent match {
      case Some(elm) if elm.roomType != GroupRoom =>
        throw new IllegalArgumentException("roomType of parent must be GroupRoom")
      case _ =>
    }
    
    val name = findRoomNewName(roomType match {
      case BasicRoom => basicRoomDefaultName
      case GroupRoom => groupRoomDefaultName
      case SmartRoom => smartRoomDefaultName
    })
    
    userExhibitRoomSource.addRoom(roomType, name, parent)
  }
  
  /**
   * 部屋の移動が可能か
   */
  def canMove(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) = {
    dest match {
      case UserExhibitRoom(dest @ RoomType(GroupRoom)) =>
        !pathToRoot(dest).startsWith(pathToRoot(source))
      case None => userExhibitRoomSource.getParent(source).nonEmpty
      case _ => false
    }
  }
  
  /**
   * 新しい親へ移動する
   * @param element 移動する要素
   * @param newParent 異動先となる親要素。ルート項目にする時は None 。
   * @throws IllegalArgumentException 指定した親が GroupRoom ではない時
   * @throws IllegalStateException 指定した親が要素自身か、子孫である時
   */
  def moveRoom(source: UserExhibitRoom, newParent: Option[UserExhibitRoom]) = {
    newParent match {
      case UserExhibitRoom(dest @ RoomType(GroupRoom)) =>
        val sourcePath = pathToRoot(source)
        val destPath = pathToRoot(dest)
        destPath.startsWith(sourcePath) match {
          case true => throw new IllegalStateException(
            "'%s' is not allowed to move to '%s'".format(sourcePath, destPath))
          case false => userExhibitRoomSource.setParent(source, newParent)
        }
      case None => userExhibitRoomSource.setParent(source, None)
      case _ => throw new IllegalArgumentException("parent must be a GroupRoom")
    }
  }
  
  /**
   * 部屋を削除する
   */
  def removeRoom(room: UserExhibitRoom) {
    userExhibitRoomSource.remove(room)
  }
  
  /**
   * 未使用の名前を検索する。
   * {@code baseName} の名前を持つ部屋がサービス中に存在するとき、
   * 連番をつけて次の名前を検索する。
   * @param baseName 基本の名前
   * @return 他と重複しない、部屋の名前。
   */
  private def findRoomNewName(baseName: String) = {
    def searchNext(index: Int): String = {
      val candidate = baseName + " " + index
      userExhibitRoomSource.nameExists(candidate) match {
        case true => searchNext(index + 1)
        case false => candidate
      }
    }
    
    userExhibitRoomSource.nameExists(baseName) match {
      case true => searchNext(1)
      case false => baseName
    }
  }
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
