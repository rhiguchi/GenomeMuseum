package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{GlazedLists, CollectionList, EventList, FunctionList}

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
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
   * ツリー構造情報を持つ博物館展示空間の変換クラス
   */
  class ExhibitRoomModelFunction(service: FreeExhibitPavilion)
      extends FunctionList.Function[IUserExhibitRoom, ExhibitRoomModel] {
    def evaluate(room: IUserExhibitRoom) =
      service.createExhibitRoomModel(room)
  }
  
  /** コンテンツ情報のリスト */
  class RoomContentEventList(contentTable: Table[RoomExhibit], room: IUserExhibitRoom)
      extends KeyedEntityEventList(contentTable) {
    /** 部屋 ID が `roomId` であるコンテンツを取得するクエリを返す */
    override def getFetchQuery(e: RoomExhibit) = where(e.roomId === room.id)
    
    /**
     * 要素を追加する。
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
class FreeExhibitPavilion(contentTable: Table[RoomExhibit])
    extends IFreeExhibitPavilion with MuseumFloor {
  import FreeExhibitPavilion._

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
  
  /** ルート要素なので階層は None */
  protected def floorModel = None

  def name = "Free Area"

  /**
   * 部屋に親を設定できるか
   */
  def canSetParent(target: IUserExhibitRoom, parent: Option[IUserExhibitRoom]) = {
    // 循環参照にならないように、祖先に子要素候補がいないか調べる
    def ancester(room: IUserExhibitRoom): Boolean = {
      roomService.flatMap(_.getParent(room)) match {
        case None => true
        case Some(`room` | `target`) => false
        case Some(parent) => ancester(parent)
      }
    }
    
    parent match {
      case None => roomService.flatMap(_.getParent(target)).nonEmpty
      case Some(`target`) => false
      case Some(parentRoom) => ancester(parentRoom)
    }
  }
  
  /**
   * 部屋の親を設定する
   */
  def setParent(target: IUserExhibitRoom, parent: Option[IUserExhibitRoom]) =
    roomService.foreach(_.setParent(target, parent)) 
  
  /**
   * 子部屋リストを返す
   */
  def createChildRoomList(parent: Option[IUserExhibitRoom]): EventList[ExhibitRoomModel] = {
    val convertFunc = new ExhibitRoomModelFunction(this)
    val roomList = roomService.map(_.getFloorRoomList(parent)) getOrElse GlazedLists.eventListOf()
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
  
  /** 部屋のデータモデルを作成する */
  protected[squeryl] def createExhibitRoomModel(room: IUserExhibitRoom) = {
    lazy val exhibitEventList = new FunctionList(
        createRoomContentEventList(room), exhibitLookupper, new ExhibitContainerReverseFunction(room))
    
    val roomModel = room.roomType match {
      case BasicRoom => new ExhibitRoomModel(exhibitEventList) with FreeExhibitRoomModel
      case SmartRoom => new ExhibitRoomModel(exhibitEventList)
      case GroupRoom => new ExhibitFloor
    }
    roomModel.sourceRoom = Some(room)
    roomModel
  }
  
  /**
   * 階層と展示物データリストのアダプター
   * 
   * @param contentList 部屋内容
   * @param contanerToExhibitFunction 部屋内容と展示物の変換関数
   */
  class ExhibitFloor extends ExhibitRoomModel with MuseumFloor {
    /** 展示物リスト */
    lazy val contentList = new CollectionList(childRoomList, new RoomExhibitCollectionModel)
    
    def freeExhibitPavilion = FreeExhibitPavilion.this
    
    /** 展示階層 */
    def floorModel = sourceRoom
    
    /** 子部屋リストを更新する */
    override def getValue() = contentList
  }

  class RoomExhibitCollectionModel extends CollectionList.Model[IExhibitRoomModel, IMuseumExhibit] {
    def getChildren(room: IExhibitRoomModel) = room.getValue
  }
}

