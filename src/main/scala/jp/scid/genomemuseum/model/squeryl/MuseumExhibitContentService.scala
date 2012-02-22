package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{CompositeList, EventList}

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  UserExhibitRoomService => IUserExhibitRoomService}
import IUserExhibitRoom.RoomType._

class MuseumExhibitContentService(
    exhibitTable: Table[MuseumExhibit])
    extends MuseumExhibitService(exhibitTable) {
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
   * 部屋のコンテンツを取得する
   * @param contentTable 部屋の中身テーブル
   */
  def getContentList(contentTable: Table[RoomExhibit], room: IUserExhibitRoom) = room.roomType match {
    case BasicRoom => new RoomContentEventList(contentTable, room)
    case SmartRoom => new RoomContentEventList(contentTable, room)
    case GroupRoom =>
      val roomContents = new FloorContents(contentTable, room)
      roomContents setContentService this
      roomContents.contentList
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
  
  class RoomContentEventList(contentTable: Table[RoomExhibit], room: IUserExhibitRoom)
      extends KeyedEntityEventList(contentTable) {
    /** 部屋 ID が `roomId` であるコンテンツを取得するクエリを返す */
    override def getFetchQuery(e: RoomExhibit) = where(e.roomId === room.id) select(e)
    // TODO listen to service
  }
  
  class FloorContents(contentTable: Table[RoomExhibit], room: IUserExhibitRoom) {
    private var service: MuseumExhibitContentService = _
    
    /** 子部屋リスト */
    var childRoomList: List[EventList[RoomExhibit]] = Nil
    
    /** この部屋の展示物リスト */
    val contentList = new CompositeList[RoomExhibit]
    
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
}

