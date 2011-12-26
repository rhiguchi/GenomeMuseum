package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler}
import java.awt.datatransfer.{Transferable, DataFlavor}
import java.io.File

import org.specs2._
import mock._

import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor

import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibitService,
  MuseumStructure, MuseumExhibit, MuseumExhibitTransferData}
import jp.scid.genomemuseum.gui.{MuseumSourceModel}
import UserExhibitRoom.RoomType._

class ExhibitRoomListTransferHandlerSpec extends Specification with Mockito {
  import MuseumExhibitTransferData.{dataFlavor => exhibitListDataFlavor}
  
  def is = "ExhibitRoomListTransferHandler" ^
    end
}
