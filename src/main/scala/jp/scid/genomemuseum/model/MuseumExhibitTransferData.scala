package jp.scid.genomemuseum.model

import java.awt.datatransfer.{Transferable, DataFlavor}

object MuseumExhibitTransferData {
  val dataFlavor = new DataFlavor(MuseumExhibitTransferData.getClass,
    "MuseumExhibitTransferData")
  
  def apply(exhibits: Seq[MuseumExhibit], sourceRoom: Option[UserExhibitRoom],
      storage: MuseumExhibitStorage): MuseumExhibitTransferData = {
    val t = new MuseumExhibitTransferDataImpl(exhibits, sourceRoom)
    t.fileStorage = Some(storage)
    t
  }
}

trait MuseumExhibitTransferData extends Transferable {
  /** 転送する展示物 */
  def museumExhibits: List[MuseumExhibit]
  /** 展示物のもとの部屋 */
  def sourceRoom: Option[UserExhibitRoom]
}