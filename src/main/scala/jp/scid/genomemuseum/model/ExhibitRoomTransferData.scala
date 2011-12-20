package jp.scid.genomemuseum.model

import java.awt.datatransfer.{Transferable, DataFlavor}

object ExhibitRoomTransferData {
  val dataFlavor = new DataFlavor(ExhibitRoomTransferData.getClass,
    "ExhibitRoomTransferData")
  
  def apply(room: UserExhibitRoom, exhibitService: MuseumExhibitService): ExhibitRoomTransferData =
    new ExhibitRoomTransferDataImpl(room, exhibitService)
}

trait ExhibitRoomTransferData extends Transferable {
  /** 移動もとの部屋 */
  def userExhibitRoom: UserExhibitRoom
}

private class ExhibitRoomTransferDataImpl(room: UserExhibitRoom, exhibitService: MuseumExhibitService)
    extends ExhibitRoomTransferData {
  import ExhibitRoomTransferData.{dataFlavor => roomDataFlavor}
  
  def userExhibitRoom = room
  
  def getTransferDataFlavors(): Array[DataFlavor] =
    Array(roomDataFlavor)
  
  def getTransferData(flavor: DataFlavor) = flavor match {
    case `roomDataFlavor` => this
    case _ => null
  }
  
  def isDataFlavorSupported(flavor: DataFlavor) =
    getTransferDataFlavors().contains(flavor)
}