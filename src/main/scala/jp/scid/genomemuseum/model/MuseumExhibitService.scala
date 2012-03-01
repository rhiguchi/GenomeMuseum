package jp.scid.genomemuseum.model

import collection.mutable.Publisher
import collection.script.Message

/**
 * MuseumExhibit データ提供サービスのインターフェイス。
 */
trait MuseumExhibitService extends ExhibitRoomModel {
  type ElementClass <: MuseumExhibit
  
  /**
   * 展示物オブジェクトを作成する。
   * 永続化された MuseumExhibit エンティティを作成する。
   * @return このサービスによって管理する新しい {@code MuseumExhibit} オブジェクト。
   */
  def create(): MuseumExhibit
  
  /**
   * 要素の更新をサービスに通知する。
   * 要素がまだサービスに永続化されていない時は、永続化される。
   * 要素がこのサービスに存在しない時は無視される。
   * @param element 保存を行う要素。
   */
  def save(element: MuseumExhibit)
  
  /**
   * このデータサービスが持つ要素を除去する。
   * 要素がこのサービスに存在しない時は無視される。
   * @return 削除に成功した場合は {@code true} 。
   *         項目が存在しなかったなどでサービス内に変更が発生しなかった時は {@code false} 。
   */
  def remove(element: MuseumExhibit): Boolean
}
