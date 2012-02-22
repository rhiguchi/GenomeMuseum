package jp.scid.genomemuseum.model

/**
 * MuseumExhibit のリストとその所属元を保持するデータ構造定義
 */
trait RoomContentExhibits {
  /** 転送する展示物 */
  def exhibitList: Seq[MuseumExhibit]
  /** 展示物のもとの部屋 */
  def userExhibitRoom: Option[UserExhibitRoom]
}
