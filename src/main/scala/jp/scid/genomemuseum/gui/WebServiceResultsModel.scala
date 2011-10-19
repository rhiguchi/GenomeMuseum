package jp.scid.genomemuseum.gui

import java.util.Comparator

import actors.{Actor, Future, Futures, TIMEOUT}

import jp.scid.gui.table.DataTableModel
import jp.scid.genomemuseum.model.SearchResult
import jp.scid.bio.ws.{WebServiceAgent, WebSourceIterator}
import WebServiceAgent.{Identifier, EntryValues}

class WebServiceResultsModel(format: WebServiceResultTableFormat)
    extends DataTableModel[SearchResult](format) {
  import WebSearchManager._
  
  def this() = this(new WebServiceResultTableFormat)
  
  private type EntryFeature = Future[(Identifier, Future[EntryValues])]
  
  // モデル
  private var _searchQuery = ""
  
  private val searchManager = new WebSearchManager(this, WebServiceAgent())
  
  listenTo(searchManager)
  
  /** データ取得元 */
  def agent = searchManager.agent
  
  def agent_=(newAgent: WebServiceAgent) = searchManager.agent = newAgent
  
  /** 検索クエリ */
  def searchQuery = _searchQuery
  
  def searchQuery_=(newQuery: String) {
    if (newQuery.trim != _searchQuery.trim) {
      _searchQuery = newQuery
      searchManager search newQuery
    }
  }
}
