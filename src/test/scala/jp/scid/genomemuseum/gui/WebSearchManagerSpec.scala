package jp.scid.genomemuseum.gui

import org.specs2._

import actors.{Futures, Future}

import jp.scid.gui.DataListModel
import jp.scid.bio.ws.{WebServiceAgent, WebSourceIterator}
import WebServiceAgent.{Identifier, EntryValues}
import jp.scid.genomemuseum.model.SearchResult

class WebSearchManagerSpec extends Specification {
  import WebSearchManager._
  
  def is = "WebSearchManager" ^
    "search" ^
      "Succeed イベント取得" ! search.s1 ^
      "項目数の増加" ! search.s2 ^
      "再検索時にリストモデルを消去" ! search.s3 ^
    bt ^ "cancel" ^ 
      "開始前に読んでも false" ! cancel.s1 ^
      "検索中には true を返す" ! cancel.s2 ^
      "Cancel イベントコール" ! cancel.s3 ^
      "項目は追加されない" ! cancel.s4 ^
    bt ^ "項目数取得のタイムアウト" ^ 
      "CountRetrivingTimeOut イベントコール" ! to.s1 ^
      "項目は追加されない" ! to.s2 ^
    bt ^ "多い項目数の時はデータ取得をしない" ^ 
      "CountRetrieved イベントコール" ! tmi.s1 ^
      "項目は追加されない" ! tmi.s2 
  
  val agent = new WebServiceAgent {
    val id1 = Identifier("id1")
    val id2 = Identifier("id2")
    val id3 = Identifier("id3")
    val id4 = Identifier("id4")
    
    val ev1 = EntryValues(id1, "acc1")
    val ev2 = EntryValues(id2, "acc2")
    val ev3 = EntryValues(id3, "acc3")
    val ev4 = EntryValues(id4, "acc4")
    
    val query1Items = IndexedSeq(id1, id2, id3)
    
    val itemsMap = Map(
      id1 -> EntryValues(id1, "acc1"),
      id2 -> EntryValues(id2, "acc2"),
      id3 -> EntryValues(id3, "acc3"),
      id4 -> EntryValues(id4, "acc4")
    )
    
    def count(query: String) = Futures.future { 
      query match {
        case "query" => query1Items.size
        case "query2" =>
          Thread.sleep(500)
          query1Items.size
        case "query3" => 0
        case "query4" =>
          Thread.sleep(5000)
          0
      }
    }
      
    def searchIdentifiers(query: String, offset: Int, limit: Int) = Futures.future {
      query match {
        case "query" =>
          query1Items.slice(offset, limit)
        case "query2" =>
          Thread.sleep(500)
          query1Items.slice(offset, limit)
      }
    }
    
    def getFieldValuesFor(identifiers: Seq[Identifier]) = Futures.future {
      identifiers.map(itemsMap.apply).toIndexedSeq
    }
  }
  
  abstract class TestBase {
    val manager = new WebSearchManager(agent)
    
    def listModel = manager.listModel
  }
  
  val search = new TestBase {
    var succeedPublished = false
    
    manager.reactions += {
      case Succeed() => succeedPublished = true
    }
    
    assert(!succeedPublished)
    assert(listModel.sourceSize == 0)
    
    manager.search("query")
    
    Thread.sleep(500)
    
    
    val s1 = succeedPublished must beTrue
    
    val s2 = todo //listModel.sourceSize must_== 3
    
    manager.search("query3")
    
    val s3 = listModel.sourceSize must_== 0
  }
  
  val cancel = new TestBase {
    var canceledPublished = false
    
    manager.reactions += {
      case Canceled() => canceledPublished = true
    }
    
    assert(!canceledPublished)
    assert(listModel.sourceSize == 0)
    
    val s1 = manager.cancel must beFalse
    
    manager.search("query2")
    
    val cancelResult = manager.cancel
    
    val s2 = cancelResult must beTrue
    
    val s3 = canceledPublished must beTrue
    
    val s4 = listModel.sourceSize must_== 0
  }
  
  val to = new TestBase {
    var timeOutPublished = false
    
    manager.reactions += {
      case CountRetrivingTimeOut() => timeOutPublished = true
    }
    
    assert(!timeOutPublished)
    
    manager.timeoutTime = 0L
    manager.search("query4")
    
    def s1 = timeOutPublished must beTrue
    
    Thread.sleep(300)
    
    val s2 = listModel.sourceSize must_== 0
  }
  
  val tmi = new TestBase {
    var tmrfPublished = false
    
    manager.reactions += {
      case CountRetrieved(count) => tmrfPublished = true
    }
    
    assert(!tmrfPublished)
    
    manager.resultMaximumCount = 1
    manager.search("query")
    
    def s1 = tmrfPublished must beTrue
    
    Thread.sleep(300)
    
    val s2 = listModel.sourceSize must_== 0
  }
}
