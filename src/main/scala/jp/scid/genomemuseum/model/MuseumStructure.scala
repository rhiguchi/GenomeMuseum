package jp.scid.genomemuseum.model

import java.beans.{PropertyChangeListener, PropertyChangeEvent}

import collection.mutable.Publisher
import collection.script.{Message, Include, Update, Remove}

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
class MuseumStructure extends TreeSource[ExhibitRoom] with PropertyChangeObservable {
  import MuseumStructure._
  
  def this(roomService: UserExhibitRoomService, localLibraryContent: MuseumExhibitService) {
    this()
    
    this.roomService = Option(roomService)
    this.museumExhibitService = Option(localLibraryContent)
  }
  
  // プロパティ
  /** {@code BasicRoom} 型の部屋を作成するときの標準の名前 */
  var basicRoomDefaultName = "New BasicRoom"
  /** {@code GroupRoom} 型の部屋を作成するときの標準の名前 */
  var groupRoomDefaultName = "New GroupRoom"
  /** {@code SmartRoom} 型の部屋を作成するときの標準の名前 */
  var smartRoomDefaultName = "New SmartRoom"
  
  /** ユーザー部屋 */
  private var roomService: Option[UserExhibitRoomService] = None
  
  /** ローカルライブラリ用展示物サービス */
  private var exhibitService: Option[MuseumExhibitService] = None
  
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
  
  /** サービスの要素変化監視 */
  val roomServiceChangeListener = new PropertyChangeListener {
    private def getOptionRoom(room: AnyRef): ExhibitRoom = room.asInstanceOf[Option[_]] match {
      case Some(parent) => parent.asInstanceOf[UserExhibitRoom]
      case None => userRoomsRoot
    }
    
    def propertyChange(evt: PropertyChangeEvent) = evt match {
      case MappedPropertyChangeEvent("children", key, _, _) =>
        val parent = getOptionRoom(key)
        fireChildrenChange(parent)
      case MappedPropertyChangeEvent("parent", room, oldValue, newValue) =>
        val oldParent = getOptionRoom(oldValue)
        val newParent = getOptionRoom(newValue)
        fireChildrenChange(oldParent)
        if (oldParent != newParent)
          fireChildrenChange(newParent)
      case MappedPropertyChangeEvent("exhibitList", room, _, _) =>
        // TODO
    }
  }
  
  /** ユーザー部屋の取得 */
  def userExhibitRoomService = roomService
  
  /** ユーザー部屋の設定 */
  def userExhibitRoomService_=(roomService: Option[UserExhibitRoomService]) {
    // 結合解除
    this.roomService.foreach(_.removePropertyChangeListener(roomServiceChangeListener))
    this.roomService = roomService
    roomService.foreach(_.addPropertyChangeListener(roomServiceChangeListener))
    
    val event = new TreeSource.MappedPropertyChangeEvent(this, "children", userRoomsRoot, null, null)
    firePropertyChange(event)
  }
  
  /** 展示物サービスを取得する */
  def museumExhibitService = exhibitService
  
  /**
   * 展示物サービスを設定する。
   * 
   * TODO ローカルライブラリとして、ツリー構造に追加される。
   */
  def museumExhibitService_=(exhibitService: Option[MuseumExhibitService]) {
    this.exhibitService = exhibitService
  }
  
  /**
   * 部屋の中身を取得する
   */
  def getContent(room: UserExhibitRoom) =
    room.exhibitListModel(userExhibitRoomService.get)
  
  override def getChildren(parent: ExhibitRoom): java.util.List[ExhibitRoom] = {
    import collection.JavaConverters._
    
    def emptyList = List.empty[ExhibitRoom].asJava
    
    val list = if (isLeaf(parent)) emptyList else parent match {
      // ユーザー設定部屋ルートの時は、サービスからのルート要素取得して返す
      case `userRoomsRoot` =>
        userExhibitRoomService map (_.getRoomList(None)) getOrElse emptyList
      // ユーザー設定部屋の時は、サービスから子要素を取得して返す
      case parent: UserExhibitRoom =>
        userExhibitRoomService.map(_.getRoomList(Some(parent))) getOrElse emptyList
      // MuseumFloor の時は、メソッドから子要素を返す
      case parent: MuseumFloor => parent.children.asJava
      // 該当が無い時は Nil
      case _ => emptyList
    }
    list.asInstanceOf[java.util.List[ExhibitRoom]]
  }
  
