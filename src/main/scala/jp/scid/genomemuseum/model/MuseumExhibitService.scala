package jp.scid.genomemuseum.model

import java.io.File

/**
 * MuseumExhibit データ提供サービスのインターフェイス。
 */
trait MuseumExhibitService extends ListDataService[MuseumExhibit] {
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
