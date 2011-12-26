package jp.scid.genomemuseum.gui

import java.util.Comparator
import javax.swing.SwingWorker

import actors.{Futures, Future, Actor, TIMEOUT}

import jp.scid.gui.table.{DataTableModel, TableColumnSortable}
import jp.scid.genomemuseum.model.SearchResult
import jp.scid.bio.ws.{WebServiceAgent}
import WebServiceAgent.{Identifier, EntryValues}

class WebServiceResultsModel(format: WebServiceResultTableFormat)
    extends DataTableModel[SearchResult](format) with TableColumnSortable[SearchResult] {
  
  def this() = this(new WebServiceResultTableFormat)
  
  /** 現在の検索している文字列 */
  private var currentQuery = ""
  
  private var searchAgent = WebServiceAgent()
  
  /** 現在実行中の検索取得タスク */
  private var currentTask: Option[(WebSearchManager, Actor)] = None
  
  /** データ取得元 */
  def agent = searchAgent
  
  def agent_=(newAgent: WebServiceAgent) =
    searchAgent = newAgent
  
  /** 現在の検索している文字列を取得する */
  def searchQuery = currentQuery
  
  /** 検索クエリ */
  def searchWith(newQuery: String) = synchronized {
    currentQuery = newQuery
    
    currentTask.foreach { case (manager, actor) =>
      if (actor.getState != Actor.State.Terminated) {
        actor ! 'stop
        deafTo(manager)
      }
    }
    
    val manager = new WebSearchManager(this, searchAgent, newQuery)
    
    val actor = Actor.actor {
      import Actor._
      val task = Futures.future {
        listenTo(manager)
        manager.run
        deafTo(manager)
      }
      
      loopWhile(! task.isSet) {
        reactWithin(50) {
          case 'stop =>
            manager.cancel
            task.apply
          case TIMEOUT =>
        }
      }
    }
    
    currentTask = Some((manager, actor))
  }
}