  /** 子要素を取得 */
  def childrenFor(parent: ExhibitRoom): List[ExhibitRoom] = {
    if (isLeaf(parent)) Nil
    else parent match {
      // ユーザー設定部屋ルートの時は、サービスからのルート要素取得して返す
      case `userRoomsRoot` =>
        userExhibitRoomService map (_.getChildren(None).toList) getOrElse Nil
      // ユーザー設定部屋の時は、サービスから子要素を取得して返す
      case parent: UserExhibitRoom =>
        userExhibitRoomService.map(_.getChildren(Some(parent)).toList) getOrElse Nil
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
  
  override def getValue(): ExhibitRoom = root
  
  override def setValue(room: ExhibitRoom) {
    // TODO
  }
  
  protected def updateNodeValue(room: ExhibitRoom, newValue: AnyRef) = room match {
    case room: UserExhibitRoom => update(room, newValue)
    case _ =>
  }
  
  /**
   * UserExhibitRoom の値を更新し、サービスへ更新を通知する。
   */
  protected def update(element: UserExhibitRoom, newValue: AnyRef) {
    newValue match {
      case value: String => element.name = value
      case _ =>
    }
    userExhibitRoomService.foreach(_.save(element))
  }
  
  /** 値の更新 */
  def update(path: IndexedSeq[ExhibitRoom], newValue: AnyRef): Unit = path.lastOption match {
    case Some(element: ExhibitRoom) => updateNodeValue(element, newValue)
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
          val parent = userExhibitRoomService.flatMap(_.getParent(room)).getOrElse(userRoomsRoot)
          getParent(parent, room :: path)
      }
    }
    
    getParent(node).toIndexedSeq
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
  def addRoom(roomType: RoomType, parent: Option[UserExhibitRoom]): UserExhibitRoom = {
    val name = findRoomNewName(roomType match {
      case BasicRoom => basicRoomDefaultName
      case GroupRoom => groupRoomDefaultName
      case SmartRoom => smartRoomDefaultName
    })
    
    val newRoom = userExhibitRoomService.get.addRoom(roomType, name, parent)
    newRoom
  }
  
  /**
   * 部屋の移動が可能か
   */
  def canMove(source: UserExhibitRoom, dest: Option[UserExhibitRoom]) = {
    dest match {
      case Some(dest @ RoomType(GroupRoom)) =>
        !pathToRoot(dest).startsWith(pathToRoot(source))
      case None => userExhibitRoomService.map(_.getParent(source).nonEmpty).getOrElse(false)
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
  def moveRoom(source: UserExhibitRoom, newParent: Option[UserExhibitRoom])  {
    val parent = newParent match {
      case Some(dest @ RoomType(GroupRoom)) => newParent
      case Some(nonGroupRoom) => userExhibitRoomService.get.getParent(nonGroupRoom) match {
        case parent @ Some(_  @ RoomType(GroupRoom)) => parent
        case _ => None
      }
      case None => None
    }
    
    userExhibitRoomService.get.setParent(source, parent)
  }
  
  /**
   * 部屋を削除する
   */
  def removeRoom(room: UserExhibitRoom) {
    userExhibitRoomService.foreach(_.remove(room))
  }
  
  private def fireChildrenChange(parent: ExhibitRoom) {
    val event = new TreeSource.MappedPropertyChangeEvent(this, "children", parent, null, null)
    firePropertyChange(event)
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
      userExhibitRoomService.map(_.nameExists(candidate)) match {
        case Some(true) => searchNext(index + 1)
        case _ => candidate
      }
    }
    
    userExhibitRoomService.map(_.nameExists(baseName)) match {
      case Some(true) => searchNext(1)
      case _ => baseName
    }
  }
}

import java.beans.PropertyChangeEvent
import TreeSource.{MappedPropertyChangeEvent => MappedEvent}

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
  
  private object MappedPropertyChangeEvent {
    def unapply(event: PropertyChangeEvent): Option[(String, AnyRef, AnyRef, AnyRef)] = event match{
      case event: MappedEvent =>
        Some(event.getPropertyName, event.getKey, event.getOldValue, event.getNewValue)
      case _ => None
    }
  }
}
