package jp.scid.genomemuseum.model.squeryl

import ref.WeakReference

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.GlazedLists

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  ExhibitFloorModel => IExhibitFloorModel,
  MuseumExhibitService => IMuseumExhibitService, UriFileStorage,
  MuseumExhibit => IMuseumExhibit, MutableMuseumExhibitListModel => IMutableMuseumExhibitListModel,
  GroupRoomContentsModel}
import jp.scid.gui.model.AbstractPersistentEventList


/**
 * 全ローカルファイル所有クラス
 */
class MuseumExhibitService(
    exhibitTable: Table[MuseumExhibit],
    roomTable: Table[UserExhibitRoom])
    extends AbstractPersistentEventList[MuseumExhibit](GlazedLists.comparableComparator())
    with IMuseumExhibitService
//    with IExhibitFloorModel
    with IMutableMuseumExhibitListModel {
  import UserExhibitRoomService.getParentId
  /** 作成するエンティティクラス */
  type ElementClass = MuseumExhibit
  
  override def userExhibitRoom = None
  
  /** 展示物 */
  def exhibitList = {
    import collection.JavaConverters._
    this.asScala.toIndexedSeq
  }
  
  private def retrieve() = inTransaction {
    from(exhibitTable)( e => select(e) orderBy(e.id asc)).toIndexedSeq
  }
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: IMuseumExhibit): Boolean = {
    indexOf(element) match {
      case -1 => false
      case index =>
        super.remove(index)
        true
    }
  }
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素が {@link #create()} で作成され、まだサービスに永続化されていない時は、永続化される。
   * それ以外は無視される。
   * @param element 保存を行う要素。
   */
  def add(element: IMuseumExhibit) = element match {
    case exhibit: ElementClass if !exhibit.isPersisted =>
      super.add(exhibit)
    case _ => false
  }
  
  /**
   * Squeryl MuseumExhibit エンティティを作成する。
   * 永続化はされないが、 {@link allElements} では要素が返される。
   * @deprecated add で追加と永続化が行われる。
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
      elementChanged(exhibit)
    case _ =>
  }
  
  override def getValue() = this.asInstanceOf[java.util.List[IMuseumExhibit]]
  
  // Read
  override def fetch() = inTransaction {
    import collection.JavaConverters._
    from(exhibitTable)( e => select(e) orderBy(e.id asc)).toIndexedSeq.asJava
  }
  
  // Insert
  override def insertToTable(index: Int, element: MuseumExhibit) = inTransaction {
    exhibitTable.insert(element)
  }
  
  // Update
  override def updateToTable(element: MuseumExhibit) = inTransaction {
    exhibitTable.update(element)
  }
  
  // Delete
  override def deleteFromTable(entity: MuseumExhibit) = inTransaction {
    exhibitTable.delete(entity.id)
  }
}
