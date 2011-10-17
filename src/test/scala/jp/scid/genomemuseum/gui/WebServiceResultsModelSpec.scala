package jp.scid.genomemuseum.gui

import org.specs2._

import actors.{Futures, Future, Actor}
import Actor.State._

import jp.scid.bio.ws.WebServiceAgent
import WebServiceAgent.{Identifier, EntryValues}

class WebServiceResultsModelSpec extends Specification {
  def is = "WebServiceResultsModel" ^
    "getSourceBy" ^
      "カウント取得" ! getSourceBy.s1 ^
    bt ^ "renewActor" ^
      "actor が設定される" ! renewActor.s1 ^
      "actor が開始される" ! renewActor.s2 ^
      "要素の遅延セット" ! renewActor.s3 ^
      "要素のセット" ! renewActor.s4 ^
      "要素の値の遅延セット" ! renewActor.s5 ^
      "要素の値のセット" ! renewActor.s6
  
  val agent = new WebServiceAgent {
    val id1 = Identifier("id1")
    val id2 = Identifier("id2")
    val id3 = Identifier("id3")
    val id4 = Identifier("id4")
    val id5 = Identifier("id5")
    
    val queryIdMap = Map(
      "query1" -> IndexedSeq(id1, id2),
      "query2" -> IndexedSeq(id3, id4, id5)
    )
    
    val idValMap = Map(
      Identifier.empty -> EntryValues.empty,
      id1 -> EntryValues(id1, "acc1"),
      id2 -> EntryValues(id2, "acc2"),
      id3 -> EntryValues(id3, "acc3"),
      id4 -> EntryValues(id4, "acc4"),
      id5 -> EntryValues(id5, "acc5")
    )
    
    def count(query: String): Future[Int] = Futures.future {
      Thread.sleep(100)
      queryIdMap(query).size
    }
    
    def searchIdentifiers(query: String, offset: Int, limit: Int): Future[IndexedSeq[Identifier]] = Futures.future {
      Thread.sleep(100)
      queryIdMap(query).slice(offset, limit)
    }
    
    def getFieldValuesFor(identifiers: Seq[Identifier]): Future[IndexedSeq[EntryValues]] = Futures.future {
      sleep
      identifiers.map(idValMap.apply).toIndexedSeq
    }
    
    private def sleep = Thread.sleep(500)
  }
  
  abstract class TestBase {
    val model = new WebServiceResultsModel
    model.agent = agent
  }
  
  def getSourceBy = new TestBase {
    
//    model.searchQuery = "lung cancer"
    
    def s1 = model.getSourceBy("query1").size must_== 2
  }
  
  val renewActor = new TestBase {
    assert(model.sourceActor == None)
    
    model.renewActor("query1")
    
    val s1 = model.sourceActor must beSome
    
    val s2 = model.sourceActor.get.getState must_== Runnable
    
    val s3 = model.source.size must_== 0
    
    Thread.sleep(400)
    
    val s4 = model.source.size must_== 2
    
    val s5 = model.source(0).accession must_== ""
    
    Thread.sleep(300)
    
    assert(model.sourceActor.get.getState == Suspended)
    val s6 = model.source(0).accession must_!= ""
  }
}
