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
  
  /** 『部屋』の『展示物』のテーブルデータサービス */
  def roomExhibitService(room: UserExhibitRoom): RoomExhibitService
}

object MuseumSchema {
  def fromMemory = {
    squeryl.MuseumSchema.makeMemoryConnection("GenomeMuseum")
  }
}