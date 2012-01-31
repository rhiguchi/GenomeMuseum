package jp.scid.genomemuseum.controller

import javax.swing.{JTree, TransferHandler}
import java.awt.datatransfer.{Transferable, DataFlavor}
import java.io.File

import org.specs2._

import TransferHandler.TransferSupport
import DataFlavor.javaFileListFlavor

import jp.scid.genomemuseum.model.{ExhibitRoom, UserExhibitRoom, MuseumExhibitService,
  MuseumStructure, MuseumExhibit, MuseumExhibitTransferData}
import jp.scid.genomemuseum.gui.{MuseumSourceModel}
import UserExhibitRoom.RoomType._

class ExhibitRoomListTransferHandlerSpec extends Specification with mock.Mockito {
  import MuseumExhibitTransferData.{dataFlavor => exhibitListDataFlavor}
  
  def is = "ExhibitRoomListTransferHandler" ^
    "sourceListModel プロパティ" ^ sourceListModelSpec(createHandler) ^
    "loadManager プロパティ" ^ loadManagerSpec(createHandler) ^
    end
  
  def createHandler() = new ExhibitRoomListTransferHandler()
  
  def sourceListModelSpec(h: => ExhibitRoomListTransferHandler) =
    "初期状態は None" ! sourceListModel(h).init ^
    "設定と取得" ! sourceListModel(h).setAndGet ^
    bt
  
  def loadManagerSpec(h: => ExhibitRoomListTransferHandler) =
    "初期状態は None" ! loadManager(h).init ^
    "設定と取得" ! loadManager(h).setAndGet ^
    bt
  
  // sourceListModel プロパティ
  def sourceListModel(ctrl: ExhibitRoomListTransferHandler) = new {
    def init = ctrl.sourceListModel must beNone
    def setAndGet = {
      val model = mock[MuseumSourceModel]
      ctrl.sourceListModel = Some(model)
      ctrl.sourceListModel must beSome(model)
    }
  }
  
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
