package jp.scid.genomemuseum.gui

import java.util.Comparator
import javax.swing.SwingWorker

import actors.{Actor, Future, Futures, TIMEOUT}

import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.model.SearchResult
import jp.scid.bio.ws.{WebServiceAgent}
import WebServiceAgent.{Identifier, EntryValues}

class WebServiceResultsModel(format: WebServiceResultTableFormat)
    extends DataTableModel[SearchResult](format) with TableColumnSortable[SearchResult] {
  import WebSearchManager.SearchingQuery
  import WebServiceResultsModel._
  
  def this() = this(new WebServiceResultTableFormat)
  
  private var searchAgent = WebServiceAgent()
  
  private var currentTask: Option[SearchingTask] = None
  
  // モデル
  private var currentQuery = ""
  
  /** データ取得元 */
  def agent = searchAgent
  
  def agent_=(newAgent: WebServiceAgent) =
    searchAgent = newAgent
  
  /** 検索クエリ */
  def searchWith(newQuery: String) {
    if (newQuery.trim != currentQuery.trim) {
      currentTask.foreach(_.cancel(true))
      currentQuery = newQuery
      
      val task = new SearchingTask(newQuery)
      task.execute()
      currentTask = Some(task)
    }
  }
  
  private def createSearchManager =
    new WebSearchManager(this, searchAgent)
  
  /**
   * 検索を実行し、イベントを発行するタスク
   */
  class SearchingTask(val query: String) extends SwingWorker[Unit, Event] {
    val manager = createSearchManager
    
    def doInBackground() = try {
      val countRetrievingFut = manager search query
      
      // 該当数取得待機（タイムアウト 30 秒）
      val timeoutTime = System.currentTimeMillis + 30 * 1000
      while(!countRetrievingFut.isSet && System.currentTimeMillis < timeoutTime) {
        Thread.sleep(50)
      }
      
      countRetrievingFut.isSet match {
        case true =>
          // 時間内に取得
          val searchingQuery = countRetrievingFut()
          publish(CountRetrieved(searchingQuery.count))
          
          searchingQuery.resultTask foreach { resultsFut =>
            val timeoutTime = System.currentTimeMillis + 30 * 1000
            
            // データ取得待機（タイムアウト 30 秒）
            while(!resultsFut.isSet && System.currentTimeMillis < timeoutTime) {
              Thread.sleep(50)
            }
            if (!resultsFut.isSet)
              publish(DataRetrivingTimeOut())
          }
        case false =>
          // タイムアウトイベント発行
          publish(CountRetrivingTimeOut())
      }
    }
    catch {
      case e: InterruptedException => manager.cancel()
    }
    
    override def process(chunks: java.util.List[Event]) {
      import collection.JavaConverters._
      chunks.asScala foreach WebServiceResultsModel.this.publish
    }
    
    override def done() {
      WebServiceResultsModel.this.publish(Done())
      currentTask = None
    }
  }
}

object WebServiceResultsModel {
  sealed abstract class Event extends swing.event.Event
  case class Started() extends Event
  case class CountRetrieved(count: Int) extends Event
  case class CountRetrivingTimeOut() extends Event
  case class DataRetrivingTimeOut() extends Event
  case class Done() extends Event
}
