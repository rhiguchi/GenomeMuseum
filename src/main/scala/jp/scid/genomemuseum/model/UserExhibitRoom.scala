package jp.scid.genomemuseum.model

object UserExhibitRoom {
  /**
   * 部屋の種類を示す序列。
   */
  object RoomType extends Enumeration {
    type RoomType = Value
    /** ユーザーが中身を設定できる『部屋』を示す */
    val BasicRoom = Value(1)
    /** 『部屋』をまとめる『部屋』 */
    val GroupRoom = Value(2)
    /** 条件を設定することで自動で中身が定まる『部屋』 */
    val SmartRoom = Value(3)
    
    def unapply(room: UserExhibitRoom): Option[Value] = {
      Some(room.roomType)
    }
  }
  
  def unapply(room: ExhibitRoom): Option[(UserExhibitRoom)] = room match {
    case room: UserExhibitRoom => Some(room)
    case _ => None
  }
}

import UserExhibitRoom.RoomType._

/**
 * ユーザーが中身である MuseumExhibits を設定できる『部屋』インターフェイス。
 * 『部屋』の種類は 3 つある。
 */
trait UserExhibitRoom extends ExhibitRoom {
  /** 部屋の識別子 */
  def id: Long
  
  /** 部屋の種類 */
  def roomType: UserExhibitRoom.RoomType.Value
  
  /** この部屋の表示名を設定する */
  def name_=(newName: String)
}
