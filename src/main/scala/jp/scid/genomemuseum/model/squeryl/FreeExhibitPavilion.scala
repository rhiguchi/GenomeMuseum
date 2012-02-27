package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{GlazedLists, CollectionList, EventList, FunctionList}

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  ExhibitMuseumFloor => IExhibitMuseumFloor, ExhibitMuseumSpace => IExhibitMuseumSpace,
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
      service.getExhibitRoomModel(room)
    }
    
    def dispose(room: IUserExhibitRoom, roomModel: ExhibitMuseumSpace) {
      roomModel.dispose()
    }
  }

  /** 展示室モデルから展示物エンティティリストを取得する機能関数 */
  class RoomExhibitCollectionModel extends CollectionList.Model[IExhibitMuseumSpace, IMuseumExhibit] {
    def getChildren(room: IExhibitMuseumSpace) = room.getValue
  }
  
  /** コンテンツ情報のリスト */
  class RoomContentEventList(contentTable: Table[RoomExhibit], room: IUserExhibitRoom)
      extends KeyedEntityEventList(contentTable) {
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
class FreeExhibitPavilion(contentTable: Table[RoomExhibit]) extends IFreeExhibitPavilion {
  import FreeExhibitPavilion._
  import collection.mutable.WeakHashMap

  // 作成した展示室のマップ
  private val roomMap = new WeakHashMap[IUserExhibitRoom, ExhibitMuseumSpace]

  // プロパティ
  /** 展示物サービス */
  var exhibitEventList: Option[KeyedEntityEventList[MuseumExhibit]] = None

  /** 部屋サービス */
  var roomService: Option[IUserExhibitRoomService] = None
  
  /** 
   * 部屋サービスと展示物サービスも同時に設定する
   * 
   * @param exhibitLookupper 部屋の中身から展示物を取得する関数
   */
  def this(contentTable: Table[RoomExhibit], 
      exhibitEventList: KeyedEntityEventList[MuseumExhibit],
      roomService: IUserExhibitRoomService) {
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
  /** サービスは自身を返す */
  protected def freeExhibitPavilion = this
  
  def name = "Free Area"
  
  /** 最上層の部屋のリストを返す */
  lazy val childRoomList =
    createChildRoomList(None).asInstanceOf[EventList[IExhibitMuseumSpace]]
  
  /** 部屋を子に設定できるか */
  def canAddRoom(room: IExhibitMuseumSpace): Boolean = canSetParent(room, None)
  
  /** 子部屋に設定 */
  def addRoom(room: IExhibitMuseumSpace) = setParent(room, None)
   
  // 展示室操作
  /**
   * 部屋のプロパティを保存する
   */
  def save(room: IExhibitMuseumSpace) = roomService.foreach(_.save(room.roomModel))
  
  /** 部屋を追加する */
  def addRoom(roomType: RoomType, name: String, parent: Option[IExhibitMuseumFloor]): IExhibitMuseumSpace = {
    import collection.JavaConverters._
    
    val parentRoomModel = parent match {
      case model: IExhibitMuseumSpace => Some(model.roomModel)
      case _ => None
    }
    
    val room = roomService.get.addRoom(roomType, name, parentRoomModel)
    getExhibitRoomModel(room)
  }
  
  /** 部屋を削除する */
  def removeRoom(room: IExhibitMuseumSpace) = roomService foreach (_.remove(room.roomModel))
  
  /**
   * 部屋に親を設定できるか
   */
  protected[squeryl] def canSetParent(room: IExhibitMuseumSpace, floor: Option[IExhibitMuseumFloor]) = {
    // 循環参照にならないように、祖先に子要素候補がいないか調べる
    def ancester(space: IExhibitMuseumFloor): Boolean = getParent(space) match {
      case Some(`floor`) => false
      case Some(parent) => space.roomModel != parent.roomModel match {
        case true => ancester(parent)
        case false => false
      }
      case None => true
    }
    
    floor match {
      case Some(`room`) => false
      case Some(parent) => ancester(parent)
      case None => getParent(room).nonEmpty
    }
  }
  
  /**
   * 部屋の親を取得する
   */
  def getParent(room: IExhibitMuseumSpace): Option[IExhibitMuseumFloor] =
    roomService.flatMap(_.getParent(room.roomModel))
      .map(getExhibitRoomModel).map(_.asInstanceOf[IExhibitMuseumFloor])
  
  /**
   * 部屋の親を設定する
   */
  protected[squeryl] def setParent(room: IExhibitMuseumSpace, floor: Option[IExhibitMuseumFloor]) =
    roomService.foreach(_.setParent(room.roomModel, floor.map(_.roomModel))) 
  
  /**
   * 子部屋リストを返す
   */
   protected[squeryl] def createChildRoomList(parent: Option[ExhibitMuseumFloor]): EventList[ExhibitMuseumSpace] = {
    val convertFunc = new ExhibitRoomModelFunction(this)
  val roomList = roomService.get.getFloorRoomList(parent.map(_.roomModel))
    new FunctionList(roomList, convertFunc)
  }
  
  /**
   * 部屋のコンテンツの EventList を作成
   */
  protected[squeryl] def createRoomContentEventList(room: IUserExhibitRoom) = {
    val list = new RoomContentEventList(contentTable, room)
    
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
      case GroupRoom => new ExhibitFloor
    }
    roomModel.roomModel = room
    roomModel
  }
  
  /**
   * 階層と展示物データリストのアダプター
   * 
   * @param contentList 部屋内容
   * @param contanerToExhibitFunction 部屋内容と展示物の変換関数
   */
  class ExhibitFloor extends ExhibitMuseumSpace with ExhibitMuseumFloor {
    /** 展示物リスト */
    lazy val contentList = new CollectionList(childRoomList, new RoomExhibitCollectionModel)
    
    def freeExhibitPavilion = FreeExhibitPavilion.this
    
    /** 子部屋リストを更新する */
    override def getValue() = contentList
  }
}

