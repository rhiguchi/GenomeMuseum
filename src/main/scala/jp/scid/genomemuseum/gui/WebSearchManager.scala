package jp.scid.genomemuseum.gui

import actors.{Futures, Future}

import jp.scid.bio.ws.{WebServiceAgent}
import WebServiceAgent.{Query, Identifier, EntryValues}
import jp.scid.gui.DataListModel
import jp.scid.genomemuseum.model.{SearchResult}

object WebSearchManager {
  sealed abstract class Event extends swing.event.Event
  /** 開始通知 */
  case class Started() extends Event
  /** 該当数取得済み通知 */
  case class CountRetrieved(count: Int) extends Event
  /** 識別子取得済み通知 */
  case class IdentifiersRetrieved(identifiers: IndexedSeq[Identifier]) extends Event
  /** 要素属性値取得中通知 */
  case class EntryValuesRetrieving() extends Event
  /** 取得待機時間切れ通知 */
  case class RetrievingTimeOut() extends Event
  /** 人為的に取り消されたことの通知 */
  case class Canceled() extends Event
  /** 全ての取得に成功したことの数値 */
  case class Success() extends Event
  /** 一連の処理が完了したことの通知 */
  case class Done() extends Event
}

/**
 * ウェブ検索結果をリストモデルに適用するクラス。
 */
class WebSearchManager(listModel: DataListModel[SearchResult],
    searchAgent: WebServiceAgent, query: String) extends Runnable with swing.Publisher {
  import WebSearchManager._
  
  /** リストに表示する最大項目数 */
  var resultMaximumCount = 200
  
  @volatile
  private var canceled = false
  
  private def notCanceled[A](a: A) = !canceled
  
  def run() {
    search(query)
  }
  
  def cancel = canceled = true
  
  def search(query: String) {
    publish(Started())
    
    awaitOption(searchAgent.getCount(query)).filter(notCanceled).flatMap { query =>
      // 該当数取得イベント発行
      publish(CountRetrieved(query.count))
      
      query.count <= resultMaximumCount match {
        case true => awaitOption(searchAgent.searchIdentifiers(query))
        case false => Some(IndexedSeq.empty)
      }
    }
    .filter(notCanceled).flatMap { identifiers =>
      // 識別子取得イベント発行
      publish(IdentifiersRetrieved(identifiers))
      
      // 行の挿入
      val items = identifiers.map(searchResultOf)
      listModel.source = items
      
      awaitOption((items, searchAgent.getFieldValues(identifiers)))
    }
    .filter(notCanceled).flatMap { case (items, valuesIte) =>
      // 情報取得イベント発行
      publish(EntryValuesRetrieving())
      
      updateResult(items, 0, valuesIte) match {
        case true => Some(items)
        case false => None
      }
    } match {
      case Some(items) => publish(Success())
      case None => canceled match {
        case false => publish(RetrievingTimeOut())
        case true => publish(Canceled())
      }
    }
    
    publish(Done())
  }
  
  protected[gui] def searchResultOf(identifier: Identifier) = {
    val url = Option(searchAgent.getSource(identifier))
    SearchResult(identifier.value, sourceUrl = url)
  }
  
  protected[gui] def makeSearchingResult(element: SearchResult, entryValues: EntryValues) {
    element.accession = entryValues.accession
    element.definition = entryValues.definition
    element.length = entryValues.length
    element.done = true
  }
  
  private def awaitOption[A](a: => A) = {
    val fut = Futures.future{a}
    Futures.awaitAll(30 * 1000, fut).head.map(_ => fut.apply)
  }
  
  private def updateResult(source: IndexedSeq[SearchResult], index: Int, valuesIte: Iterator[EntryValues]): Boolean = {
    valuesIte.hasNext match {
      case true => awaitOption(valuesIte.next).filter(notCanceled) match {
        case Some(values) =>
          makeSearchingResult(source(index), values)
          listModel.itemUpdated(index)
          updateResult(source, index + 1, valuesIte)
        case None => false
      }
      case false => true
    }
  }
}
