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
    "loadManager プロパティ" ^ loadManagerSpec(createHandler) ^
    end
  
  def createHandler() = new ExhibitRoomListTransferHandler()
  
  def loadManagerSpec(h: => ExhibitRoomListTransferHandler) =
    "初期状態は None" ! loadManager(h).init ^
    "設定と取得" ! loadManager(h).setAndGet ^
    bt
  
  // exhibitLoadManager プロパティ
  def loadManager(ctrl: ExhibitRoomListTransferHandler) = new {
    def init = ctrl.exhibitLoadManager must beNone
    def setAndGet = {
      val model = mock[MuseumExhibitLoadManager]
      ctrl.exhibitLoadManager = Some(model)
      ctrl.exhibitLoadManager must beSome(model)
    }
  }
}
