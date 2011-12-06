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
  def onMemory = {
    squeryl.MuseumSchema.onMemory("GenomeMuseum")
  }
  
  def onFile(file: java.io.File) = {
    squeryl.MuseumSchema.onFile(file)
  }
}