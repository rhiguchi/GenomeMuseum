package jp.scid.genomemuseum.model

import java.util.Date

/**
 * アプリケーションの基本データ構造
 */
trait MuseumSchema {
  /** 『部屋』のデータサービス */
  def userExhibitRoomService: UserExhibitRoomService
  
  /** 『展示物』データのテーブルデータサービス */
  def museumExhibitService: MuseumExhibitService
  
  /** 自由展示棟 */
  def freeExhibitPavilion: FreeExhibitPavilion
  
  /** 部屋のコンテンツを取得 */
  @deprecated("2012/02/26", "use via museumExhibitService")
  def getExhibitRoomModel(room: UserExhibitRoom): ExhibitRoomModel
}

object MuseumSchema {
  /**
   * ファイルを格納先に持ったデータスキーマを作成する。
   */
  def on(place: String): MuseumSchema = {
    squeryl.MuseumSchema.on(place)
  }
}