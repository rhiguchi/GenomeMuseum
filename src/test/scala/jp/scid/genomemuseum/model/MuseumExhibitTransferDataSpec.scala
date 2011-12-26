package jp.scid.genomemuseum.model

import java.awt.datatransfer.{Transferable, DataFlavor}

object MuseumExhibitTransferDataMock extends org.specs2.mock.Mockito {
  def of(exhibits: Seq[MuseumExhibit] = Nil, room: Option[UserExhibitRoom] = None) = {
    val flavor = MuseumExhibitTransferData.dataFlavor
    val data = mock[MuseumExhibitTransferData]
    data.museumExhibits returns exhibits.toList
    data.sourceRoom returns room
    data.getTransferData(flavor) returns data
    data.isDataFlavorSupported(flavor) returns true
    data.getTransferDataFlavors returns Array(flavor)
    data
  }
}
