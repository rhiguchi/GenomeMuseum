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
}

object MuseumSchema {
  /**
   * メモリー上のプライベート空間を格納先に持ったデータスキーマを作成する。
   */
  def onMemory = {
    squeryl.MuseumSchema.onMemory("")
  }
  
  /**
   * ファイルを格納先に持ったデータスキーマを作成する。
   */
  def onFile(file: java.io.File) = {
    squeryl.MuseumSchema.onFile(file)
  }
}