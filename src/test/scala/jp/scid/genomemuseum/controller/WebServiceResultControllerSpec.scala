package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, TransferHandler}

import org.specs2._

import jp.scid.bio.ws.WebServiceAgent
import DataListController.View

class WebServiceResultControllerSpec extends Specification with mock.Mockito with DataListControllerSpec {
  private type Factory = View => WebServiceResultController
  
  def is = "WebServiceResultController" ^
    "ビュー - テーブル" ^ viewTableSpec2(createController) ^
    "検索文字列モデル" ^ searchTextModelSpec2(createController) ^
    "遅延実行検索" ^ canScheduleSearch(createController) ^
    end
  
  def createController(view: View) = {
    val ctrl = new WebServiceResultController(view)
    ctrl.tableModel.agent = emptyWebServiceAgent
    ctrl
  }
  
  def emptyWebServiceAgent = {
    val agent = mock[WebServiceAgent]
    agent.getCount(any) returns WebServiceAgent.Query("", 0)
    agent.getFieldValues(any) returns Iterator.empty
    agent.searchIdentifiers(any) returns IndexedSeq.empty
    agent
  }
  
  def viewTableSpec2(f: Factory) =
    "ドラッグ不可能" ! viewTable2(f).isNotDragEnabled ^
    super.viewTableSpec(f)
  
  def searchTextModelSpec2(f: Factory) =
    "変更すると1秒の遅延後検索が実行される" ! searchTextModel2(f).callsSearchQueue ^
    super.searchTextModelSpec(f)
  
  def canScheduleSearch(f: Factory) =
    "遅延実行される" ! scheduleSearch(f).delaySearch ^
    "実行は指定時間まで実行されない" ! scheduleSearch(f).notSearchImmediately ^
    "遅延中に新しいクエリを入れると古いクエリの実行はキャンセルされる" ! scheduleSearch(f).canceling ^
    bt
  
  class TestBase(f: Factory) extends super.TestBase(f) {
    override val ctrl = f(view)
    
    val agent = emptyWebServiceAgent
    ctrl.tableModel.agent = agent
  }
  
  // テーブル
  def viewTable2(f: Factory) = new TestBase(f) {
    def isNotDragEnabled = table.getDragEnabled must beFalse
  }
  
  // 検索文字列モデル
  def searchTextModel2(f: Factory) = new TestBase(f) {
    def callsSearchQueue = {
      List("test", "query", "aav") foreach { query =>
        ctrl.searchTextModel := query
        Thread.sleep(1000)
      }
      
      there was three(agent).getCount(any) then
        one(agent).getCount("test") then
        one(agent).getCount("query") then
        one(agent).getCount("aav")
    }
  }
  
  // 遅延検索
  def scheduleSearch(f: Factory) = new TestBase(f) {
    def delaySearch = {
      val future = ctrl.scheduleSearch("query", 500)
      Thread.sleep(800)
      there was one(agent).getCount(any) then
        one(agent).getCount("query")
    }
    
    def notSearchImmediately = {
      ctrl.scheduleSearch("query", 500)
      there was no(agent).getCount(any)
    }
    
    def canceling = {
      ctrl.scheduleSearch("query", 500)
      ctrl.scheduleSearch("", 0)
      Thread.sleep(1000)
      there was no(agent).getCount(any)
    }
  }
}
