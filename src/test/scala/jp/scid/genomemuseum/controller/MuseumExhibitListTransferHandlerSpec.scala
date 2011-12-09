package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import java.awt.datatransfer.{Clipboard, DataFlavor, Transferable}
import java.io.File

import javax.swing.TransferHandler

import jp.scid.genomemuseum.gui.ExhibitTableModel
import jp.scid.genomemuseum.model.{MuseumExhibit, MuseumExhibitTransferData}
import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
import DataFlavor.javaFileListFlavor

object MuseumExhibitListTransferHandlerSpec extends Mockito {
  def fileTransferObject(files: File*) = {
    val fileList = new java.util.ArrayList[File]()
    files foreach fileList.add
    
    val t = mock[Transferable]
    t.getTransferData(javaFileListFlavor) returns fileList
    t.getTransferDataFlavors returns Array(javaFileListFlavor)
    t.isDataFlavorSupported(javaFileListFlavor) returns true
    
    t
  }
}

class MuseumExhibitListTransferHandlerSpec extends Specification with Mockito {
  import MuseumExhibitListTransferHandlerSpec._
  
  def is = "MuseumExhibitListTransferHandler" ^
    "項目が選択されていない状態" ^ cannotTransfer(defaultHandler) ^ bt ^
    "項目が選択されている状態" ^ canTransfer(itemSelectedHandler) ^ bt ^
    "読み込みハンドラが設定されている時" ^ withLoadManager(handlerWithLoadManager) ^ bt ^
    "読み込みハンドラが設定されていない時" ^ withNoLoadManager(defaultHandler) ^ bt ^
    end
  
  def defaultHandler = {
    val tableModel = spy(new ExhibitTableModel)
    new MuseumExhibitListTransferHandler(tableModel)
  }
  
  def itemSelectedHandler = {
    val tableModel = spy(new ExhibitTableModel)
    tableModel.selections returns List(mock[MuseumExhibit])
    new MuseumExhibitListTransferHandler(tableModel)
  }
  
  def handlerWithLoadManager = {
    val loadManager = mock[MuseumExhibitLoadManager]
    val handler = defaultHandler
    handler.loadManager = Some(loadManager)
    handler
  }
  
  def cannotTransfer(handler: => MuseumExhibitListTransferHandler) =
    "アクション取得" ! exportingSpec(handler).sourceAction ^
    "転送オブジェクトは作成されない" ! exportingSpec(handler).notCreateTransferable
  
  def canTransfer(handler: => MuseumExhibitListTransferHandler) =
    "アクション取得" ! exportingSpec(handler).sourceAction ^
    "転送オブジェクトが作成される" ! exportingSpec(handler).createsTransferable ^
    "選択項目が転送される" ! exportingSpec(handler).exportsSelectedItem
  
  def withLoadManager(handler: => MuseumExhibitListTransferHandler) =
    "ファイルフレーバーの取り込みが可能" ! importingSpec(handler).acceptFileFlavor ^
    "ファイルが読み込まれる" ! importingSpec(handler).importsFile ^
    "ディレクトリは読み込まれない" ! importingSpec(handler).notImportsDir ^
    "読み込みハンドラの読み込み処理が呼ばれる" ! importingSpec(handler).callsLoadExhibits
  
  def withNoLoadManager(handler: => MuseumExhibitListTransferHandler) =
    "ファイルフレーバーの取り込みはできない" ! importingSpec(handler).notAcceptFileFlavor
  
  class TestBase {
  }
  
  def exportingSpec(handler: MuseumExhibitListTransferHandler) = new Object {
    def sourceAction =
      handler.getSourceActions(null) must_!= TransferHandler.NONE
    
    def createsTransferable =
      handler.createTransferable(null) must beAnInstanceOf[MuseumExhibitTransferData]
    
    def notCreateTransferable =
      handler.createTransferable(null) must beNull
    
    def exportsSelectedItem = {
      val testClipboard = new Clipboard("testClipboard")
      handler.exportToClipboard(null, testClipboard, TransferHandler.COPY)
      handler.createTransferable(null) must beAnInstanceOf[MuseumExhibitTransferData]
      todo
//      testClipboard.getData(exhibitDataFlavor).asInstanceOf[MuseumExhibitTransferData]
//        .exhibits must_== handler.tableModel.selections
    }
  }
  
  def importingSpec(handler: MuseumExhibitListTransferHandler) = new Object {
    import DataFlavor.javaFileListFlavor
    import java.io.File
    
    def acceptFileFlavor = {
      handler.canImport(null, Array(javaFileListFlavor)) must beTrue
    }
    
    def notAcceptFileFlavor = {
      handler.canImport(null, Array(javaFileListFlavor)) must beFalse
    }
    
    def importsFile = {
      val t = fileTransferObject(File.createTempFile("test", ".dat"))
      
      handler.importData(null, t) must beTrue
    }
    
    def notImportsDir = {
      val t = fileTransferObject(new File("xxxx"))
      
      handler.importData(null, t) must beFalse
    }
    
    def callsLoadExhibits = {
      val file1, file2, file3 = File.createTempFile("test", ".dat")
      val t = fileTransferObject(file1, file2, file3)
      handler.importData(null, t)
      
      there was one(handler.loadManager.get).loadExhibit(file1) then
        one(handler.loadManager.get).loadExhibit(file2) then
        one(handler.loadManager.get).loadExhibit(file3)
    }
  }
}
