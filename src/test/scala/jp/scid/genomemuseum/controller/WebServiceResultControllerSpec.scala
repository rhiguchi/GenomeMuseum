package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, TransferHandler}

import org.specs2._

import jp.scid.bio.ws.WebServiceAgent
import jp.scid.genomemuseum.view.TaskProgressTableCell
import jp.scid.genomemuseum.model.TaskProgressModel

class WebServiceResultControllerSpec extends Specification with mock.Mockito {
  private val dataListCtrlSpec = new DataListControllerSpec
  
  def is = "WebServiceResultController" ^
    "dataTable 結合" ^ canBindToTable(createController) ^
    "quickSearchField 結合" ^ dataListCtrlSpec.canBindSearchField(createController) ^
    "プロパティ" ^ propertiesSpec(createController) ^
    "検索文字列モデル" ^ searchTextModelSpec(createController) ^
    "遅延実行検索" ^ canScheduleSearch(createController) ^
    end
  
  def createController() = {
    new WebServiceResultController
  }
  
  protected def emptyWebServiceAgent = {
    val agent = mock[WebServiceAgent]
    agent.getCount(any) returns WebServiceAgent.Query("", 0)
    agent.getFieldValues(any) returns Iterator.empty
    agent.searchIdentifiers(any) returns IndexedSeq.empty
    agent
  }
  
  def canBindToTable(c: => WebServiceResultController) =
    "テーブルレンダラのボタンにダウンロードアクションを設定" ! bindTable(c).downloadAction ^
    dataListCtrlSpec.canBindToTable(c)
  
  def propertiesSpec(c: => WebServiceResultController) =
    "ドラッグは不可" ! properties(c).isTableDraggable ^
    "削除アクション無し" ! properties(c).tableDeleteAction ^
    bt

  def searchTextModelSpec(c: => WebServiceResultController) =
    "変更すると1秒の遅延後検索が実行される" ! searchTextModel(c).callsSearchQueue ^
    bt
  
  def canScheduleSearch(ctrl: => WebServiceResultController) =
    "遅延実行される" ! scheduleSearch(ctrl).delaySearch ^
    "実行は指定時間まで実行されない" ! scheduleSearch(ctrl).notSearchImmediately ^
    "遅延中に新しいクエリを入れると古いクエリの実行はキャンセルされる" ! scheduleSearch(ctrl).canceling ^
    bt
  
  def bindTable(ctrl: WebServiceResultController) = new {
    val table = new JTable
    
    def downloadAction = {
      val renderer = mock[TaskProgressTableCell]
      table.setDefaultRenderer(classOf[TaskProgressModel], renderer)
      ctrl.bindTable(table)
      there was one(renderer).setExecuteButtonAction(ctrl.downloadAction.peer)
    }
  }
  
  def properties(ctrl: WebServiceResultController) = new {
    def isTableDraggable = ctrl.isTableDraggable must beFalse
    def tableDeleteAction = ctrl.tableDeleteAction must beNone
  }
  
  // 検索文字列モデル
  def searchTextModel(ctrl: WebServiceResultController) = new {
    val agent = emptyWebServiceAgent
    ctrl.tableModel.agent = agent
    
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
  def scheduleSearch(ctrl: WebServiceResultController) = new {
    val agent = emptyWebServiceAgent
    ctrl.tableModel.agent = agent
    
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
