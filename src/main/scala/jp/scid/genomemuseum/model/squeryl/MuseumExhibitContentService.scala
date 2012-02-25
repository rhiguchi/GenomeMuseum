package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{GlazedLists, CompositeList, EventList, FunctionList}

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  ExhibitFloorModel => IExhibitFloorModel, MuseumExhibit => IMuseumExhibit,
  UserExhibitRoomService => IUserExhibitRoomService}
import IUserExhibitRoom.RoomType._

object MuseumExhibitContentService {
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
 * 部屋の中身を取り扱うことができるサービス
 */
class MuseumExhibitContentService(
    exhibitTable: Table[MuseumExhibit])
    extends MuseumExhibitService(exhibitTable)
    with IExhibitFloorModel {
  import MuseumExhibitContentService._
  
  /** 部屋サービスも同時に設定する */
  def this(exhibitTable: Table[MuseumExhibit], roomService: IUserExhibitRoomService) {
    this(exhibitTable)
    
    setRoomService(roomService)
  }

  /** 部屋サービス */
  private var roomService: Option[IUserExhibitRoomService] = None

  /**
   * 部屋サービスを設定する。
   * 
   * 部屋の親子関係のデータ処理を行えるようになる。
   */
  def setRoomService(newRoomService: IUserExhibitRoomService) {
    roomService = Option(newRoomService)
  }
  
  /**
   * 親が存在する部屋は {@code true} 。
   */
  def canAddRoom(target: IUserExhibitRoom) =
    roomService.map(_.getParent(target).nonEmpty) getOrElse false
  
  /**
   * 親を除去する
   */
  def addRoom(element: IUserExhibitRoom) =
    roomService.get.setParent(element, None)
  
  /**
   * 部屋のコンテンツを作成
   */
  def createRoomContentEventList(contentTable: Table[RoomExhibit], room: IUserExhibitRoom) = {
    val list = new RoomContentEventList(contentTable, room)
    val changeHandler = new ContentsParentChangeHandler[MuseumExhibit](list)
    val listenerProxy = GlazedLists.weakReferenceProxy(exhibitEventList, changeHandler)
    exhibitEventList.addListEventListener(listenerProxy)
    list
  }
  
  /**
   * 部屋のコンテンツを取得する
   * @param contentTable 部屋の中身テーブル
   */
  protected[squeryl] def getContentList(contentTable: Table[RoomExhibit], room: IUserExhibitRoom) = room.roomType match {
    case BasicRoom => createRoomContentEventList(contentTable, room)
    case SmartRoom => createRoomContentEventList(contentTable, room)
    case GroupRoom =>
      val roomContents = new FloorContents(contentTable, room)
      roomContents setContentService this
      roomContents.contentList
  }
  
  /** 部屋のデータモデルを作成する */
  protected[squeryl] def createExhibitRoomModel(contentTable: Table[RoomExhibit], room: IUserExhibitRoom) = {
    val contentList = getContentList(contentTable, room)
    val reverseFunction = new ExhibitContainerReverseFunction(room)
    val exhibitEventList = new FunctionList(contentList, containerToExhibitFunction, reverseFunction)
    
    val roomModel = room.roomType match {
      case BasicRoom => new FreeExhibitRoomModel(contentList)
      case SmartRoom => new ExhibitRoomModel
      case GroupRoom => new ExhibitFloorModel(roomService.get)
    }
    roomModel.exhibitEventList = exhibitEventList
    roomModel.sourceRoom = Some(room)
    roomModel
  }
  
  /**
   * この部屋の子部屋のコンテンツを取得する
   */
  private def getChildContentList(contentTable: Table[RoomExhibit], room: IUserExhibitRoom) = roomService match {
    case Some(service) =>
      val children = service getChildren Some(room)
      children map (c => getContentList(contentTable, c)) toList
    case None => Nil
  }
  
  /**
   * コンテンツの展示物を取得する
   */
  def getContentExhibit(container: RoomExhibit) = inTransaction {
    exhibitTable.lookup(container.exhibitId).getOrElse(create())
  }
  
  /**
   * @param contentList この部屋の展示物リスト
   */
  class FloorContents(
      val contentList: CompositeList[RoomExhibit],
      contentTable: Table[RoomExhibit],
      room: IUserExhibitRoom) {
    
    def this(contentTable: Table[RoomExhibit], room: IUserExhibitRoom) {
      this(new CompositeList[RoomExhibit], contentTable, room)
    }
    
    private var service: MuseumExhibitContentService = _
    
    /** 子部屋リスト */
    var childRoomList: List[EventList[RoomExhibit]] = Nil
    
    /** 子部屋リストを更新する */
    def updateChildRoomList() = {
      contentList.getReadWriteLock.writeLock.lock()
      try {
        childRoomList foreach contentList.removeMemberList
        
        childRoomList = service getChildContentList (contentTable, room)
        
        childRoomList foreach contentList.addMemberList
      }
      finally {
        contentList.getReadWriteLock.writeLock.unlock()
      }
    }
    
    def setContentService(newService: MuseumExhibitContentService) {
      service = newService
      updateChildRoomList()
    }
  }
  
  /** 部屋の中身から展示物を取得する関数 */
  private lazy val containerToExhibitFunction = new FunctionList.Function[RoomExhibit, IMuseumExhibit]() {
    def evaluate(container: RoomExhibit) = getContentExhibit(container)
  }
}

