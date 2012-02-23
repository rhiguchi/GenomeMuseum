package jp.scid.genomemuseum.model

import collection.mutable.Publisher
import collection.script.Message

/**
 * MuseumExhibit データ提供サービスのインターフェイス。
 */
trait MuseumExhibitService extends MuseumExhibitListModel {
  type ElementClass <: MuseumExhibit
  
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
  def save(element: MuseumExhibit)
  
  /** {@inheritDoc} */
  def userExhibitRoom = None
  
  def exhibitList: Seq[ElementClass]
}
