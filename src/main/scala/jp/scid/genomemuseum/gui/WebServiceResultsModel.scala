package jp.scid.genomemuseum.gui

import java.util.Comparator

import actors.{Actor, Future, Futures, TIMEOUT}

import jp.scid.gui.table.DataTableModel
import jp.scid.genomemuseum.model.SearchResult
import jp.scid.bio.ws.{WebServiceAgent, WebSourceIterator}
import WebServiceAgent.{Identifier, EntryValues}

class WebServiceResultsModel(format: WebServiceResultTableFormat)
    extends DataTableModel[SearchResult](format) {
  import SearchResult.Status._
  import Actor._
  import WebSourceActor._
  
  def this() = this(new WebServiceResultTableFormat)
  
  private type EntryFeature = Future[(Identifier, Future[EntryValues])]
  
  /** データ取得元 */
  var agent = WebServiceAgent()
  
  /** 非同期データソース */
  private var currentSourceActor: Option[WebSourceActor] = None
  
  /**
   * このモデルのソースを指定したクエリの検索結果にする
   */
  def searchWith(newQuery: String) {
    currentSourceActor.map(_.stopActor())
    currentSourceActor = None
    
    sourceWithWriteLock { _.clear }
    
    actor {
      renewActor(newQuery)
    }
  }
  
  /**
   * 検索クエリから {@code WebSourceIterator} を作成。
   * {@code WebServiceAgent#count} が取得できるまではすべてのメソッドの処理はブロックされる。
   * @param query 検索クエリ
   */
  private[gui] def getSourceBy(query: String) = {
    val sizeFuture = agent.count(query)
    new WebSourceIterator(agent, query, sizeFuture.apply)
  }
  
  /**
   * 指定したクエリでモデルのアクターを更新する。
   * クエリの結果数が多い場合は、None となる
   */
  private[gui] def renewActor(newQuery: String) {
    val webSource = getSourceBy(newQuery)
  
    // 結果数が少ない時は、テーブルデータとして追加する
    val newActor = if (webSource.size > 200) {
      tooManyResultsFound()
      None
    }
    else {
      Some(new WebSourceActor)
    }
    
    newActor.foreach { a =>
      a.start()
      a.loadWith(webSource)
    }
    currentSourceActor = newActor
  }
  
  /**
   * 要素を Identifier から追加する
   */
  def addElement(identifier: Identifier) {
    // 行オブジェクト作成
    val rowObj = SearchResult(identifier.value, status = Searching)
    // 追加
    sourceWithWriteLock { source =>
      source += rowObj
    }
  }
  
  /**
   * Identifier の値を持つ要素を検索する
   */
  private def findByIdentifier(identifier: Identifier, results: Seq[SearchResult]):
      Option[(SearchResult, Int)] = {
    val sMap = results.view.zipWithIndex.map( e => (e._1.identifier, e)).toMap
    sMap.get(identifier.value)
  }
  
  /**
   * EntryValues から、この Identifier の値を持つ要素の値の更新を行う
   */
  protected def updateElementWith(values: EntryValues) {
    sourceWithReadLock { source =>    
      findByIdentifier(values.identifier, source) match {
        case Some((element, index)) =>
          element.status = Successed
          element.accession = values.accession
          element.length = values.length
          element.definition = values.definition
          
          // TODO 行オブジェクトに更新通知
          source(index) = element
        case None =>
      }
    }
  }
  
  /** データソースの非同期処理の取得 */
  private[gui] def sourceActor: Option[Actor] = currentSourceActor
  
  /** 結果が多すぎる時の処理 */
  private def tooManyResultsFound() {
    
  }
  
  object WebSourceActor {
    abstract sealed class Message
    
    // ソース追加メッセージ
    private final case class AddNextElementFrom(source: WebSourceIterator)
      extends Message
    
    // 要素追加メッセージ
    private final case class TryToAddElementWith(
      element: Future[(Identifier, Future[EntryValues])]) extends Message
    
    // Identifier 確認メッセージ
    private final case class TryToUpdateElementWith(
      valueFuture: Future[EntryValues]) extends Message
    
    // 終了メッセージ
    private final case class StopActor() extends Message
  }
  
  private class WebSourceActor extends Actor {
    def act() {
      reactWithin(0) {
        case StopActor() =>
          exit
        case TIMEOUT => react {
          case msg @ AddNextElementFrom(source) =>
            if (source.hasNext) {
              val element = source.next
              this ! TryToAddElementWith(element)
              this ! msg
            }
            act()
          case msg @ TryToAddElementWith(fut) =>
            fut.isSet match {
              // identifier が取得できたらオブジェクト追加
              case true =>
                val (identifier, valuesFut) = fut()
                addElement(identifier)
                this ! TryToUpdateElementWith(valuesFut)
              // 未取得の時はメッセージを再送信
              case false =>
                this ! msg
            }
            act()
          case msg @ TryToUpdateElementWith(valFut) =>
            valFut.isSet match {
              // 値の取得ができたらオブジェクト追加
              case true =>
                val entryValues = valFut()
                updateElementWith(entryValues)
              // 未取得の時はメッセージを再送信
              case false =>
                this ! msg
            }
            act()
        }
      }
    }
    
    def stopActor() {
      this !! StopActor()
    }
    
    def loadWith(source: WebSourceIterator) {
      if (source.hasNext) {
        this ! AddNextElementFrom(source)
      }
    }
  }
}
