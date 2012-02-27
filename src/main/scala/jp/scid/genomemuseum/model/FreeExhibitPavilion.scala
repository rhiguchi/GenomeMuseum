package jp.scid.genomemuseum.model

import UserExhibitRoom.RoomType._

/**
 * 展示室の管理クラス構造。
 * 
 * 『自由展示棟の最上層』
 */
trait FreeExhibitPavilion extends MuseumFloor {
  /**
   * 指定した階層に部屋を追加する
   */
  def addRoom(roomType: RoomType, name: String,
    parent: Option[ExhibitMuseumFloor]): ExhibitMuseumSpace
  
  /**
   * 部屋を削除する
   */
  def removeRoom(room: ExhibitMuseumSpace)
  
  /**
   * 親を取得する
   */
  def getParent(room: ExhibitMuseumSpace): Option[ExhibitMuseumFloor]
}
