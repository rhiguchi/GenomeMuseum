package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler}
import java.awt.datatransfer.{Transferable, DataFlavor}
import java.io.File

import org.specs2._

import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor

import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibitService,
  MuseumStructure, MuseumExhibit}
import UserExhibitRoom.RoomType._

class ExhibitRoomListTransferHandlerSpec extends Specification with mock.Mockito {
  
  def is = "ExhibitRoomListTransferHandler" ^
    end
  
  def createHandler() = new ExhibitRoomListTransferHandler()
  
}
