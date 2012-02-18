package jp.scid.genomemuseum.model.squeryl

import ref.WeakReference

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  MuseumExhibitService => IMuseumExhibitService, UriFileStorage,
  MuseumExhibit => IMuseumExhibit, MutableMuseumExhibitListModel => IMutableMuseumExhibitListModel,
  GroupRoomContentsModel}

/**
 * 全ローカルファイル所有クラス
 */
class MuseumExhibitService(
  exhibitTable: Table[MuseumExhibit],
  roomTable: Table[UserExhibitRoom]
) extends IMuseumExhibitService with GroupRoomContentsModel with IMutableMuseumExhibitListModel {
  import UserExhibitRoomService.getParentId
  /** 作成するエンティティクラス */
  type ElementClass = MuseumExhibit
  
  private var exhibitListRef = new WeakReference(null: IndexedSeq[MuseumExhibit])
  
  override def userExhibitRoom = None
  
  /** 展示物 */
  def exhibitList = getExhibits.toList
  
  private def getExhibits() = exhibitListRef.get match {
    case Some(list) => list
    case None =>
      val list = retrieve()
      updateExhibitsReference(list)
      list
  }
  
  private def updateExhibitsReference(exhibits: IndexedSeq[MuseumExhibit]) {
    exhibitListRef = new WeakReference(exhibits)
  }
  
  private def retrieve() = inTransaction {
    from(exhibitTable)( e => select(e) orderBy(e.id asc)).toIndexedSeq
  }
  
  /**
   * 親IDが存在する部屋は {@code true} 。
   */
  def canAddChild(target: IUserExhibitRoom) = {
    getParentId(target.id, roomTable).nonEmpty
  }
  
  /**
   * 親IDを除去する
   */
  def addChild(element: IUserExhibitRoom) = inTransaction {
    update(roomTable) ( e =>
      where(e.id === element.id)
      set(e.parentId := None)
    )
  }
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: IMuseumExhibit): Boolean = {
    val exhibits = getExhibits
        
    val removed = inTransaction {
      exhibitTable.delete(element.id)
    }
    exhibits.indexOf(element) match {
      case -1 => false
      case index =>
        val newExhibits = exhibits.take(index) ++ exhibits.drop(index + 1)
        updateExhibitsReference(newExhibits)
        
        fireValueIndexedPropertyChange(index, exhibits, null)
        true
    }
    
    removed
  }
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素が {@link #create()} で作成され、まだサービスに永続化されていない時は、永続化される。
   * それ以外は無視される。
   * @param element 保存を行う要素。
   */
  def add(element: IMuseumExhibit) = element match {
    case exhibit: ElementClass if !exhibit.isPersisted =>
      val exhibits = getExhibits
      val newExhibits = exhibits :+ exhibit
      updateExhibitsReference(newExhibits)
      
      inTransaction {
        exhibitTable.insert(exhibit)
      }
      
      fireValueIndexedPropertyChange(exhibits.size, null, newExhibits)
      true
    case _ => false
  }
  
  /**
   * Squeryl MuseumExhibit エンティティを作成する。
   * 永続化はされないが、 {@link allElements} では要素が返される。
   */
  def create(): ElementClass = MuseumExhibit("")
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素がまだサービスに永続化されていない時は、永続化される。
   * 要素がこのサービスに存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def save(element: IMuseumExhibit) = element match {
    case exhibit: ElementClass if exhibit.isPersisted =>
      val index = getExhibits.indexOf(exhibit)
      inTransaction {
        exhibitTable.update(exhibit)
      }
      
      index match {
        case -1 =>
        case index => fireValueIndexedPropertyChange(index, null, null)
      }
    case _ =>
  }
}
