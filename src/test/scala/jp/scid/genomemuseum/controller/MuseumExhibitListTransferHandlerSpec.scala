package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import java.awt.datatransfer.{Clipboard, DataFlavor, Transferable}
import java.io.File

import javax.swing.TransferHandler

import jp.scid.genomemuseum.gui.ExhibitTableModel
import jp.scid.genomemuseum.model.MuseumExhibit
import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
import DataFlavor.javaFileListFlavor

class MuseumExhibitListTransferHandlerSpec extends Specification with Mockito {
  import MuseumExhibitListController.TableSource._
  
  def is = "MuseumExhibitListTransferHandler" ^
    "getSourceActions" ^
      "LocalSource モード の時に転送可能" ! getSourceActions.s1 ^
      "WebSource モード の時は転送不可" ! getSourceActions.s2 ^
    bt ^ "exportToClipboard" ^
      "選択されている項目が転送される" ! exportToClipboard.s1 ^
      "無選択状態の時は転送されない" ! exportToClipboard.s2 ^
    bt ^ "canImport" ^
      "ファイルの受け入れ可能" ! canImport.s1 ^
      "MuseumExhibitTransferData の受け入れ可能" ! canImport.s2 ^
    bt ^ "importData" ^
      "ファイル読み込み受付" ! importData.s1 ^
      "親コントローラの loadBioFiles コール" ! importData.s2 ^
      "空フォルダでは読み込まない" ! importData.s3 ^
      "MuseumExhibitTransferData の時はファイル読み込みを行わない" ! importData.s4
  
  private[MuseumExhibitListTransferHandlerSpec] class TestBase {
    val e1 = MuseumExhibit("element1")
    val e2 = MuseumExhibit("element2")
    
    val localTableModel = mock[ExhibitTableModel]
    
    val ctrl = mock[MuseumExhibitListController]
    ctrl.tableSource returns LocalSource
    ctrl.localSourceTableModel returns localTableModel
    
    val handler = new MuseumExhibitListTransferHandler(ctrl)
  }
  
  def getSourceActions = new TestBase {
    import TransferHandler.{NONE, COPY}
    
    def onLocalSource = {
      handler.getSourceActions(null)
    }
    
    def onWebSource = {
      ctrl.tableSource returns WebSource
      handler.getSourceActions(null)
    }
    
    def s1 = onLocalSource must_!= NONE
    
    def s2 = onWebSource must_== NONE
  }
  
  def exportToClipboard = new TestBase {
    import TransferHandler.COPY
    
    private def selectAndTransferToClipboard(data: List[MuseumExhibit]) = {
      localTableModel.selections returns data
      val testClipboard = new Clipboard("testClipboard")
      handler.exportToClipboard(null, testClipboard, COPY)
      testClipboard
    }
    
    def transferedObj = {
      selectAndTransferToClipboard(List(e1, e2))
    }
    
    def noSelectionTransfering = {
      selectAndTransferToClipboard(Nil).getContents(null)
    }
    
    def s1_1 = transferedObj.isDataFlavorAvailable(exhibitDataFlavor) must beTrue
    def s1_2 = transferedObj.getData(exhibitDataFlavor)
      .asInstanceOf[MuseumExhibitTransferData].exhibits must contain(e1, e2).only.inOrder
    def s1 = s1_1 and s1_2
    
    def s2 = noSelectionTransfering must beNull
  }
  
  def canImport = new TestBase {
    def s1 = handler.canImport(null, Array(javaFileListFlavor)) must beTrue
    def s2 = handler.canImport(null, Array(exhibitDataFlavor)) must beTrue
  }
  
  def importData = new TestBase {
    val file1 = File.createTempFile("test_file", ".txt")
    
    private def fileTransferObjectOf(files: File*) = {
      val fileList = new java.util.ArrayList[File]
      files foreach fileList.add
      
      val fileTransferObject = mock[Transferable]
      fileTransferObject.getTransferData(javaFileListFlavor) returns fileList
      fileTransferObject.isDataFlavorSupported(javaFileListFlavor) returns true
      fileTransferObject.getTransferDataFlavors returns Array(javaFileListFlavor)
      fileTransferObject
    }
    
    def fileTransfered = {
      ctrl.loadBioFiles(any) returns true
      handler.importData(null, fileTransferObjectOf(file1))
    }
    
    def ctrlCall = {
      handler.importData(null, fileTransferObjectOf(file1))
      ctrl
    }
    
    def emptyFolder = {
      val emptyDir = new File(file1.getParent, file1.getName + ".f")
      emptyDir.mkdir
      handler.importData(null, fileTransferObjectOf(emptyDir))
    }
    
    def meData = {
      ctrl.loadBioFiles(any) returns true
      val data = MuseumExhibitTransferData(List(e1, e2))
      val tObj = fileTransferObjectOf(file1)
      tObj.getTransferData(exhibitDataFlavor) returns data
      tObj.isDataFlavorSupported(exhibitDataFlavor) returns true
      handler.importData(null, tObj)
      
      ctrl
    }
    
    def s1 = fileTransfered must beTrue
    
    def s2 = there was one(ctrlCall).loadBioFiles(List(file1))
    
    def s3 = emptyFolder must beFalse
    
    def s4 = there was no(meData).loadBioFiles(List(file1))
  }
}
