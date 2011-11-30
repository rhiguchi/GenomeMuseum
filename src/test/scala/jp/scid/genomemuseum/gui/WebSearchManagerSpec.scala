package jp.scid.genomemuseum.gui

import org.specs2._
import mock._

import java.net.URL

import actors.{Futures, Future}

import jp.scid.gui.DataListModel
import jp.scid.bio.ws.{WebServiceAgent}
import WebServiceAgent.{Query, Identifier, EntryValues}
import jp.scid.genomemuseum.model.SearchResult
import WebSearchManager._

class WebSearchManagerSpec extends Specification with Mockito {
  def is = "WebSearchManager" ^
    "SearchResult オブジェクト作成" ^ searchResultCreationSpec(simpleManager) ^ bt ^
    "SearchResult オブジェクト構築" ^ canMakeSearchingResult(simpleManager) ^ bt ^
    "該当数取得イベント発行" ^ canPublishCountRetrieved ^ bt ^
    "識別子取得イベント発行" ^ canPublishIdentifiersRetrieved ^ bt ^
    "サービスのデータから検索" ^ canRetrieve ^ bt ^
    end
  
  def searchResultCreationSpec(m: => WebSearchManager) =
    "identifier 値適用" ! searchResultOf(m).identifier ^
    "sourceUrl は agent からの値を適用" ! searchManager.applySourceURL
  
  def canMakeSearchingResult(m: => WebSearchManager) =
    "accession 値の適用" ! makeSearchingResult(m).accession ^
    "definition 値の適用" ! makeSearchingResult(m).definition ^
    "length 値の適用" ! makeSearchingResult(m).length ^
    "done 値の更新" ! makeSearchingResult(m).done
  
  def canPublishCountRetrieved =
    "発行" ! publishCountRetrieved.publish ^
    "キャンセルで発行しない" ! publishCountRetrieved.notPublishIfCanceled
  
  def canPublishIdentifiersRetrieved =
    "発行" ! publishIdentifiersRetrieved.publish ^
    "キャンセルで発行しない" ! publishIdentifiersRetrieved.notPublishIfCanceled
  
  def canRetrieve =
    "リストモデルに適用" ! searchManager.setsSource ^
    "行更新" ! searchManager.callsItemUpdate
  
  /** 識別子リスト作成 */
  def identifiersFor(count: Int) =
    0 until count map (i => Identifier(i.toString)) toIndexedSeq
  
  /** カウントを返す重量処理をするエージェント */
  def countAgent(queryText: String, count: Int, waitTime: Long = 0) = {
    val agent = mock[WebServiceAgent]
    val query = Query(queryText, count)
    agent.getCount(queryText) answers { arg =>
      Thread.sleep(waitTime)
      query
    }
    agent.searchIdentifiers(any) returns IndexedSeq.empty
    agent.getFieldValues(any) returns Iterator.empty
    agent
  }
  
  /** 識別子を返す重量処理をするエージェント */
  def identifierAgent(queryText: String, identifiers: Seq[Identifier],
      waitTime: Long = 0) = {
    val agent = countAgent(queryText, identifiers.size, 0)
    agent.searchIdentifiers(agent.getCount(queryText)) answers { arg =>
      Thread.sleep(waitTime)
      identifiers.toIndexedSeq
    }
    agent
  }
  
  /** 属性値返す重量処理をするエージェント */
  def entryValuesAgent(queryText: String, evs: Seq[EntryValues], waitTime: Long = 0) = {
    val identifiers = evs map (_.identifier)
    val agent = identifierAgent(queryText, identifiers, 0)
    agent.getFieldValues(identifiers) answers { arg =>
      Thread.sleep(waitTime)
      evs.iterator
    }
    agent
  }
  
  def managerOf(query: String, agent: WebServiceAgent) = {
    val model = mock[DataListModel[SearchResult]]
    new WebSearchManager(model, agent, "q")
  }
  
  def simpleManager = managerOf("", countAgent("", 0))
  
