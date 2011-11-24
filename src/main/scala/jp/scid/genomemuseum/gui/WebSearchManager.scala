package jp.scid.genomemuseum.gui

import actors.{Actor, Futures, Future}

import jp.scid.bio.ws.{WebServiceAgent}
import WebServiceAgent.{Query, Identifier, EntryValues}
import jp.scid.gui.DataListModel
import jp.scid.genomemuseum.model.{SearchResult}

/**
 * ウェブ検索結果をリストモデルに適用するクラス。
 */
class WebSearchManager(val listModel: DataListModel[SearchResult],
    var agent: WebServiceAgent) {
  import WebSearchManager._
  
  // パラメータ
  /** リストに表示する最大項目数 */
  var resultMaximumCount = 200
  
  /** 実行が取り消されたかどうか */
  private var canceled = false
  
  /** 実行が終了したかどうか */
  private var finished = false
    
  /**
   * 指定の文字列で検索処理を行う
   * 現在実行中の検索は中断される。
   */
  def search(queryText: String) = {
    if (canceled) throw new IllegalStateException("already terminated")
    Futures.future {
      val result = countTask(queryText)
      finished = true
      result
    }
  }
  
  /**
   * 現在実行中の検索を停止する。
   */
  def cancel() = canceled = true
  
  def isCanceled = canceled
  
  def isDone = finished
  
  private[gui] def toResult(entryValues: EntryValues) = {
    val element = SearchResult(entryValues.identifier.value)
    makeSearchingResult(element, entryValues)
    element
  }
  
  private[gui] def toSearchingResult(identifier: String) =
    SearchResult(identifier)
  
  private[gui] def makeSearchingResult(element: SearchResult, entryValues: EntryValues) {
    element.accession = entryValues.accession
    element.definition = entryValues.definition
    element.length = entryValues.length
    element.done = true
    element.sourceUrl = Option(agent.getSource(entryValues.identifier))
  }
  
  /** 属性値取得とテーブル適用処理 */
  private def fieldValuesTask(ids: IndexedSeq[Identifier]) = {
    val elements = ids map (id => toSearchingResult(id.value))
    setSource(elements)
    
    elements zip agent.getFieldValues(ids) foreach { _ => makeSearchingResult _ }
    setSource(elements)
    elements
  }
  
  /** 識別子取得とテーブル適用処理 */
  private def identifierTask(query: Query) = {
    val ids = agent.searchIdentifiers(query)
    canceled match {
      case false => fieldValuesTask(ids)
      case true => IndexedSeq.empty
    }
  }
  
  /** 該当数取得とテーブル適用処理 */
  private def countTask(queryString: String) = {
    setSource(Nil)
    
    val query = agent.getCount(queryString)
    
    val resultTask = 0 < query.count && query.count < resultMaximumCount match {
      case true =>
        val fut = Futures.future {
          canceled match {
            case false => identifierTask(query)
            case true => IndexedSeq.empty
          }
        }
        Some(fut)
      case false => None
    }
    
    SearchingQuery(query.count, resultTask)
  }
  
  private def setSource(newSource: Seq[SearchResult]) =
    if (!canceled) listModel.source = newSource
}

object WebSearchManager {
  case class SearchingQuery(
    count: Int,
    resultTask: Option[Future[IndexedSeq[SearchResult]]]
  )
}
