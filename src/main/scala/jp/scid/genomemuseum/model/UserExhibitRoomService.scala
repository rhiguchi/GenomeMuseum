package jp.scid.genomemuseum.model

import UserExhibitRoom.RoomType._

/**
 * UserExhibitRoom データ提供サービスのインターフェイス。
 */
trait UserExhibitRoomService extends TreeDataService[UserExhibitRoom] {
  /**
   * 部屋をサービスに追加する。
   * @param roomType 部屋の種類
   * @param name 表示名
   * @param parent 親要素
   * @see UserExhibitRoom
   */
  def addRoom(roomType: RoomType, name: String,
    parent: Option[UserExhibitRoom]): UserExhibitRoom
  
  /**
   * この名前をもつ部屋が存在するか。
   * @param name
   * @return 存在する時は {@code true} 。
   */
  def nameExists(name: String): Boolean
  
  /**
   * 要素と、その子孫全てを削除する
   * @param element 削除する要素。
   * @return 削除された要素数。
   */
  def remove(element: UserExhibitRoom): Int
  
  /**
   * 親要素を変更する
   * @param element 要素
   * @param parent 新しい親の要素。ルート項目にするには None。
   */
  def setParent(element: UserExhibitRoom, parent: Option[UserExhibitRoom])
}