  def searchResultOf(m: WebSearchManager) = new Object {
    def identifier =
      m.searchResultOf(Identifier("id")).identifier must_== "id"
  }
  
  def makeSearchingResult(m: WebSearchManager) = new Object {
    val identifier = Identifier("id")
    
    def makedSearchingResultMock(ev: EntryValues) = {
      val resultMock = mock[SearchResult]
      m.makeSearchingResult(resultMock, ev)
      resultMock
    }
    
    def accession = there was one(makedSearchingResultMock(
        EntryValues(identifier, accession = "accession"))).accession_=("accession")
    
    def definition = there was one(makedSearchingResultMock(
        EntryValues(identifier, definition = "definition"))).definition_=("definition")
    
    def length = there was one(makedSearchingResultMock(
        EntryValues(identifier, length = 12345))).length_=(12345)
    
    def done = there was one(makedSearchingResultMock(
        EntryValues(identifier))).done_=(true)
  }
  
  def actAndCancel(manager: WebSearchManager, after: Long = 100) = {
    val future = Futures.future(manager.run)
    Thread.sleep(after)
    manager.cancel
    future
  }
  
  def publishCountRetrieved = new Object {
    var event: Option[CountRetrieved] = None
    
    def setReactionsTo(manager: WebSearchManager) = manager.reactions += {
      case e: CountRetrieved => event = Some(e)
    }
    
    def publish = {
      val manager = managerOf("q", countAgent("q", 100, 0))
      setReactionsTo(manager)
      manager.run()
      
      event must beSome(CountRetrieved(100))
    }
    
    def notPublishIfCanceled = {
      val manager = managerOf("q", countAgent("q", 3, 500))
      setReactionsTo(manager)
      actAndCancel(manager).apply
      
      event must beNone
    }
  }
  
  def publishIdentifiersRetrieved = new Object {
    val identifiers = identifiersFor(3)
    
    var event: Option[IdentifiersRetrieved] = None
    
    def setReactionsTo(manager: WebSearchManager) = manager.reactions += {
      case e: IdentifiersRetrieved => event = Some(e)
    }
    
    def publish = {
      val manager = managerOf("q", identifierAgent("q", identifiers))
      setReactionsTo(manager)
      
      manager.run()
      
      event must beSome(IdentifiersRetrieved(identifiers))
    }
    
    def notPublishIfCanceled = {
      val manager = managerOf("q", identifierAgent("q", identifiers, 500))
      setReactionsTo(manager)
      actAndCancel(manager).apply
      
      event must beNone
    }
  }
  
  def searchManager() = new Object {
    def setsSource = {
      val model = mock[DataListModel[SearchResult]]
      val entryValuesList = identifiersFor(3) map (i =>
        EntryValues(i, i.value + "-acc", 123, i.value + "-def"))
      val manager = new WebSearchManager(model, entryValuesAgent("q", entryValuesList), "q")
      
      manager.run()
      
      val results = entryValuesList map { e => 
        val result = manager.searchResultOf(e.identifier)
        manager.makeSearchingResult(result, e)
        result
      }
      
      there was one(model).source_=(results)
    }
    
    def callsItemUpdate = {
      val model = mock[DataListModel[SearchResult]]
      val entryValuesList = identifiersFor(3) map (i =>
        EntryValues(i, i.value + "-acc", 123, i.value + "-def"))
      val manager = new WebSearchManager(model, entryValuesAgent("q", entryValuesList), "q")
      
      manager.run()
      
      there was one(model).itemUpdated(0) then
        one(model).itemUpdated(1) then
        one(model).itemUpdated(2) then
        no(model).itemUpdated(3)
    }
    
    def applySourceURL = {
      val model = mock[DataListModel[SearchResult]]
      
      val identifier = Identifier("sample")
      val url = new URL("http://example.com")
      val agent = mock[WebServiceAgent]
      agent.getSource(identifier) returns url
      
      val manager = new WebSearchManager(model, agent, "")
      
      manager.searchResultOf(identifier).sourceUrl must beSome(url)
    }
  }
}
