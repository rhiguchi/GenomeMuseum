package jp.scid.genomemuseum.controller

import org.specs2._
import mock._

import javax.swing.{JTable, JTextField, JLabel, TransferHandler}

import jp.scid.gui.event.DataListSelectionChanged
import jp.scid.genomemuseum.{model, gui, view}
import model.{MuseumExhibitService, MuseumExhibit}
import gui.ExhibitTableModel
import view.FileContentView

class MuseumExhibitListControllerSpec extends Specification with Mockito {
  def is = "MuseumExhibitListController" ^
    "初期状態" ^ initialSpec(controller) ^ bt ^
    "データテーブル" ^ dataTableSpec(controller) ^ bt ^
    "検索フィールド" ^ searchFieldSpec(controller) ^ bt ^
    "削除アクション" ^ removingActionSpec(controller) ^ bt ^
    "プロパティ" ^ propertiesSpec(controller) ^ bt ^
    end
  
  def controller = {
    val spiedTable = spy(new JTable)
    val spiedQuickSearchField = spy(new JTextField)
    val spiedStatusField = spy(new JLabel)
    val spiedContentViewer = spy(new FileContentView)
    
    val ctrl = new MuseumExhibitListController(spiedTable, spiedQuickSearchField,
        spiedStatusField, spiedContentViewer) {
      override def createTableModel = spy(new ExhibitTableModel2)
    }
    ctrl.bind()
    ctrl
  }
  
  def initialSpec(ctrl: => MuseumExhibitListController) =
    "無選択" ! controller(ctrl).notSelected ^
    "削除アクションが無効" ! controller(ctrl).removingActionUnabled
  
  def dataTableSpec(ctrl: => MuseumExhibitListController) =
    "モデルの適用" ! dataTable(ctrl).bindTableModel ^
    "選択モデルの適用"  ! dataTable(ctrl).bindSelectionModel ^
    "転送ハンドラの適用"  ! dataTable(ctrl).hasTransferHandler
  
  def searchFieldSpec(ctrl: => MuseumExhibitListController) =
    "モデル適用" ! searchField(ctrl).boundModel ^
    "テーブルのフィルタリング効果"  ! searchField(ctrl).callsFilterWith
  
  def removingActionSpec(ctrl: => MuseumExhibitListController) =
    "選択時に有効" ! removingAction(ctrl).availableToSelect ^
    "無選択時には無効" ! removingAction(ctrl).unavailableToSelect ^
    "tableModel#removeSelections コール" ! removingAction(ctrl).callsTableMethod
  
  def propertiesSpec(ctrl: => MuseumExhibitListController) =
    "dataService 適用" ! properties(ctrl).applyDataService ^
    "loadManager 適用" ! properties(ctrl).applyLoadManager
  
  class TestBase(ctrl: MuseumExhibitListController) {
    def dataTable = ctrl.dataTable
    
    def tableModel = ctrl.tableModel
  }
  
  def controller(ctrl: MuseumExhibitListController) = new TestBase(ctrl) {
    def notSelected = ctrl.tableSelection must beEmpty
    
    def removingActionUnabled = ctrl.removeSelectionAction.enabled must beFalse
  }
  
  def dataTable(ctrl: MuseumExhibitListController) = new TestBase(ctrl) {
    def bindTableModel = {
      dataTable.getModel must_== tableModel.tableModel
    }
    
    def bindSelectionModel = {
      dataTable.getSelectionModel must_== tableModel.selectionModel
    }
    
    def hasTransferHandler = {
      dataTable.getTransferHandler must beAnInstanceOf[MuseumExhibitListTransferHandler]
    }
  }
  
  def searchField(ctrl: MuseumExhibitListController) = new TestBase(ctrl) {
    def quickSearchField = ctrl.quickSearchField
    
    def applyTextToModel(text: String*) =
      text foreach ctrl.searchTextModel.:=
    
    def boundModel = {
      applyTextToModel("test text", "txt2", "xxx")
      there was one(quickSearchField).setText("test text") then
      one(quickSearchField).setText("txt2") then
      one(quickSearchField).setText("xxx")
    }
    
    def callsFilterWith = {
      applyTextToModel("test text", "txt2", "xxx")
      there was one(tableModel).filterWith("test text") then
      one(tableModel).filterWith("txt2") then
      one(tableModel).filterWith("xxx")
    }
  }
  
  def removingAction(ctrl: MuseumExhibitListController) = new TestBase(ctrl) {
    def action = ctrl.removeSelectionAction
    
    def publishTableSelectionEvent(elements: MuseumExhibit*) =
      tableModel.publish(DataListSelectionChanged(tableModel, false, elements.toList))
    
    def availableToSelect = {
      publishTableSelectionEvent(mock[MuseumExhibit])
      action.enabled must beTrue
    }
    
    def unavailableToSelect = {
      publishTableSelectionEvent()
      action.enabled must beFalse
    }
    
    def callsTableMethod = {
      action()
      there was one(tableModel).removeSelections()
    }
  }
  
  def properties(ctrl: MuseumExhibitListController) = new TestBase(ctrl) {
    def applyDataService = {
      val service = mock[MuseumExhibitService]
      service.allElements returns Nil
      ctrl.dataService = service
      there was one(tableModel).dataService_=(service)
    }
    
    def applyLoadManager = {
      val mgr = mock[MuseumExhibitLoadManager]
      ctrl.loadManager = mgr
      ctrl.tableTransferHandler.loadManager must beSome(mgr)
    }
  }
  
  class ExhibitTableModel2 extends ExhibitTableModel {
    override def filterWith(text: String) {
      super.filterWith(text)
    }
    
    override def removeSelections() {
      super.removeSelections()
    }
  }
}
