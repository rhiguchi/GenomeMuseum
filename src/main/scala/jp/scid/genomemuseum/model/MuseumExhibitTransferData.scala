package jp.scid.genomemuseum.model

import java.awt.datatransfer.{Transferable, DataFlavor}

object MuseumExhibitTransferData {
  /** データフレーバー */
  val dataFlavor = new DataFlavor(MuseumExhibitTransferData.getClass,
    "MuseumExhibitTransferData")
}

/**
 * MuseumExhibit が転送される時の転送データインターフェイス。
 */
trait MuseumExhibitTransferData extends Transferable {
  /** 転送する展示物 */
  def museumExhibits: List[MuseumExhibit]
  /** 展示物のもとの部屋 */
  def sourceRoom: Option[UserExhibitRoom]
}
