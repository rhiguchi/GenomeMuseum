package jp.scid.genomemuseum.gui

import org.specs2._
import mock._

import jp.scid.gui.DataListModel
import jp.scid.bio.ws.{WebServiceAgent}
import WebServiceAgent.{Query, Identifier, EntryValues}
import jp.scid.genomemuseum.model.SearchResult

class WebSearchManagerSpec extends Specification with Mockito {
  def is = "WebSearchManager" ^
    "検索結果が空" ^ notFound(managetWith(emptyAgent)) ^ bt ^
    "検索結果が存在する" ^ resultsExist(managetWith(findableAgent)) ^ bt ^
    "検索結果が非常に多い" ^ tooManyResults(managetWith(agentWithMuchCount)) ^ bt ^
    "検索反応" ^ response(managetWith(findableAgent)) ^ bt ^
    "検索" ^ canSearch(managetWith(emptyAgent)) ^ bt ^
    end
  
  def managetWith(agent: => WebServiceAgent) = {
    val listModel = mock[DataListModel[SearchResult]]
    new WebSearchManager(listModel, agent)
  }
  
  /** 空結果を返すエージェント */
  def emptyAgent = agentFor(500, 0, 500)
  
  /** いくつかの結果を返すエージェント */
  def findableAgent = agentFor(500, 4, 500)
  
  /** 多量の該当数を返すエージェント */
  def agentWithMuchCount = {
    val agent = mock[WebServiceAgent]
    makeAgentMockCount(agent, 500, 100000)
    agent
  }
  
  def makeAgentMockCount(agent: WebServiceAgent, waitTime: Long, count: Int) {
    agent.getCount(any) answers { query =>
      Thread.sleep(waitTime)
      Query(query.asInstanceOf[String], count)
    }
  }
  
  def makeAgentMockEntryValues(agent: WebServiceAgent, waitTime: Long, values: Seq[EntryValues]) {
    val ids = values map (r => r.identifier)
    agent.searchIdentifiers(any) answers { query => 
      Thread.sleep(waitTime)
      ids.toIndexedSeq
    }
    agent.getFieldValues(any) answers { query => 
      Thread.sleep(waitTime)
      values.toIndexedSeq
    }
  }
  
  def agentFor(countTime: Long, count: Int, identifiersTime: Long) = {
    val agent = mock[WebServiceAgent]
    val results = 0 until count map (i => WebServiceAgent.EntryValues(
      WebServiceAgent.Identifier("id" + i)))
    
    makeAgentMockCount(agent, countTime, count)
    makeAgentMockEntryValues(agent, identifiersTime, results)
    agent
  }
  
  def agentFor(results: Seq[EntryValues]) = {
    val agent = mock[WebServiceAgent]
    makeAgentMockCount(agent, 0, results.size)
    makeAgentMockEntryValues(agent, 0, results)
    agent
  }
  
  def identifiersOf(values: String*) =
    values.map(new WebServiceAgent.Identifier(_))
  
  def notFound(m: => WebSearchManager) =
    "モデルが空に設定される" ! listModel(m).sourceIsEmpty ^
    "該当数が 0" ! searchQuery(m).countZero
  
  def resultsExist(m: => WebSearchManager) =
    "モデルに要素が設定される" ! listModel(m).sourceIsNotEmpty ^
    "該当数が 0 より多い" ! searchQuery(m).countGtZero ^
    "識別子を取得する" ! searchQuery(m).haveResult ^
    "識別子リストが空ではない" ! searchQuery(m).getSomeResults
  
  def response(m: => WebSearchManager) =
    "制御がすぐ戻る" ! searching(m).backImmediately ^
    "該当数の取得には結果の取得を待たない" ! searching(m).notWaitResultToGetCount
  
  def tooManyResults(m: => WebSearchManager) =
    "モデルが空に設定される" ! listModel(m).sourceIsEmpty ^
    "識別子を取得しない" ! searchQuery(m).haveNoResult
  
  def canSearch(m: => WebSearchManager) =
    "エージェントから返された結果が設定される" ! searching(m).appliedEntryValues
  
  class TestBase(val manager: WebSearchManager) {
    def listModel = manager.listModel
  }
  
  def listModel(m: WebSearchManager) = new TestBase(m) {
    def sourceIsEmpty = {
      manager.search("q").apply.resultTask.map(_.apply)
      there was one(listModel).source_=(Nil)
    }
    
    def sourceIsNotEmpty = {
      val result = manager.search("q").apply.resultTask.map(_.apply).get
      there was atLeastOne(listModel).source_=(result)
    }
  }
  
  def searchQuery(m: WebSearchManager) = new TestBase(m) {
    def countZero = manager.search("q").apply.count must_== 0
    
    def countGtZero = manager.search("q").apply.count must be_>(0)
    
    def haveResult = manager.search("q").apply.resultTask must beSome
    
    def haveNoResult = manager.search("q").apply.resultTask must beNone
    
    def getSomeResults = manager.search("q").apply.resultTask
      .map(_.apply).getOrElse(Nil) must not beEmpty
  }
  
  def searching(m: WebSearchManager) = new TestBase(m) {
    def backImmediately = {
      val startTime = System.currentTimeMillis
      m.search("q")
      System.currentTimeMillis - startTime must be_<(200L)
    }
    
    def notWaitResultToGetCount = {
      m.agent = agentFor(500, 1, 3000)
      val startTime = System.currentTimeMillis
      val result = m.search("q").apply
      System.currentTimeMillis - startTime must be_<(700L)
    }
    
    def appliedEntryValues = {
      val evs = 0 until 3 map (i => EntryValues(Identifier("id" + i), "acc" + i)) toList;
      m.agent = agentFor(evs)
      val results = m.search("q").apply.resultTask.get.apply
      
      there was two(m.listModel).source_=(results)
    }
  }
}
