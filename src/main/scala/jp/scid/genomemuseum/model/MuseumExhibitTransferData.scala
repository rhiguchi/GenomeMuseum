package jp.scid.genomemuseum.model

import java.awt.datatransfer.{Transferable, DataFlavor}

object RoomContentExhibits {
  def apply(exhibits: List[MuseumExhibit], room: Option[UserExhibitRoom]): RoomContentExhibits =
    RoomContentExhibitsImpl(exhibits, room)
  
  /**
   * 単純実装
   */
  private case class RoomContentExhibitsImpl(
    exhibitList: List[MuseumExhibit],
    userExhibitRoom: Option[UserExhibitRoom]
  ) extends RoomContentExhibits
}

/**
 * MuseumExhibit のリストとその所属元を保持するデータ構造定義
 */
trait RoomContentExhibits {
  /** 転送する展示物 */
  def exhibitList: List[MuseumExhibit]
  /** 展示物のもとの部屋 */
  def userExhibitRoom: Option[UserExhibitRoom]
}
