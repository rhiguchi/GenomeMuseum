package jp.scid.genomemuseum.model

import UserExhibitRoom.RoomType._

/**
 * 展示室の管理クラス構造。
 * 
 * 『自由展示棟の最上層』
 */
trait FreeExhibitPavilion extends ExhibitMuseumFloor {
  /**
   * 指定した階層に部屋を追加する
   */
  def addRoom(roomType: RoomType, name: String, parent: ExhibitMuseumFloor): ExhibitRoomModel
}
