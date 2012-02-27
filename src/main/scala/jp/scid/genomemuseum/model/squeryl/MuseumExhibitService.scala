package jp.scid.genomemuseum.model.squeryl

import ref.WeakReference

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._

import ca.odell.glazedlists.GlazedLists

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  MuseumExhibitService => IMuseumExhibitService, UriFileStorage,
  MuseumExhibit => IMuseumExhibit}

/**
 * 全ローカル展示物所有クラス
 * 
 * @param exhibitTable 展示物テーブル
 */
class MuseumExhibitService(exhibitTable: Table[MuseumExhibit]) extends IMuseumExhibitService {
  /** 全展示物リスト */
  val exhibitEventList = new KeyedEntityEventList(exhibitTable)
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: IMuseumExhibit): Boolean =
    exhibitEventList.remove(element)
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素が {@link #create()} で作成され、まだサービスに永続化されていない時は、永続化される。
   * それ以外は無視される。
   * @param element 保存を行う要素。
   */
  def add(element: IMuseumExhibit) = element match {
    case exhibit: MuseumExhibit if !exhibit.isPersisted =>
      exhibitEventList.add(exhibit)
    case _ => false
  }
  
  /**
   * Squeryl MuseumExhibit エンティティを作成する。
   * 永続化はされないが、 {@link allElements} では要素が返される。
   */
  def create() = MuseumExhibit("No Name")
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素がまだサービスに永続化されていない時は、永続化される。
   * 要素がこのサービスに存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def save(element: IMuseumExhibit) = element match {
    case exhibit: MuseumExhibit if exhibit.isPersisted =>
      exhibitEventList.elementChanged(exhibit)
    case _ =>
  }
  
  /** インデックス値に関係なく展示物を保存 */
  def set(index: Int, element: IMuseumExhibit) = save(element)
  
  /** この行の展示物を除去 */
  def remove(index: Int): IMuseumExhibit = exhibitEventList.remove(index)
  
  /** {@inheritDoc} */
  override def getValue() = exhibitEventList.asInstanceOf[java.util.List[IMuseumExhibit]]
  
  val roomModel = UserExhibitRoom("All Artifacts")
}
