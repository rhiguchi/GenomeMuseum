package jp.scid.genomemuseum.model

import actors.{Actor, Future, TIMEOUT}

import jp.scid.gui.DataListModel
import jp.scid.bio.ws.{WebServiceAgent, WebSourceIterator}
import WebServiceAgent.{Identifier, EntryValues}

class SearchResultRetrievingActor(listModel: DataListModel[SearchResult]) extends Actor {
  import SearchResultRetrievingActor._
  import SearchResult.Status._
  
  private var retrievingCount = 0
  
  def act() { loop {
    reactWithin(0) {
      case Cancel() =>
        println("Cancel")
        done()
        exit
      case Succeed() =>
        done()
        exit
      case TIMEOUT => react {
        case IdentifierRetriving(element) if element.isSet =>
          val (identifier, evFut) = element()
          println("IdentifierRetriving: " + identifier)
          try {
            evFut()
          }
          catch {
            case e: Throwable => e.printStackTrace
          }
          println("fut" + evFut())
          retrievingCount += 1
          identifierRetrived(identifier)
          this ! EntryValuesRetriving(evFut)
          
        case EntryValuesRetriving(element) if element.isSet =>
          retrievingCount -= 1
          println("EntryValuesRetriving: " + element())
          elementValueRetrived(element())
          
          if (retrievingCount <= 0)
            this ! Succeed()
      }
    }
  }}
  
  def stop() {
    this ! Cancel()
  }
  
  def add(element: Future[(Identifier, Future[EntryValues])]) {
    this ! IdentifierRetriving(element)
  }
  
  /** 識別子の取得が完了した時の処理 */
  protected def identifierRetrived(identifier: Identifier) {
    // 行オブジェクトの追加
    val rowObj = SearchResult(identifier.value, status = Searching)
    listModel.sourceWithWriteLock { source =>
      source += rowObj
    }
  }
  
  /** 要素の値の取得が完了した時の処理 */
  protected def elementValueRetrived(values: EntryValues) {
    listModel.sourceWithReadLock { source =>    
      findByIdentifier(values.identifier, source) match {
        case Some((element, index)) =>
          element.status = Successed
          element.accession = values.accession
          element.length = values.length
          element.definition = values.definition
          
          // 行オブジェクトに更新通知
          source(index) = element
        case None =>
      }
    }
  }

  // 成功処理
  /** すべての要素の値の取得が完了した時の処理 */
  protected def done() {
  }
  
  /**
   * Identifier の値を持つ要素を検索する
   */
  private def findByIdentifier(identifier: Identifier, results: Seq[SearchResult]):
      Option[(SearchResult, Int)] = {
    val sMap = results.view.zipWithIndex.map( e => (e._1.identifier, e)).toMap
    sMap.get(identifier.value)
  }
}

private object SearchResultRetrievingActor {
  private case class Cancel()
  private case class Succeed()
  private case class IdentifierRetriving(element: Future[(Identifier, Future[EntryValues])])
  private case class EntryValuesRetriving(element: Future[EntryValues])
}
