package jp.scid.genomemuseum.model.squeryl

import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

import jp.scid.genomemuseum.model.{UserExhibitRoom => IUserExhibitRoom,
  MuseumExhibitListModel => IMuseumExhibitListModel, UserExhibitRoomService => IUserExhibitRoomService}
import IUserExhibitRoom.RoomType

/**
 * ユーザーが中身を設定できる展示室オブジェクトの、Squeryl 実装
 * @param name 表示名。
 * @param boxType この部屋のタイプ
 * @param parentId 親子構造を示すときの、親の識別子。
 */
case class UserExhibitRoom(
  @Column("name")
  var name: String = "",
  @Column("type")
  val roomType: RoomType.Value = RoomType.BasicRoom,
  @Column("parent_id")
  var parentId: Option[Long] = None
) extends IUserExhibitRoom with KeyedEntity[Long] {
  /** 
   * この部屋を一意に決める識別子。
   * 1 以上の値の時、同じ id を持つオブジェクトは同じ部屋であることを示す。
   * 0 の時は、どの部屋とも一致しない。
   */
  var id: Long = 0
  
  /** Squeryl のための値付きの標準コンストラクタ */
  def this() = this("", parentId = Some(0))
}
