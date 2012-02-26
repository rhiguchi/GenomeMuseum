package jp.scid.genomemuseum.model

import scala.collection.mutable.Publisher
import scala.collection.script.Message

import ca.odell.glazedlists.EventList

import UserExhibitRoom.RoomType._

/**
 * UserExhibitRoom データ提供サービスのインターフェイス。
 */
trait UserExhibitRoomService extends PropertyChangeObservable {

  /**
   * 展示物を作成する
   */
  def create(roomType: RoomType, baseName: String, parent: Option[UserExhibitRoom]): UserExhibitRoom
  /**
   * 部屋をサービスに追加する。
   * @param roomType 部屋の種類
   * @param name 表示名
   * @param parent 親要素
   * @see UserExhibitRoom
   */
  def addRoom(roomType: RoomType, name: String, parent: Option[UserExhibitRoom]): UserExhibitRoom
  
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
  def remove(element: UserExhibitRoom): Boolean
  
  /**
   * 指定した親をもつ部屋のリストを返す
   * @param parent 親要素。{@code None} で、ルート要素（どの親にも属さない要素）を返す。
   * @return 子要素。
   */
  def getFloorRoomList(parent: Option[UserExhibitRoom]): EventList[UserExhibitRoom]
  
  /**
   * 子要素を返す。
   * @param parent 親要素。{@code None} で、ルート要素（どの親にも属さない要素）を返す。
   * @return 子要素。
   */
  def getChildren(parent: Option[UserExhibitRoom]): Iterable[UserExhibitRoom]
  
  /**
   * 親要素を取得する
   * @param element 子要素
   * @return 親要素。属する親が無いの時は {@code None} 。
   */
  def getParent(element: UserExhibitRoom): Option[UserExhibitRoom]
  
  /**
   * 要素の更新を通知する。
   * @param element 削除する要素。
   * @return 削除された要素数。
   */
  def save(element: UserExhibitRoom)
  
  /**
   * 親要素を変更する
   * @param element 要素
   * @param parent 新しい親の要素。ルート項目にするには None。
   */
  def setParent(element: UserExhibitRoom, parent: Option[UserExhibitRoom])
}
