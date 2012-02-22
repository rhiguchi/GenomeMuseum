package jp.scid.genomemuseum.model.squeryl

import org.squeryl.KeyedEntity

/**
 * 部屋のコンテンツを表す
 */
private[squeryl] case class RoomExhibit(
  roomId: Long,
  var exhibitId: Long
) extends KeyedEntity[Long] {
  def this() = this(0, 0)
  var id: Long = 0
}

private[squeryl] object RoomExhibit {
  def apply(room: UserExhibitRoom, exhibit: MuseumExhibit): RoomExhibit =
    apply(room.id, exhibit.id)
}
