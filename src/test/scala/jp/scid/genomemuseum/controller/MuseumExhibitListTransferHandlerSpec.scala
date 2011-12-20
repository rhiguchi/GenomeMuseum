package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Clipboard, DataFlavor, Transferable}
import java.io.File

import javax.swing.{TransferHandler, JComponent}

import org.specs2._
import mock._

import jp.scid.genomemuseum.gui.ExhibitTableModel
import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitTransferData, UserExhibitRoom,
  UserExhibitRoomMock, MuseumExhibitMock}
import UserExhibitRoom.RoomType._
import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
import DataFlavor.javaFileListFlavor
import TransferHandler.TransferSupport

class MuseumExhibitListTransferHandlerSpec extends Specification with Mockito {
  def is = "MuseumExhibitListTransferHandler" ^
    "転入可能性の確認" ^ canImportSpec(createHandler) ^
    "転入操作" ^ importDataSpec(createHandler) ^
    end
  
  def createHandler = new MuseumExhibitListTransferHandler() {
    def importFiles(files: Seq[File], targetRoom: Option[UserExhibitRoom]) = true
    def importExhibits(exhibits: Seq[MuseumExhibit], targetRoom: UserExhibitRoom) = true
    protected[controller] def getTargetRooom(ts: TransferSupport) = None
  }
  
  def canImportSpec(h: => MuseumExhibitListTransferHandler) =
    "MuseumExhibitTransferData を受け入れ可能" ! canimport(h).exhibit ^
    "File を受け入れ可能" ! canimport(h).file ^
    "標準で受け入れ不可" ! canimport(h).empty ^
    bt
  
  def importDataSpec(h: => MuseumExhibitListTransferHandler) =
    "MuseumExhibitTransferData を転入" ! importData(h).exhibit ^
    "File を転入" ! importData(h).file ^
    "他は転入しない" ! importData(h).empty ^
    bt
  
  class TestBase {
    val t = mock[Transferable]
    val ts = new TransferSupport(mock[JComponent], t)
    
    val Seq(basicRoom, smartRoom, groupRoom) = List(BasicRoom, SmartRoom, GroupRoom)
      .map(r => Some(UserExhibitRoomMock.of(r)))
    
    // ファイルフレーバーを付与
    protected def makeTransferDataFileFlavor(files: Seq[File]) {
      import scala.collection.JavaConverters._
      
      t.isDataFlavorSupported(javaFileListFlavor) returns true
      t.getTransferData(javaFileListFlavor) returns files.toBuffer.asJava
    }
    
    // 展示物フレーバーを付与
    protected def makeTransferDataExhibitFlavor(museumExhibits: Seq[MuseumExhibit], sourceRoom: Option[UserExhibitRoom]) {
      import scala.collection.JavaConverters._
      
      t.isDataFlavorSupported(exhibitDataFlavor) returns true
      
      val data = mock[MuseumExhibitTransferData]
      data.museumExhibits returns museumExhibits.toList
      data.sourceRoom returns sourceRoom
      t.getTransferData(exhibitDataFlavor) returns data
    }
  }
  
  def canimport(h: MuseumExhibitListTransferHandler) = new TestBase {
    val handler = spy(h)
    
    def exhibit = {
      val exhibits = (0 to 4).map(i => MuseumExhibitMock.of("exhibit" + i))
      val sourceRoom = UserExhibitRoomMock.of(BasicRoom)
      makeTransferDataExhibitFlavor(exhibits, Some(sourceRoom))
      handler.getTargetRooom(ts) returns basicRoom
      
      handler.canImport(ts) must beTrue
    }
    
    def file = {
      t.isDataFlavorSupported(javaFileListFlavor) returns true
      h.canImport(ts) must beTrue
    }
    
    def empty = h.canImport(ts) must beFalse
  }
  
  def importData(h: MuseumExhibitListTransferHandler) = new TestBase {
    val handler = spy(h)
    
    // 転入先を指定して転入操作
    private def importDataTo(room: Option[UserExhibitRoom]) {
      handler.getTargetRooom(ts) returns room
      handler.importData(ts)
    }
    
    def exhibit = {
      val exhibits = (0 to 4).map(i => MuseumExhibitMock.of("exhibit" + i))
      val sourceRoom = UserExhibitRoomMock.of(BasicRoom)
      makeTransferDataFileFlavor(Nil)
      
      makeTransferDataExhibitFlavor(exhibits, Some(sourceRoom))
      Seq(basicRoom, smartRoom, groupRoom, None) foreach importDataTo
      
      makeTransferDataExhibitFlavor(exhibits, None)
      Seq(basicRoom, smartRoom, groupRoom, None) foreach importDataTo
      
      makeTransferDataExhibitFlavor(exhibits, basicRoom)
      Seq(basicRoom, smartRoom, groupRoom, None) foreach importDataTo
      
      there was two(handler).importExhibits(exhibits, basicRoom.get) then
        two(handler).importExhibits(any, any)
    }
    
    def file = {
      val files = (0 to 4).map(i => new File("file" + i))
      makeTransferDataFileFlavor(files)
      
      Seq(basicRoom, smartRoom, groupRoom, None) foreach importDataTo
      
      there was one(handler).importFiles(files, basicRoom) then
        one(handler).importFiles(files, None) then
        two(handler).importFiles(any, any)
    }
    def empty = todo
  }
}
