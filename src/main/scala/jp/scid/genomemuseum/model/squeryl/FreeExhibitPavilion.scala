package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{GlazedLists, CollectionList, EventList, BasicEventList, FunctionList}

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  ExhibitMuseumSpace => IExhibitMuseumSpace,
  ExhibitPavilionFloor => IExhibitPavilionFloor,
  ExhibitRoomModel => IExhibitRoomModel, MuseumExhibit => IMuseumExhibit,
  UserExhibitRoomService => IUserExhibitRoomService, FreeExhibitPavilion => IFreeExhibitPavilion}
import IUserExhibitRoom.RoomType._

object FreeExhibitPavilion {
  import ca.odell.glazedlists.event.{ListEventListener, ListEvent}
  
  /**
   * 関係の親要素が削除されたことを反映させるためのクラス
   * @tparam E 親要素の型
   * @tparam C 子要素の型
   */
  class ContentsParentChangeHandler[E](childList: RoomContentEventList) extends ListEventListener[E] {
    def listChanged(listChanges: ListEvent[E]) {
      val changeList = Iterator.continually(listChanges)
        .takeWhile(_.hasNext).map(e => (e.getType, e.getIndex)).toList
      
      if (changeList.contains(ListEvent.DELETE)) {
        childList.reload()
      }
    }
  }
  
  /** 展示物を追加した時に、部屋の中身として作成される関数 */
  class ExhibitContainerReverseFunction(room: IUserExhibitRoom)
      extends FunctionList.Function[IMuseumExhibit, RoomExhibit] {
    def evaluate(exhibit: IMuseumExhibit) = RoomExhibit(room.id, exhibit.id)
  }
  
  /**
   * 展示室データモデルから展示室モデルを作成する関数
   */
  class ExhibitRoomModelFunction(service: FreeExhibitPavilion)
      extends FunctionList.AdvancedFunction[IUserExhibitRoom, ExhibitMuseumSpace] {
    def evaluate(room: IUserExhibitRoom) =
      service.getExhibitRoomModel(room)
    
    def reevaluate(room: IUserExhibitRoom, roomModel: ExhibitMuseumSpace) = {
      service.roomMap.remove(room)
      service.getExhibitRoomModel(room)
    }
    
    def dispose(room: IUserExhibitRoom, roomModel: ExhibitMuseumSpace) {
      service.roomMap.remove(room)
    }
  }

  /** 展示室モデルから展示物エンティティリストを取得する機能関数 */
  class RoomExhibitCollectionModel extends CollectionList.Model[IExhibitMuseumSpace, IMuseumExhibit] {
    def getChildren(room: IExhibitMuseumSpace) = room.getValue
  }
  
  /** コンテンツ情報のリスト */
  class RoomContentEventList(eventList: EventList[RoomExhibit], contentTable: Table[RoomExhibit], room: IUserExhibitRoom)
      extends KeyedEntityEventList(eventList, contentTable) {
    /** 部屋 ID が `roomId` であるコンテンツを取得するクエリを返す */
    override def getFetchQuery(e: RoomExhibit) = where(e.roomId === room.id)
    
    /**
     * 項目番目の展示物を変更する。
     */
    override def set(index: Int, newElement: RoomExhibit) = {
      require(newElement.roomId == room.id, "newElement requires same room id to update for room content")
      require(newElement.exhibitId > 0, "newElement requires positive ehixibit id")
      
      val oldElement = get(index)
      newElement.exhibitId = oldElement.id
      super.set(index, newElement)
    }
  }
}

/**
 * 自由展示棟の実装
 */
class FreeExhibitPavilion(contentTable: Table[RoomExhibit]) extends IFreeExhibitPavilion with ExhibitPavilionFloor {
  import FreeExhibitPavilion._
  import collection.mutable.WeakHashMap

  // 作成した展示室のマップ
  private val roomMap = new WeakHashMap[IUserExhibitRoom, ExhibitMuseumSpace]

  // プロパティ
  /** 展示物サービス */
  var exhibitEventList: Option[KeyedEntityEventList[MuseumExhibit]] = None

  /** 部屋サービス */
  var roomService: Option[UserExhibitRoomService] = None
  
  /** 
   * 部屋サービスと展示物サービスも同時に設定する
   * 
   * @param exhibitLookupper 部屋の中身から展示物を取得する関数
   */
  def this(contentTable: Table[RoomExhibit], 
      exhibitEventList: KeyedEntityEventList[MuseumExhibit],
      roomService: UserExhibitRoomService) {
    this(contentTable)
    
    this.roomService = Option(roomService)
    this.exhibitEventList = Option(exhibitEventList)
  }

  /** 部屋の中身から展示物を取得する関数 */
  private val exhibitLookupper = new FunctionList.Function[RoomExhibit, IMuseumExhibit]() {
    def evaluate(c: RoomExhibit) = exhibitEventList.get.findOrNull(c.exhibitId) match {
      case null => MuseumExhibit("Nil")
      case exhibit => exhibit
    }
  }
  
