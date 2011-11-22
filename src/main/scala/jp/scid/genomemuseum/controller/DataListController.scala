package jp.scid.genomemuseum.controller

import javax.swing.{JTable, JTextField, JLabel, JComponent, TransferHandler}

import jp.scid.gui.ValueHolder
import jp.scid.gui.table.DataTableModel
import jp.scid.gui.event.ValueChange

import jp.scid.genomemuseum.{view, model, gui}
import gui.{ExhibitTableModel, WebSearchManager, WebServiceResultsModel}

abstract class DataListController(
  dataTable: JTable,
  quickSearchField: JTextField,
  statusField: JLabel
) {
  /** テーブルモデル */
  private[controller] def tableModel: DataTableModel[_]
  /** 検索文字列モデル */
  private[controller] val searchTextModel: ValueHolder[String] = new ValueHolder("")
  /** 状態文字列モデル */
  private[controller] val statusTextModel: ValueHolder[String] = new ValueHolder("")
  /** 転送ハンドラ */
  private[controller] val tableTransferHandler = new TransferHandler("")
  
  // アクション
  /** 項目削除アクション */
  val removeSelectionAction = new swing.Action("Remove") {
    def apply() {}
    enabled = false
  }
  
  /**
   * ビューとモデルの結合を行う。
   */
  def bind() = {
    DataTableModel.connect(tableModel, dataTable)
    dataTable setTransferHandler tableTransferHandler
    dataTable.getActionMap.put("delete", removeSelectionAction.peer)
    
    val searchConn = ValueHolder.connect(searchTextModel, quickSearchField)
    val statConn = ValueHolder.connect(statusTextModel, statusField, "text")
    List(searchConn, statConn)
  }
}


class WebServiceResultController(
  dataTable: JTable,
  quickSearchField: JTextField,
  statusField: JLabel,
  progressView: JComponent
) extends DataListController(dataTable, quickSearchField, statusField) {
  import WebSearchManager._
  // モデル
  /** タスクが実行中であるかの状態を保持 */
  val isProgress = new ValueHolder(false)
  /** テーブルモデル */
  private[controller] val tableModel = new WebServiceResultsModel
  
  // モデルバインド
  /** Web 検索文字列の変更 */
  statusTextModel.reactions += {
    case ValueChange(_, _, newValue) =>
      println("searching query: " + newValue)
      tableModel.searchQuery = newValue.asInstanceOf[String]
  }
  /** 検索状態の更新 */
  tableModel.reactions += {
    case Started() =>
      isProgress := true
    case CountRetrivingTimeOut() =>
      statusTextModel := "取得に失敗しました。"
    case CountRetrieved(count) =>
      statusTextModel := "%d 件".format(count)
    case Canceled() =>
    
    case Succeed() =>
    
    case Done() =>
      isProgress := false
  }
  
  override def bind() = {
    val connList = super.bind()
    val progConn = ValueHolder.connectVisible(isProgress, progressView)
    
    List(progConn) ::: connList
  }
}
