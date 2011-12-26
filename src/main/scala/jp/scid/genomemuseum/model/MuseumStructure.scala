package jp.scid.genomemuseum.model

import scala.collection.mutable.Publisher
import scala.collection.script.Message

import jp.scid.gui.tree.EditableTreeSource
import UserExhibitRoom.RoomType
import RoomType._

/**
 * ExhibitRoom のツリーのモデル
 */
class MuseumStructure(val roomService: UserExhibitRoomService)
    extends EditableTreeSource[ExhibitRoom] with Publisher[Message[ExhibitRoom]] {
  import MuseumStructure._
  
  // プロパティ
  /** {@code BasicRoom} 型の部屋を作成するときの標準の名前 */
  var basicRoomDefaultName = "New BasicRoom"
  /** {@code GroupRoom} 型の部屋を作成するときの標準の名前 */
  var groupRoomDefaultName = "New GroupRoom"
  /** {@code SmartRoom} 型の部屋を作成するときの標準の名前 */
  var smartRoomDefaultName = "New SmartRoom"
  
  // 規定ノード
  /** ローカルソースの要素を取得 */
  val localSource = MuseumFloor("Local")
  /** Web ソースの要素を取得 */
  val webSource = MuseumFloor("NCBI")
  /** ライブラリーカテゴリの要素 */
  val sourcesRoot = MuseumFloor("Libraries", localSource, webSource)
  /** ユーザー部屋カテゴリの要素 */
  val userRoomsRoot = MuseumFloor("User Rooms")
  /** ルート要素 */
  val root = MuseumFloor("Museum", sourcesRoot, userRoomsRoot)
  
  /** 変更イベントの結合 */
  roomService.subscribe(new roomService.Sub {
    def notify(pub: roomService.Pub, event: Message[UserExhibitRoom]) {
      publish(event)
    }
  })
  
  /** 子要素を取得 */
  override def childrenFor(parent: ExhibitRoom) = {
    if (isLeaf(parent)) Nil
    else parent match {
      // ユーザー設定部屋ルートの時は、サービスからのルート要素取得して返す
      case `userRoomsRoot` => roomService.getChildren(None).toList
      // ユーザー設定部屋の時は、サービスから子要素を取得して返す
      case parent: UserExhibitRoom => roomService.getChildren(Some(parent)).toList
      // MuseumFloor の時は、メソッドから子要素を返す
      case parent: MuseumFloor => parent.children
      // 該当が無い時は Nil
      case _ => Nil
    }
  }
  
  /** 末端要素であるか */
  override def isLeaf(room: ExhibitRoom) = room match {
    case room: UserExhibitRoom => room.roomType != GroupRoom
    case `userRoomsRoot` => false
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
    roomService.save(element)
  }
  
  /** 値の更新 */
  override def update(path: IndexedSeq[ExhibitRoom], newValue: AnyRef) = path.lastOption match {
    case Some(element: UserExhibitRoom) => update(element, newValue)
    case None =>
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
          val parent = roomService.getParent(room).getOrElse(userRoomsRoot)
          getParent(parent, room :: path)
      }
    }
    
    getParent(node).toIndexedSeq
  }
  
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
    
    roomService.addRoom(roomType, name, parent)
  }
  
  /**
   * 部屋の移動が可能か
   */
  def canMove(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) = {
    dest match {
      case Some(dest @ RoomType(GroupRoom)) =>
        !pathToRoot(dest).startsWith(pathToRoot(source))
      case None => roomService.getParent(source).nonEmpty
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
  def moveRoom(source: UserExhibitRoom, newParent: Option[UserExhibitRoom]) {
    newParent match {
      case Some(dest @ RoomType(GroupRoom)) =>
        val sourcePath = pathToRoot(source)
        val destPath = pathToRoot(dest)
        destPath.startsWith(sourcePath) match {
          case true => throw new IllegalStateException(
            "'%s' is not allowed to move to '%s'".format(sourcePath, destPath))
          case false => roomService.setParent(source, newParent)
        }
      case None => roomService.setParent(source, None)
      case _ => throw new IllegalArgumentException("parent must be a GroupRoom")
    }
  }
  
  /**
   * 部屋を削除する
   */
  def removeRoom(room: UserExhibitRoom) {
    roomService.remove(room)
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
      roomService.nameExists(candidate) match {
        case true => searchNext(index + 1)
        case false => candidate
      }
    }
    
    roomService.nameExists(baseName) match {
      case true => searchNext(1)
      case false => baseName
    }
  }
}

object MuseumStructure {
  /**
   * 階層オブジェクト
   */
  case class MuseumFloor(
    /** このノードの名前 */
    var name: String,
    /** このノードの子要素 */
    children: List[MuseumFloor] = Nil
  ) extends ExhibitRoom {
    /** 親要素 */
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
