package jp.scid.genomemuseum.model.squeryl

import org.squeryl.Table
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.OneToManyRelation

import collection.script.{End, Include, Update, Remove, Index, NoLo}
import collection.mutable.Publisher

import jp.scid.genomemuseum.model.{MuseumExhibitService => IMuseumExhibitService,
  UriFileStorage}
import SquerylTriggerAdapter.{TableOperation, Inserted, Updated, Deleted}

/**
 * ローカルライブラリ中の {@link jp.scid.genomemuseum.model.MuseumExhibit}
 * データサービス。
 */
private[squeryl] class MuseumExhibitService(
    val exhibitRelation: OneToManyRelation[MuseumExhibit, RoomExhibit],
    val roomTable: Table[UserExhibitRoom]) extends IMuseumExhibitService with RoomElementService
    with MuseumExhibitPublisher {
  type ElementClass = MuseumExhibit
  
  private var fileStorage: Option[UriFileStorage] = None
  
  def museumExhibitTablePublisher = SquerylTriggerAdapter.connect(exhibitRelation.leftTable, 7)
  
  /**
   * このサービスが持つ全ての要素を取得する。
   * @return 全ての要素の {@code List} 。
   */
  def allElements = inTransaction {
    from(exhibitTable)( e => select(e) orderBy(e.id asc)).toList
  }
  
  /**
   * Squeryl MuseumExhibit エンティティを作成する。
   * 永続化はされないが、 {@link allElements} では要素が返される。
   */
  def create() = MuseumExhibit("")
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: MuseumExhibit): Boolean = inTransaction {
      exhibitTable.delete(element.id)
  }
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素がまだサービスに永続化されていない時は、永続化される。
   * 要素がこのサービスに存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def save(element: ElementClass) = inTransaction {
    exhibitTable.insertOrUpdate(element)
  }
  
  /** MuseumExhibit のローカルファイル管理オブジェクトを取得する */
  def localFileStorage = fileStorage
  
  /** MuseumExhibit のローカルファイル管理オブジェクトを設定する */
  def localFileStorage_=(newStorage: Option[UriFileStorage]) {
    fileStorage = newStorage
    MuseumExhibit.defaultStorage = newStorage
  }
}

import collection.script.Message

private[squeryl] abstract class ObservableListDataService[A](
    observedTable: Publisher[TableOperation[A]]) {
  
  private val sub = new observedTable.Sub {
    def notify(pub: observedTable.Pub, event: TableOperation[A]) {
      ObservableListDataService.this.notify(event)
    }
  }
  observedTable.subscribe(sub)
  
  protected def notify(event: TableOperation[A])
}
