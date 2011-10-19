package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JLabel}

import actors.{Actor, Futures, Future, TIMEOUT}


import jp.scid.gui.{StringValueModel, BooleanValueHolder, IntValueHolder}
import jp.scid.gui.event.{StringValueChanged, BooleanValueChange, IntValueChange}

import jp.scid.genomemuseum.{view, model, gui}
import gui.{ExhibitTableModel, WebSearchManager, WebServiceResultsModel}
import model.{MuseumScheme, TableDataService, MuseumExhibit}

class DataListViewController(
  dataTable: JTable,
  quickSearchField: JTextField,
  searchStateField: JLabel,
  loadingIconLabel: JLabel
) {
  import DataListViewController._
  import ViewMode._
  import WebSearchManager._
  
  // 検索フィールドバインド解除関数
  private var unbindSearchField: () => Unit = () => {}
  
  // モデル
  /** 現在の表示モード */
  private var currentViewMode: ViewMode = LocalSource
  /** 現在のスキーマ */
  private var _dataSchema = MuseumScheme.empty
  
  /** テーブルモデル */
  val tableModel = new ExhibitTableModel
  /** テーブルモデルフィルタリング検索文字列 */
  private val tableFilteringText = new StringValueModel("")
  /** 検索状態を保持したモデル */
  private val localSearchStatusText = new StringValueModel("")
  /** 検索実行中であるかの状態を保持 */
  private val isFilteringRunning = new BooleanValueHolder(false)
  
  /** ウェブソース検索モデル */
  private val webServiceResultModel = new WebServiceResultsModel()
  /** ウェブソース検索文字列 */
  private val webSourceSearchText = new StringValueModel("")
  /** 検索状態を保持したモデル */
  private val webSearchStatusText = new StringValueModel("")
  /** タスクが実行中であるかの状態を保持 */
  private val isRunning = new BooleanValueHolder(false)
  
  // イベント
  // フィルタリング文字列の変更
  tableFilteringText.reactions += {
    case StringValueChanged(source, newValue, oldValue) =>
      tableModel refilterWith newValue
  }
  
  // Web 検索文字列の変更
  webSourceSearchText.reactions += {
    case StringValueChanged(source, newValue, oldValue) =>
      println("searching query: " + newValue)
      webServiceResultModel.searchQuery = newValue
  }
  
  webServiceResultModel.reactions += {
    case Started() =>
      isRunning := true
    case CountRetrivingTimeOut() =>
      webSearchStatusText := "取得に失敗しました。"
    case CountRetrieved(count) =>
      webSearchStatusText := "%d 件".format(count)
    case Canceled() =>
    
    case Succeed() =>
    
    case Done() =>
      isRunning := false
  }
  
  // プロパティ
  /** 表示モードを取得 */
  def viewMode = currentViewMode
  
  /** 表示モードを設定 */
  def viewMode_=(newMode: ViewMode) {
    if (currentViewMode != newMode) {
      currentViewMode = newMode
      updateBindings()
    }
  }
  
  /** 現在のデータモデルを取得設定 */
  def dataSchema = _dataSchema
  
  /** ソースリストやデータリストの表示に使用するデータモデルを設定 */
  def dataSchema_=(newSchema: MuseumScheme) {
    _dataSchema = newSchema
  }
  
  /** データテーブルのソースを取得する */
  def tableModelDataService = tableModel.dataService
  
  /** データテーブルのソースを設定する */
  def tableModelDataService_=(newRoom: TableDataService[MuseumExhibit]) {
    tableModel.dataService = newRoom
  }
  
  /** 表示モードによってモデル接続を変更 */
  private def updateBindings() {
    // バインディングリスナの削除
    unbindSearchField()
    
    def connectAll(searchFieldModel: StringValueModel, searchStatusModel: StringValueModel,
        loadingIconVisibilityModel: BooleanValueHolder) = {
      val qfConn = connect(searchFieldModel, quickSearchField)
      val statusConn = connect(searchStatusModel, searchStateField, "text")
      val iconConn = connect(loadingIconVisibilityModel, loadingIconLabel, "visible")
      () => {
        qfConn.release()
        statusConn.release()
        iconConn.release()
      }
    }
    
    currentViewMode match {
      case WebSource =>
        webServiceResultModel installTo dataTable
        unbindSearchField = connectAll(webSourceSearchText, webSearchStatusText, isRunning)
      case LocalSource =>
        tableModel installTo dataTable
        unbindSearchField = connectAll(tableFilteringText, localSearchStatusText, isFilteringRunning)
    }
  }
  
  // モデル接続
  updateBindings()
}

object DataListViewController {
  object ViewMode extends Enumeration {
    type ViewMode = Value
    val LocalSource = Value
    val WebSource = Value
  }
  
  import com.jgoodies.binding.adapter.TextComponentConnector
  import com.jgoodies.binding.value.ValueModel
  import com.jgoodies.binding.beans.PropertyConnector
  
  /**
   * テキストモデルの接続
   */
  private def connect(valueModel: ValueModel, field: JTextField) = {
    val conn = new TextComponentConnector(valueModel, field)
    conn.updateTextComponent()
    conn
  }
  
  private def connect(valueModel: ValueModel, component: AnyRef, property: String) = {
    val conn = PropertyConnector.connect(valueModel, "value", component, property)
    conn.updateProperty2()
    conn
  }
}
