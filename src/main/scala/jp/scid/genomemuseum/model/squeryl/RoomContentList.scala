package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.{GlazedLists, CollectionList, FunctionList,
  BasicEventList, EventList}

import jp.scid.genomemuseum.model.{FreeExhibitRoomModel => IFreeExhibitRoomModel,
  MuseumExhibit => IMuseumExhibit, ExhibitFloorModel => IExhibitFloorModel,
  UserExhibitRoom => IUserExhibitRoom}

object FreeExhibitRoomModel {
  
  /** テーブルオブジェクトから変換関数を作成 */
  class RoomContentEventList(roomId: Long, table: Table[RoomExhibit])
      extends KeyedEntityEventList(table) {
    /** 部屋 ID が `roomId` であるコンテンツを取得するクエリを返す */
    override def getFetchQuery(e: RoomExhibit) = where(e.roomId === roomId) select(e)
  }
}

/**
 * 自由に展示物を入れ替えできる部屋のモデル
 * 
 * @param roomId 部屋の ID
 * @param table 部屋内容テーブル
 */
class FreeExhibitRoomModel(
    room: IUserExhibitRoom, contentList: EventList[RoomExhibit],
    contanerToExhibitFunction: FunctionList.Function[RoomExhibit, MuseumExhibit])
    extends ExhibitRoomModel(room, contentList, contanerToExhibitFunction)
    with IFreeExhibitRoomModel {
  
  /** テーブルオブジェクトから構築 */
  def this(room: IUserExhibitRoom, contentTable: Table[RoomExhibit],
      contanerToExhibitFunction: FunctionList.Function[RoomExhibit, MuseumExhibit]) {
    this(room, new FreeExhibitRoomModel.RoomContentEventList(room.id, contentTable), contanerToExhibitFunction)
  }
  
  //
  // コンテンツ
  /**
   * 要素を追加する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def addContent(element: IMuseumExhibit) =
    contentList.add(RoomExhibit(roomId, element.id))
  
  /**
   * インデックスの展示物を置換する。
   * 
   * 要素を ID で DB からルックアップし、存在するときは追加される。
   * 要素が存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def setContent(index: Int, element: IMuseumExhibit) = {
    val oldExhibit = get(index)
    val content = contentList.get(index)
    content.exhibitId = element.id
    contentList.set(index, content)
    oldExhibit
  }
  
  /**
   * 指定項目番目の要素を除去する。
   */
  def removeContent(index: Int) = {
    val removed = get(index)
    contentList.remove(index)
    removed
  }
}

/**
 * 部屋と展示物データリストのアダプター
 * 
 * @param roomId 部屋の ID
 * @param table 部屋内容テーブル
 * @todo SmartRoom 用構築
 */
class CompositeRoomList(
    parentRoom: IUserExhibitRoom,
    contentList: EventList[RoomExhibit],
    contanerToExhibitFunction: FunctionList.Function[RoomExhibit, MuseumExhibit],
    roomService: UserExhibitRoomService)
    extends ExhibitRoomModel(parentRoom, contentList, contanerToExhibitFunction)
    with IExhibitFloorModel {
//  /** 管理用のシングルトンリスト */
//  private val parentRoomList = Glazedlists.eventListOf(parentRoom)
  
//  /** 配下にあるすべての部屋のリスト */
//  val childRoomList = new CollectionList(parentRoomList, ChildRoomModel)
//  
//  /** 配下にあるすべての部屋のリスト */
//  val childRoomList = new CollectionList(parentRoomList, ChildRoomModel)
  
  def canAddRoom(target: IUserExhibitRoom) = {
    // 循環参照にならないように、祖先に子要素候補がいないか調べる
    def ancester(room: IUserExhibitRoom): Boolean = {
      roomService.getParent(room) match {
        case None => true
        case Some(`parentRoom` | `target`) => false
        case Some(parent) => ancester(parent)
      }
    }
    
    parentRoom == target match {
      case true => false
      case false => ancester(parentRoom)
    }
  }
  
  /** {@inheritDoc} */
  def addRoom(element: IUserExhibitRoom) =
    roomService.setParent(element, Some(parentRoom))
  
//  /** この部屋以下すべての部屋を返す変換モデル */
//  private object ChildRoomModel extends CollectionList.Model[IUserExhibitRoom, IUserExhibitRoom] {
//    override def getChildren(parentRoom: IUserExhibitRoom) = {
//      import collection.JavaConverters._
//      
//      roomService.getAllLeafs(parentRoom).asJava
//    }
//  }
//  
//  /** コンテンツリストを返す関数 */
//  private object RoomContentListFunction extends FunctionList.Function[IUserExhibitRoom, EventList[RoomExhibit]] {
//    def evaluate(room: IUserExhibitRoom) =
//      new RoomContentList(room, table)
//  }
}