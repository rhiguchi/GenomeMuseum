package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import javax.swing.{JTable, JTextField, TransferHandler}

import jp.scid.genomemuseum.model.{MuseumExhibitService, MuseumExhibit}
import MuseumExhibitListController.TableSource._

class MuseumExhibitListControllerSpec extends Specification with Mockito {
  def is = "MuseumExhibitListController" ^
    "初期状態" ^
      "dataTable#getModel が localSourceTableModel と同一" ! initial.s1 ^
      "localSourceSearchTextModel にテキストを設定すると quickSearchField に反映" ! initial.s2 ^
      "contentViewerVisibilityModel が true" ! initial.s3 ^
    bt ^ "LocalSource サービス" ^
      "要素の表示" ! ls.s1 ^
      "フィルタリング" ! ls.s2 ^
    bt ^ "WebSource モード" ^
      "dataTable#getModel が webSourceModel と同一" ! ws.s1 ^
      "webSourceSearchTextModel にテキストを設定すると quickSearchField に反映" ! ws.s2 ^
    bt ^ "removeSelectedExhibitAction" ^
      "localDataService#remove コール" ! removeSelectedExhibitAction.s1 ^
      "選択時は使用可能" ! removeSelectedExhibitAction.s2 ^
      "無選択時は使用不可" ! removeSelectedExhibitAction.s3 ^
      "WebSource モードでは選択していてもは使用不可" ! removeSelectedExhibitAction.s4 ^
    bt ^ "contentViewerVisibilityModel" ^
      "WebSource モードで false" ! contentViewerVisibilityModel.s1 ^
      "LocalSource モードで true" ! contentViewerVisibilityModel.s2 ^
    bt ^ "dataTable の 転送ハンドラ" ^
      "LocalSource モード の時に転送可能" ! dataTableTH.s1 ^
      "WebSource モード の時は転送不可能" ! dataTableTH.s2 ^
      "選択要素が転送される" ! dataTableTH.s3
  
  class TestBase {
    val dataTable = new JTable(0, 0)
    val quickSearchField = new JTextField("")
    
    val ctrl = new MuseumExhibitListController(dataTable, quickSearchField)
    
    def setModeWebSource() {
      ctrl.tableSource = LocalSource
      ctrl.tableSource = WebSource
    }
    
    def setModeLocalSource() {
      ctrl.tableSource = WebSource
      ctrl.tableSource = LocalSource
    }
    
    def contentViewerVisibilityModelValue = ctrl.contentViewerVisibilityModel()
  }
  
  val initial = new TestBase {
    ctrl.localSourceSearchTextModel := "test"
    
    def s1 = dataTable.getModel must_== ctrl.localSourceTableModel.tableModel
    
    def s2 = quickSearchField.getText must_== "test"
    
    def s3 = contentViewerVisibilityModelValue must beTrue
  }
  
  val ls = new TestBase {
    val e1 = MuseumExhibit("element1")
    val e2 = MuseumExhibit("element2")
    val e3 = MuseumExhibit("element3")
    val service = mock[MuseumExhibitService]
    service.allElements returns List(e1, e2, e3)
    
    ctrl.localDataService = service
    
    // EventList のスレッド実行待ち
    Thread.sleep(10)
    
    val serviceRowCount = dataTable.getRowCount
    
    // フィルタリング
    ctrl.localSourceSearchTextModel := "nt2"
    val filteredRowCount = dataTable.getRowCount
    ctrl.localSourceSearchTextModel := "xx"
    val filteredRowCount2 = dataTable.getRowCount
    
    def s1 = serviceRowCount must_== 3
    
    def s2_1 = filteredRowCount must_== 1
    def s2_2 = filteredRowCount2 must_== 0
    def s2 = s2_1 and s2_2
  }
  
  val ws = new TestBase {
    ctrl.tableSource = WebSource
    ctrl.webSourceSearchTextModel := "query"
    
    def s1 = dataTable.getModel must_== ctrl.webSourceTableModel.tableModel
    
    def s2 = quickSearchField.getText must_== "query"
  }
  
  class LocalServiceApplied extends TestBase {
    val e1 = MuseumExhibit("element1")
    val e2 = MuseumExhibit("element2")
    val e3 = MuseumExhibit("element3")
    val service = mock[MuseumExhibitService]
    service.allElements returns List(e1, e2, e3)
    
    ctrl.localDataService = service
  }
  
  def removeSelectedExhibitAction = new LocalServiceApplied {
    def selectE2() = ctrl.localSourceTableModel.selections = List(e2)
    def actionEnabled = ctrl.removeSelectedExhibitAction.enabled
    
    def performedService = {
      service.remove(any) returns true
      
      selectE2()
      ctrl.removeSelectedExhibitAction()
      
      service
    }
    
    def selected = {
      selectE2()
      actionEnabled
    }
    
    def notSelected = {
      actionEnabled
    }
    
    def deselectAfterSelect = {
      selectE2()
      ctrl.localSourceTableModel.selections = Nil
      actionEnabled
    }
    
    def webSourceModeEnability = {
      selectE2()
      setModeWebSource()
      actionEnabled
    }
    
    def s1 = there was one(performedService).remove(e2)
    
    def s2 = selected must beTrue
    
    def s3_1 = notSelected must beFalse
    def s3_2 = deselectAfterSelect must beFalse
    def s3 = s3_1 and s3_2
    
    def s4 = webSourceModeEnability must beFalse
  }
  
  def contentViewerVisibilityModel = new LocalServiceApplied {
    def webSourceModeValue = {
      setModeWebSource()
      contentViewerVisibilityModelValue
    }
    
    def localSourceModeValue = {
      setModeLocalSource()
      contentViewerVisibilityModelValue
    }
    
    def s1 = webSourceModeValue must beFalse
    
    def s2 = localSourceModeValue must beTrue
  }
  
  def dataTableTH = new LocalServiceApplied {
    import TransferHandler.{NONE, COPY}
    import java.awt.datatransfer.Clipboard
    import MuseumExhibitTransferData.{dataFlavor => exhibitDataFlavor}
    
    def sourceActionForLocalSource = {
      setModeLocalSource()
      dataTable.getTransferHandler.getSourceActions(dataTable)
    }
    
    def sourceActionForWebSource = {
      setModeWebSource()
      dataTable.getTransferHandler.getSourceActions(dataTable)
    }
    
    def transferToClipboard = {
      ctrl.localSourceTableModel.selections = List(e2, e3)
      val testClipboard = new Clipboard("testClipboard")
      dataTable.getTransferHandler.exportToClipboard(dataTable, testClipboard, COPY)
      testClipboard
    }
    
    def s1 = sourceActionForLocalSource must_!= NONE
    
    def s2 = sourceActionForWebSource must_== NONE
    
    def s3_1 = transferToClipboard.isDataFlavorAvailable(exhibitDataFlavor) must beTrue
    def s3_2 = transferToClipboard.getData(exhibitDataFlavor)
      .asInstanceOf[MuseumExhibitTransferData].exhibits must contain(e2, e3).only.inOrder
    def s3 = s3_1 and s3_2
  }
}
