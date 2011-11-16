package jp.scid.genomemuseum.controller

import java.awt.datatransfer.{Transferable, DataFlavor}
import javax.swing.{JTable, JTextField, JComponent, TransferHandler}

import jp.scid.gui.ValueHolder
import jp.scid.gui.event.{ValueChange, DataListSelectionChanged}
import jp.scid.gui.table.DataTableModel
import jp.scid.genomemuseum.{view, model, gui}
import gui.{ExhibitTableModel, WebSearchManager, WebServiceResultsModel}
import model.{TableDataService, MuseumExhibit, MuseumExhibitService}

class MuseumExhibitListController(
  dataTable: JTable,
  quickSearchField: JTextField
) {
  import MuseumExhibitListController._
  import TableSource._
  
  // 転送ハンドラ
  private val tableTransferHandler = new MuseumExhibitListTransferHandler(this)
  
  // ハンドラバインド
  dataTable setTransferHandler tableTransferHandler
  
  // モデル
  /** 現在の表示モード */
  private var currentTableSource: TableSource = LocalSource
  
  /** ローカルソース選択時のモデル */
  private[controller] val localSourceModel = new LocalSourceMuseumExhibitModel
  /** ウェブソース選択時のモデル */
  private[controller] val webSourceModel = new WebSourceMuseumExhibitModel
  /** コンテントビューワーの表示状態モデル */
  val contentViewerVisibilityModel = new ValueHolder(true)
  
  // バインディング解除関数
  private var unbindModels: () => Unit = () => {}
  
  localSourceTableModel.reactions += {
    // 削除アクション有効化更新のため
    case DataListSelectionChanged(_, false, _) => updateActionAvailability()
  }
  
  /** ローカルソースの選択項目を除去するアクション */
  val removeSelectedExhibitAction = swing.Action("Remove") {
    localSourceTableModel.removeSelections()
  }
  
  /** Exhibit の中身を表示 */
  private def setViewerContent(exhibit: Option[MuseumExhibit]) {
    val source = exhibit match {
      case Some(exhibit) => exhibit.filePath match {
        case "" => Iterator.empty
        case filePath => io.Source.fromFile(filePath).getLines
      }
      case None => Iterator.empty
    }
    
    // ソースが存在する時はビューワーを開く
    if (source.hasNext)
      showContentViewer()
    
//    contentViewer.source = source
  }
  
  /** コンテントビューワーを表示する */
  def showContentViewer() {
//    if (mainView.isContentViewerClosed)
//      mainView.openContentViewer(200)
}
  
  // プロパティ
  /** 表示モードを取得 */
  def tableSource = currentTableSource
  
  /** 表示モードを設定 */
  def tableSource_=(newSource: TableSource) {
    if (currentTableSource != newSource) {
      currentTableSource = newSource
      updateTableSource()
    }
  }
  
  /** ローカルデータテーブルのデータサービスを取得する */
  def localDataService = localSourceTableModel.dataService
  
  /** ローカルデータテーブルのデータサービスを設定する */
  def localDataService_=(newLocalDataService: MuseumExhibitService) {
    localSourceTableModel.dataService = newLocalDataService
  }
  
  private def updateTableSource() {
    unbindModels()
    
    val connector = currentTableSource match {
      case LocalSource =>
        DataTableModel.connect(localSourceTableModel, dataTable)
        val qsConn = ValueHolder.connect(localSourceSearchTextModel, quickSearchField)
        qsConn
      case WebSource =>
        DataTableModel.connect(webSourceTableModel, dataTable)
        val qsConn = ValueHolder.connect(webSourceSearchTextModel, quickSearchField)
        qsConn
    }
    
    val isLocalSource = currentTableSource == LocalSource
    contentViewerVisibilityModel := isLocalSource
    
    updateActionAvailability()
    
    unbindModels = () => {
      connector.release()
    }
  }
  
  private def updateActionAvailability() {
    // LocalSource 状態で行選択が行われている時のみ利用可能
    removeSelectedExhibitAction.enabled = currentTableSource == LocalSource &&
      !localSourceTableModel.selectionModel.isSelectionEmpty
  }
  
  /**
   * バイオファイルの読み込み処理を行う。
   */
  private[controller] def loadBioFiles(files: Seq[java.io.File]) = {
    false
  }
  
  /** ローカルソースのテーブルモデルショートカット */
  private[controller] def localSourceTableModel = localSourceModel.tableModel
  
  /** ウェブソースのテーブルモデルショートカット */
  private[controller] def webSourceTableModel = webSourceModel.tableModel
  
  /** ローカルソースの検索文字列モデルショートカット */
  private[controller] def localSourceSearchTextModel = localSourceModel.searchTextModel
  
  /** ウェブソースの検索文字列モデルショートカット */
  private[controller] def webSourceSearchTextModel = webSourceModel.searchTextModel
  
  // 表示を更新
  updateTableSource()
}

