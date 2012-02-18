package jp.scid.genomemuseum.model

import java.util.Date

/**
 * アプリケーションの基本データ構造
 */
trait MuseumSchema {
  /** 『部屋』のデータサービス */
  def userExhibitRoomService: UserExhibitRoomService
  
  /** 『展示物』データのテーブルデータサービス */
  def museumExhibitService: MuseumExhibitListModel with MuseumExhibitService
}

object MuseumSchema {
  /**
   * ファイルを格納先に持ったデータスキーマを作成する。
   */
  def on(place: String): MuseumSchema = {
    squeryl.MuseumSchema.on(place)
  }
}