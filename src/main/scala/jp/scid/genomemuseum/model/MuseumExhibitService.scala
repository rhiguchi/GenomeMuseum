package jp.scid.genomemuseum.model

/**
 * MuseumExhibit データ提供サービスのインターフェイス。
 */
trait MuseumExhibitService extends ListDataService[MuseumExhibit] {
  type ElementClass <: MuseumExhibit
  /**
   * 展示物オブジェクトを作成する。
   * このメソッドを呼び出しただけでは要素の永続化はなされていない。
   * 作成した要素を永続化するには {@link #save(A)} を行う。
   * @return このサービスによって管理する新しい {@code MuseumExhibit} オブジェクト。
   */
  def create(): ElementClass
}

/**
 * MuseumExhibit データ提供サービスのインターフェイス。
 */
trait RoomExhibitService extends MuseumExhibitService {
  /**
   * 部屋に項目を追加する
   */
  def add(element: MuseumExhibit)
}