  // ExhibitFloor 実装
  /** ノード名 */
  def name = "Free Area"
  
  /** 最上層の部屋のリストを返す */
  protected def freeExhibitPavilion = this
   
  // 展示室操作
  /**
   * 部屋のプロパティを保存する
   */
  def save(room: IExhibitMuseumSpace) = roomService.foreach(_.save(room.roomModel))
  
  /** 部屋を追加する */
  def addRoom(roomType: RoomType, name: String, floor: IExhibitPavilionFloor): IExhibitMuseumSpace = {
    val room = roomService.get.addRoom(roomType, name, floor)
    getExhibitRoomModel(room)
  }
  
  /** 部屋を削除する */
  def removeRoom(room: IExhibitMuseumSpace) = roomService foreach (_.remove(room.roomModel))
  
  /**
   * 部屋に親を設定できるか
   */
  protected[squeryl] def canSetParent(room: IExhibitMuseumSpace, floor: IExhibitPavilionFloor) = {
    // 循環参照にならないように、祖先に子要素候補がいないか調べる
    def ancester(space: IExhibitPavilionFloor): Boolean = space match {
      case `room` => false
      case space: IExhibitMuseumSpace => ancester(getParent(space))
      case _ => true
    }
    
    ancester(floor)
  }
  
  /**
   * 部屋の親を取得する
   */
  def getParent(room: IExhibitMuseumSpace): IExhibitPavilionFloor = roomService
      .flatMap(_.getParent(room.roomModel)).map(getExhibitRoomModel) match {
    case None => this
    case Some(floor: IExhibitPavilionFloor) => floor
    case invalid => throw new IllegalStateException(
      "cannot find the parent of '%s', but invalid '%s' found.".format(room, invalid))
  }
  
  /**
   * 部屋の親を設定する
   */
  protected[squeryl] def setParent(room: IExhibitMuseumSpace, floor: IExhibitPavilionFloor) =
    roomService.foreach(_.setParent(room.roomModel, floor)) 
  
  /**
   * 子部屋リストを返す
   */
  protected[squeryl] def createChildRoomList(parent: IExhibitPavilionFloor): EventList[ExhibitMuseumSpace] = {
    val convertFunc = new ExhibitRoomModelFunction(this)
    val roomList = roomService.get.getFloorRoomList(parent)
    new FunctionList(roomList, convertFunc)
  }
  
  /** 部屋を部屋モデルのオプション値へ変換 */
  private implicit def convertToRoom(parent: IExhibitPavilionFloor): Option[IUserExhibitRoom] = parent match {
    case model: IExhibitMuseumSpace => Some(model.roomModel)
    case _ => None
  }
  
  /**
   * 部屋のコンテンツの EventList を作成
   */
  protected[squeryl] def createRoomContentEventList(room: IUserExhibitRoom) = {
    val baseList = new BasicEventList[RoomExhibit](roomService.get.getPublisher, roomService.get.getReadWriteLock)
    val list = new RoomContentEventList(baseList, contentTable, room)
    
    val changeHandler = new ContentsParentChangeHandler[MuseumExhibit](list)
    exhibitEventList foreach { list =>
      val listenerProxy = GlazedLists.weakReferenceProxy(list, changeHandler)
      list.addListEventListener(listenerProxy)
    }
    list
  }
  
  /** 部屋のデータモデルを返す */
  def getExhibitRoomModel(room: IUserExhibitRoom): ExhibitMuseumSpace =
    roomMap.getOrElseUpdate(room, createExhibitRoomModel(room))
  
  /** 部屋のデータモデルを作成する */
  private def createExhibitRoomModel(room: IUserExhibitRoom) = {
    lazy val exhibitEventList = new FunctionList(
        createRoomContentEventList(room), exhibitLookupper, new ExhibitContainerReverseFunction(room))
    
    val roomModel = room.roomType match {
      case BasicRoom => new ExhibitMuseumSpace(exhibitEventList) with FreeExhibitRoomModel
      case SmartRoom => new ExhibitMuseumSpace(exhibitEventList)
      case GroupRoom => new ExhibitMuseumFloor
    }
    roomModel.roomModel = room
    roomModel
  }
  
  /** 名前 */
  override def toString = name
  
  /**
   * 階層と展示物データリストのアダプター
   * 
   * @param contentList 部屋内容
   * @param contanerToExhibitFunction 部屋内容と展示物の変換関数
   */
  class ExhibitMuseumFloor extends ExhibitMuseumSpace with ExhibitPavilionFloor {
    /** 展示物リスト */
    lazy val contentList = new CollectionList(childRoomList, new RoomExhibitCollectionModel)
    
    def freeExhibitPavilion = FreeExhibitPavilion.this
    
    /** 子部屋リストを更新する */
    override def getValue() = contentList
  }
}

