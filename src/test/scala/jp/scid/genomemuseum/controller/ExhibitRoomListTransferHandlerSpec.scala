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
  private type Factory = MuseumExhibitLoadManager => ExhibitRoomListTransferHandler
  
  import MuseumExhibitTransferData.{dataFlavor => exhibitListDataFlavor}
  
  def is = "ExhibitRoomListTransferHandler" ^
    "sourceListModel プロパティ" ^ sourceListModelSpec(createHandler) ^
    end
  
  def createHandler(loadManager: MuseumExhibitLoadManager) = {
    new ExhibitRoomListTransferHandler(loadManager)
  }
  
  implicit def consturctByMock(f: Factory): ExhibitRoomListTransferHandler = {
    createHandler(mock[MuseumExhibitLoadManager])
  }
  
  def sourceListModelSpec(c: Factory) =
    "初期状態は None" ! sourceListModel(c).init ^
    "設定と取得" ! sourceListModel(c).setAndGet ^
    bt
  
  // loadManager プロパティ
  def sourceListModel(ctrl: ExhibitRoomListTransferHandler) = new {
    def init = ctrl.sourceListModel must beNone
    def setAndGet = {
      val model = mock[MuseumSourceModel]
      ctrl.sourceListModel = Some(model)
      ctrl.sourceListModel must beSome(model)
    }
  }
}
