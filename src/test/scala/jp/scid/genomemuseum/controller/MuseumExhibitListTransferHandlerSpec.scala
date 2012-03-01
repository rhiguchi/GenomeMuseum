package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Clipboard, DataFlavor, Transferable}
import java.io.File

import javax.swing.{TransferHandler, JComponent}

import org.specs2._
import mock._

import jp.scid.genomemuseum.model.{MuseumExhibit, UserExhibitRoom,
  UserExhibitRoomMock, MuseumExhibitMock, ExhibitRoomModel, ExhibitMuseumSpace,
  FreeExhibitRoomModel}
import UserExhibitRoom.RoomType._
import DataFlavor.javaFileListFlavor
import TransferHandler.TransferSupport
import MuseumExhibitListTransferHandler._
import ExhibitRoomListTransferHandler.{TransferData => TreeTransferData}

object MuseumExhibitListTransferHandlerSpec {
  object TransferDataMock extends Mockito {
    def of(tableModel: ExhibitRoomModel, exhibitList: List[MuseumExhibit] = Nil) = {
      val data = mock[TransferData]
      data.tableModel returns tableModel
      data.exhibitList returns exhibitList
      data
    }
  }
}

class MuseumExhibitListTransferHandlerSpec extends Specification with Mockito {
  import MuseumExhibitListTransferHandlerSpec._
  
  def is = "MuseumExhibitListTransferHandler" ^
    "転入可能性" ^ canImportSpec(createHandler) ^
//    "転入操作" ^ importDataSpec(createHandler) ^
    end
  
  def createHandler() = new MuseumExhibitListTransferHandler
  
  def canImportSpec(h: => MuseumExhibitListTransferHandler) =
    "TransferData を FreeExhibitRoomModel に転入できる" ! canimport(h).dataToFree ^
    "TransferData を同じ部屋には転入できない" ! canimport(h).dataNotToSame ^
    "TreeTransferData を FreeExhibitRoomModel に転入できる" ! canimport(h).treeToFree ^
    "TreeTransferData を同じ部屋には転入できない" ! canimport(h).treeNotToSame ^
    "ファイルリスト を FreeExhibitRoomModel に転入できる" ! canimport(h).fileToFree ^
    bt
  
  def createTransferSupport(flavor: DataFlavor, data: AnyRef) = {
    val transferable = mockTransferable(flavor, data)
    new TransferSupport(mock[JComponent], transferable)
  }
  
  def mockTransferable(flavor: DataFlavor, data: AnyRef) = {
    val transferable = mock[Transferable]
    transferable.isDataFlavorSupported(flavor) returns true
    transferable.getTransferData(flavor) returns data
    transferable
  }
  
  def createTransferSupport(tableModel: ExhibitRoomModel): TransferSupport =
    createTransferSupport(TransferData.dataFlavor, TransferData(tableModel))
  
  def createTreeTransferSupport(lastNode: ExhibitRoomModel): TransferSupport =
    createTransferSupport(TreeTransferData.dataFlavor,
      TreeTransferData(null, List(IndexedSeq(lastNode))))
  
  def createFileTransferSupport(files: File*): TransferSupport = {
    import scala.collection.JavaConverters._
    
    val fileList = files.asJava
    createTransferSupport(DataFlavor.javaFileListFlavor, fileList)
  }
  
  private[controller] trait ImportableRoom extends ExhibitMuseumSpace with FreeExhibitRoomModel
  
  def canimport(h: MuseumExhibitListTransferHandler) = new {
    val roomModel, ctrlRoomModel = mock[ImportableRoom]
    
    val handler = spy(h)
    doAnswer{_ => ctrlRoomModel}.when(handler).controllerModel
    
    def dataToFree =
      handler.canImport(createTransferSupport(roomModel)) must beTrue
    
    def dataNotToSame =
      handler.canImport(createTransferSupport(ctrlRoomModel)) must beFalse
    
    def treeToFree =
      handler.canImport(createTreeTransferSupport(roomModel)) must beTrue
    
    def treeNotToSame =
      handler.canImport(createTreeTransferSupport(ctrlRoomModel)) must beFalse
    
    def fileToFree =
      handler.canImport(createFileTransferSupport(
        File.createTempFile("transfer test", null))) must beTrue
  }
}
