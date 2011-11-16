package jp.scid.genomemuseum.model.squeryl

import org.squeryl.KeyedEntity

import jp.scid.genomemuseum.model.{MuseumExhibit => IMuseumExhibit}
import IMuseumExhibit.FileType

/**
 * 部屋のコンテンツを表す
 */
private[squeryl] case class RoomExhibit(
  roomId: Long,
  exhibitId: Long
) extends KeyedEntity[Long] {
  def this() = this(0, 0)
  var id: Long = 0
}
