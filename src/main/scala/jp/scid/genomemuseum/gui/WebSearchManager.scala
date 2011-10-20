package jp.scid.genomemuseum.gui

import actors.{Actor, Futures}

import jp.scid.bio.ws.{WebServiceAgent, WebSourceIterator}
import WebServiceAgent.{Identifier, EntryValues}
import jp.scid.gui.{DataListModel, BooleanValueHolder, IntValueHolder}
import jp.scid.genomemuseum.model.{SearchResult, SearchResultRetrievingActor}

/**
 * ウェブ検索結果をリストモデルに適用する管理クラス。
 */
class WebSearchManager(val listModel: DataListModel[SearchResult], var agent: WebServiceAgent)
    extends swing.Publisher {
  import WebSearchManager._
  import Actor._
  
  def this(agent: WebServiceAgent) = this(new DataListModel[SearchResult], agent)
  
  // パラメータ
  /** 項目数取得のタイムアウト */
  var timeoutTime = 10000L
  /** リストに表示する最大項目数 */
  var resultMaximumCount = 200
  
  /** 検索アクター */
  private var searchingActor: Option[SearchingActor] = None
  
  /**
   * 指定の文字列で検索処理を行う
   * 現在実行中の検索は中断される。
   */
  def search(query: String) {
    cancel()
    clearListModel()
    
    if (query.nonEmpty) {
      publish(Started())
      val r = new SearchingActor(query)
      r.start()
      searchingActor = Some(r)
    }
  }
  
  /**
   * 現在実行中の検索を中断する。
   */
  def cancel() = searchingActor match {
    case Some(actor) =>
      actor.stop
      canceled()
      done()
      true
    case None =>
      false
  }
  
  /** リストモデルの内容を消去 */
  private def clearListModel() {
    listModel.sourceWithWriteLock { _.clear }
  }
  
  /** 検索結果数を取得。処理をタイムアウト時間まで待つ。 */
  private def tryRetrievingCount(query: String, timeOut: Long): Option[Int] = {
    val countFut = agent.count(query)
    val result = Futures.awaitAll(timeOut, countFut).head.map(_.asInstanceOf[Int])
    result
  }
  
  /** 項目数の取得完了 */
  protected def countRetrieved(count: Int) {
    publish(CountRetrieved(count))
  }
  
  /** 取得完了後の処理 */
  protected def succeed() {
    publish(Succeed())
  }
  
  /** ユーザーによる中断命令後の処理 */
  protected def canceled() {
    publish(Canceled())
  }
  
  // 失敗処理
  /** 検索の適合数の取得がタイムアウトした後の処理 */
  protected def countRetrivingTimeOut() {
    publish(CountRetrivingTimeOut())
  }
  
  // 終了処理
  /** 成功、中断、失敗などの処理後に呼ばれる処理 */
  protected def done() {
    searchingActor = None
    publish(Done())
  }
  
  /**
   * 取得
   */
  private class RetrievingActor()
      extends SearchResultRetrievingActor(listModel) {
    override protected def done() {
      succeed()
      WebSearchManager.this.done()
    }
  }
  
  /**
   * 検索
   */
  private class SearchingActor(query: String) extends Actor {
    val dataRetrievingActor = new RetrievingActor
    var cancelSearching = false
    
    def act() {
      val count = tryRetrievingCount(query, timeoutTime)
      
      if (!cancelSearching) count match {
        case Some(count) =>
          countRetrieved(count)
          
          if (count <= resultMaximumCount) {
            val resultSet = new WebSourceIterator(agent, query, count)
            resultSet foreach dataRetrievingActor.add
            dataRetrievingActor.start()
          }
          else {
            done()
          }
        case None =>
          countRetrivingTimeOut()
          done()
      }
    }
    
    def stop() {
      cancelSearching = true
      dataRetrievingActor.stop()
    }
  }
}

object WebSearchManager {
  import swing.event.Event
  
  case class Started() extends Event
  case class CountRetrieved(count: Int) extends Event
  case class CountRetrivingTimeOut() extends Event
  case class Canceled() extends Event
  case class Succeed() extends Event
  case class Done() extends Event
}
