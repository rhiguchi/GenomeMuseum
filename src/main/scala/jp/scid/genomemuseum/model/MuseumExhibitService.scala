package jp.scid.genomemuseum.model

import collection.mutable.Publisher
import collection.script.Message

/**
 * MuseumExhibit データ提供サービスのインターフェイス。
 */
trait MuseumExhibitService extends Publisher[Message[_ <: MuseumExhibit]] {
  type ElementClass <: MuseumExhibit
  
  /**
   * このサービスが持つ全ての要素を取得する。
   * @return 全ての要素の {@code List} 。
   */
  def allElements: List[ElementClass]
  
  /**
   * 展示物オブジェクトを作成する。
   * このメソッドを呼び出しただけでは要素の永続化はなされていない。
   * 作成した要素を永続化するには {@link #save(A)} を行う。
   * @return このサービスによって管理する新しい {@code MuseumExhibit} オブジェクト。
   */
  def create(): ElementClass
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素がまだサービスに永続化されていない時は、永続化される。
   * 要素がこのサービスに存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def save(element: ElementClass)
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: ElementClass): Boolean
  
  /**
   * 指定した部屋の要素を取得する。
   */
  def getExhibits(room: UserExhibitRoom): List[ElementClass]
  
  /**
   * 部屋の要素を追加する。
   */
  def addElement(room: UserExhibitRoom, item: ElementClass)
}