object MuseumExhibitListController {
  /**
   * ビューに表示するデータソースの種類
   */
  object TableSource extends Enumeration {
    type TableSource = Value
    val LocalSource = Value
    val WebSource = Value
  }
  
  def connect(model: MuseumExhibitModel, table: JTable, textField: JTextField) = {
    DataTableModel.connect(model.tableModel, table)
    ValueHolder.connect(model.searchTextModel, textField)
  }
  
  private def connect(model: Model, table: JTable, textField: JTextField,
      viewerPane: JComponent) = {
    DataTableModel.connect(model.tableModel, table)
    ValueHolder.connect(model.searchTextModel, textField)
    ValueHolder.connectVisible(model.contentViewerVisibilityModel, viewerPane)
  }
  
  private case class Model(
    tableModel: DataTableModel[_],
    searchTextModel: ValueHolder[String],
    contentViewerVisibilityModel: ValueHolder[Boolean] = new ValueHolder(false)
  )
}

trait MuseumExhibitModel {
  /** テーブルモデル */
  def tableModel: DataTableModel[_]
  
  /** 検索文字列モデル */
  val searchTextModel: ValueHolder[String] = new ValueHolder("")
}

class LocalSourceMuseumExhibitModel extends MuseumExhibitModel {
  /** ローカルデータのテーブルモデル */
  val tableModel = new ExhibitTableModel
  /** コンテントビューワーの内容モデル */
  val contentViewerContentModel =
    new ValueHolder[Option[MuseumExhibit]](None)
  
  /** テーブルフィルタリング */
  searchTextModel.reactions += {
    case ValueChange(_, _, text: String) =>
      tableModel filterWith text
  }
  
  // テーブル行選択
  tableModel.reactions += {
    case DataListSelectionChanged(_, false, selections) =>
      val s = selections.map(_.asInstanceOf[MuseumExhibit])
      contentViewerContentModel := s.headOption
  }
}

class WebSourceMuseumExhibitModel extends MuseumExhibitModel {
  import WebSearchManager._
  
  /** ローカルデータのテーブルモデル */
  val tableModel = new WebServiceResultsModel
  /** 検索状態を保持したモデル */
  val statusTextModel = new ValueHolder("")
  /** タスクが実行中であるかの状態を保持 */
  val isRunning = new ValueHolder(false)
  
  // イベント接続
  /** Web 検索文字列の変更 */
  statusTextModel.reactions += {
    case ValueChange(_, _, newValue) =>
      println("searching query: " + newValue)
      tableModel.searchQuery = newValue.asInstanceOf[String]
  }
  
  /** 検索状態の更新 */
  tableModel.reactions += {
    case Started() =>
      isRunning := true
    case CountRetrivingTimeOut() =>
      statusTextModel := "取得に失敗しました。"
    case CountRetrieved(count) =>
      statusTextModel := "%d 件".format(count)
    case Canceled() =>
    
    case Succeed() =>
    
    case Done() =>
      isRunning := false
  }
}
