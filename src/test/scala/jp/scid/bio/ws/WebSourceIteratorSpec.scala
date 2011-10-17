package jp.scid.bio.ws

import org.specs2._
import mock._
import actors.{Futures, Future}
import WebServiceAgent.{Identifier, EntryValues}

class WebSourceIteratorSpec extends Specification with Mockito {
  def is = "WebSourceIterator" ^
    "hasNext" ^
      "コンストラクタに 0 を超える数を指定" ! hasNext.s1 ^
      "コンストラクタに 0 を指定" ! hasNext.s2 ^
    bt ^ "next" ^
      "agent#searchIdentifiers が 1 コール" ! next.s1 ^
      "値が agent から出されたもの" ! next.s2 ^
      "agent#searchIdentifiers コール" ! next.s3 ^
      "値の終了" ! next.s4 ^
      "指定したサイズよりも取得した識別子数が少ない時も値を取得" ! next.s5 ^
      "agent#searchIdentifiers コール" ! next.s6 ^
      "ページング" ! next.s7
  
  val sampleId1 = Identifier("id1")
  val sampleId2 = Identifier("id2")
  val sampleId3 = Identifier("id3")
  val idList = IndexedSeq(sampleId1, sampleId2, sampleId3)
  
  val sampleEV1 = EntryValues(sampleId1, "Acc1")
  val sampleEV2 = EntryValues(sampleId2, "Acc2")
  val sampleEV3 = EntryValues(sampleId3, "Acc3")
  val evList = IndexedSeq(sampleEV1, sampleEV2, sampleEV3)
  
  abstract class TestBase {
    val agentMock = mock[WebServiceAgent]
    agentMock.searchIdentifiers("test", 0, 3) returns Futures.future {
      idList
    }
    agentMock.searchIdentifiers("test2", 0, 2) returns Futures.future {
      IndexedSeq(sampleId1)
    }
    agentMock.searchIdentifiers("test3", 0, 2) returns Futures.future {
      IndexedSeq(sampleId1, sampleId2)
    }
    agentMock.searchIdentifiers("test3", 2, 2) returns Futures.future {
      IndexedSeq(sampleId3, sampleId1)
    }
    agentMock.searchIdentifiers("test3", 4, 1) returns Futures.future {
      IndexedSeq(sampleId2)
    }
    agentMock.getFieldValuesFor(idList) returns Futures.future {
      evList
    }
    agentMock.getFieldValuesFor(IndexedSeq(sampleId1)) returns Futures.future {
      IndexedSeq(sampleEV1)
    }
    agentMock.getFieldValuesFor(IndexedSeq(sampleId1, sampleId2)) returns Futures.future {
      IndexedSeq(sampleEV1, sampleEV2)
    }
    agentMock.getFieldValuesFor(IndexedSeq(sampleId3, sampleId1)) returns Futures.future {
      IndexedSeq(sampleEV3, sampleEV1)
    }
    agentMock.getFieldValuesFor(IndexedSeq(sampleId2)) returns Futures.future {
      IndexedSeq(sampleEV2)
    }
    
    val source = new WebSourceIterator(agentMock, "test", 3)
    val source2 = new WebSourceIterator(agentMock, "test2", 2)
    val source0 = new WebSourceIterator(agentMock, "test", 0)
    val source3 = new WebSourceIterator(agentMock, "test3", 5)
    source3.defaultLimit = 2
  }
  
  val hasNext = new TestBase {
    def s1 = source.hasNext must beTrue
    def s2 = source0.hasNext must beFalse
  }
  
  val next = new TestBase {
    // agent のメソッドがコールされるまで待機
    val (id1, valuFut1) = source.next.apply
    val ev1 = valuFut1.apply
    
    val (id2, valuFut2) = source.next.apply
    val (id3, valuFut3) = source.next.apply
    
    // 指定したサイズよりも取得した識別子数が少ない
    val (id2_1, valuFut2_1) = source2.next.apply
    val ev2_1 = valuFut2_1.apply
    
    // ページング
    source3.next
    source3.next
    source3.next
    source3.next
    source3.next
    
    def s1 = there was one(agentMock).searchIdentifiers("test", 0, 3)
    def s2 = id1 must_== sampleId1
    def s3 = there was one(agentMock).getFieldValuesFor(idList)
    def s4 = source.hasNext must beFalse
    
    val s5 = (source2.hasNext must beTrue) and (source2.next.apply._1 must_==
      Identifier.empty)
    def s6 = there was one(agentMock).getFieldValuesFor(IndexedSeq(sampleId1))
    
    val s7_1 = there was one(agentMock).searchIdentifiers("test3", 0, 2)
    val s7_2 = there was one(agentMock).searchIdentifiers("test3", 2, 2)
    val s7_3 = there was one(agentMock).searchIdentifiers("test3", 4, 1)
    val s7 = s7_1 and s7_2 and s7_3
  }
}
